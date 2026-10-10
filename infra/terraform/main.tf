# NexPOS en la capa gratuita de Azure:
#   API (Spring Boot) en Container Apps, escala a cero
#   Frontend (React) en Static Web Apps Free
#   Azure SQL con la oferta gratuita, sin contraseñas (identidad administrada)
#   Secretos (JWT, admin) en Key Vault
# Costo esperado: USD 0 dentro de los cupos de la suscripción.

data "azurerm_client_config" "current" {}

module "naming" {
  source = "git::https://github.com/miguelortiz13/terraform-modules-iac.git//modules/azure/naming?ref=v0.2.0"

  project     = "nexpos"
  environment = "prod"
  location    = var.location
  unique_seed = data.azurerm_client_config.current.subscription_id
  owner       = var.owner
  repository  = "miguelortiz13/nexpos-backend"
}

module "rg" {
  source = "git::https://github.com/miguelortiz13/terraform-modules-iac.git//modules/azure/resource-group?ref=v0.2.0"

  name     = module.naming.names.resource_group
  location = var.location
  tags     = module.naming.tags
}

# Identidad de la API: lee Key Vault y entra a Azure SQL sin contraseña.
module "api_identity" {
  source = "git::https://github.com/miguelortiz13/terraform-modules-iac.git//modules/azure/user-assigned-identity?ref=v0.2.0"

  name                = "${module.naming.names.user_assigned_identity}-api"
  resource_group_name = module.rg.name
  location            = module.rg.location
  tags                = module.naming.tags
}

# ---------------------------------------------------------------------------
# Secretos
# ---------------------------------------------------------------------------
module "kv" {
  source = "git::https://github.com/miguelortiz13/terraform-modules-iac.git//modules/azure/key-vault?ref=v0.2.0"

  name                = module.naming.names.key_vault
  resource_group_name = module.rg.name
  location            = module.rg.location
  tags                = module.naming.tags
  tenant_id           = data.azurerm_client_config.current.tenant_id

  # Demo: permite recrear el vault con el mismo nombre tras un destroy.
  purge_protection_enabled   = false
  soft_delete_retention_days = 7

  role_assignments = {
    api      = { principal_id = module.api_identity.principal_id, role_definition_name = "Key Vault Secrets User" }
    deployer = { principal_id = data.azurerm_client_config.current.object_id, role_definition_name = "Key Vault Secrets Officer" }
  }
}

# Los permisos de RBAC tardan en propagarse antes de poder escribir secretos.
resource "time_sleep" "kv_rbac" {
  create_duration = "60s"
  depends_on      = [module.kv]
}

resource "random_password" "jwt" {
  length  = 64
  special = false
}

resource "random_password" "admin" {
  length      = 20
  special     = false
  min_numeric = 2
  min_upper   = 2
  min_lower   = 2
}

resource "azurerm_key_vault_secret" "jwt" {
  name         = "jwt-secret"
  key_vault_id = module.kv.id
  # La app espera Base64 (mínimo 256 bits).
  value        = base64encode(random_password.jwt.result)
  content_type = "text/plain"

  depends_on = [time_sleep.kv_rbac]
}

resource "azurerm_key_vault_secret" "admin" {
  name         = "admin-password"
  key_vault_id = module.kv.id
  value        = random_password.admin.result
  content_type = "text/plain"

  depends_on = [time_sleep.kv_rbac]
}

# ---------------------------------------------------------------------------
# Base de datos: Azure SQL gratis
# ---------------------------------------------------------------------------
# La identidad de la API es la administradora de Entra ID del servidor: Flyway
# necesita DDL completo y así no hace falta crear usuarios con T-SQL.
module "sql" {
  source = "git::https://github.com/miguelortiz13/terraform-modules-iac.git//modules/azure/mssql-serverless-free?ref=v0.2.0"

  server_name         = module.naming.names.mssql_server
  database_name       = module.naming.names.mssql_database
  resource_group_name = module.rg.name
  location            = module.rg.location
  tags                = module.naming.tags

  entra_admin = {
    login_username = "${module.naming.names.user_assigned_identity}-api"
    object_id      = module.api_identity.principal_id
  }
}

# ---------------------------------------------------------------------------
# Frontend y API
# ---------------------------------------------------------------------------
module "web" {
  source = "git::https://github.com/miguelortiz13/terraform-modules-iac.git//modules/azure/static-web-app?ref=v0.2.0"

  name                = module.naming.names.static_web_app
  resource_group_name = module.rg.name
  location            = var.location
  tags                = module.naming.tags
}

module "cae" {
  source = "git::https://github.com/miguelortiz13/terraform-modules-iac.git//modules/azure/container-app-environment?ref=v0.2.0"

  name                = module.naming.names.container_app_environment
  resource_group_name = module.rg.name
  location            = module.rg.location
  tags                = module.naming.tags
}

locals {
  jdbc_url = join(";", [
    "jdbc:sqlserver://${module.sql.server_fqdn}:1433",
    "database=${module.sql.database_name}",
    "encrypt=true",
    "trustServerCertificate=false",
    "hostNameInCertificate=*.database.windows.net",
    # La base gratis se pausa sin uso: reanudarla tarda hasta ~1 min.
    "loginTimeout=90",
    "authentication=ActiveDirectoryManagedIdentity",
    "msiClientId=${module.api_identity.client_id}",
  ])
}

module "api" {
  source = "git::https://github.com/miguelortiz13/terraform-modules-iac.git//modules/azure/container-app?ref=v0.2.0"

  name                         = "${module.naming.names.container_app}-api"
  resource_group_name          = module.rg.name
  container_app_environment_id = module.cae.id
  tags                         = module.naming.tags

  image        = var.api_image
  cpu          = 0.5
  memory       = "1Gi"
  min_replicas = 0
  max_replicas = 1

  ingress           = { target_port = 8088 }
  health_probe_path = "/actuator/health"
  identity_ids      = [module.api_identity.id]

  key_vault_secrets = {
    "jwt-secret"     = azurerm_key_vault_secret.jwt.versionless_id
    "admin-password" = azurerm_key_vault_secret.admin.versionless_id
  }

  # Variable de entorno => nombre del secreto de Key Vault (no son valores).
  secret_env = {
    JWT_SECRET     = "jwt-secret"
    ADMIN_PASSWORD = "admin-password" # gitleaks:allow
  }

  env = {
    SPRING_PROFILES_ACTIVE = "azure"
    SPRING_DATASOURCE_URL  = local.jdbc_url
    CORS_ALLOWED_ORIGINS   = module.web.url
    JAVA_TOOL_OPTIONS      = "-XX:MaxRAMPercentage=75"
  }

  depends_on = [module.sql]
}
