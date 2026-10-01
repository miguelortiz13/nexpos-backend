# Log Analytics Workspace for Container Apps
resource "azurerm_log_analytics_workspace" "law" {
  name                = "law-${var.project_name}-${var.environment}"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  sku                 = "PerGB2018"
  retention_in_days   = 30

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# Container App Environment
resource "azurerm_container_app_environment" "aca_env" {
  name                       = "cae-${var.project_name}-${var.environment}"
  location                   = azurerm_resource_group.rg.location
  resource_group_name        = azurerm_resource_group.rg.name
  log_analytics_workspace_id = azurerm_log_analytics_workspace.law.id
  infrastructure_subnet_id   = azurerm_subnet.aca_subnet.id

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# Monolith App (Backend)
resource "azurerm_container_app" "monolith_app" {
  name                         = "ca-monolith-${var.project_name}-${var.environment}"
  container_app_environment_id = azurerm_container_app_environment.aca_env.id
  resource_group_name          = azurerm_resource_group.rg.name
  revision_mode                = "Single"

  registry {
    server               = azurerm_container_registry.acr.login_server
    username             = azurerm_container_registry.acr.admin_username
    password_secret_name = "acr-password"
  }

  secret {
    name  = "acr-password"
    value = azurerm_container_registry.acr.admin_password
  }

  secret {
    name  = "mysql-connection-url"
    value = "jdbc:mysql://${azurerm_mysql_flexible_server.mysql.fqdn}:3306/${var.database_name}"
  }

  template {
    container {
      name   = "monolith-app"
      image  = "${azurerm_container_registry.acr.login_server}/monolith-app:latest"
      cpu    = 1.0
      memory = "2.0Gi"

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }
      env {
        name        = "SPRING_DATASOURCE_URL"
        secret_name = "mysql-connection-url"
      }
      env {
        name  = "SPRING_DATASOURCE_USERNAME"
        value = var.mysql_admin_username
      }
      env {
        name  = "SPRING_DATASOURCE_PASSWORD"
        value = var.mysql_admin_password
      }
    }

    min_replicas = 1
    max_replicas = 5
  }

  ingress {
    allow_insecure_connections = false
    target_port                = 8088
    external_enabled           = true

    traffic_weight {
      percentage      = 100
      latest_revision = true
    }
  }

  # Dependency to ensure DB is created first
  depends_on = [
    azurerm_mysql_flexible_database.nexpos_db
  ]
}

# Frontend App (React)
resource "azurerm_container_app" "frontend_app" {
  name                         = "ca-frontend-${var.project_name}-${var.environment}"
  container_app_environment_id = azurerm_container_app_environment.aca_env.id
  resource_group_name          = azurerm_resource_group.rg.name
  revision_mode                = "Single"

  registry {
    server               = azurerm_container_registry.acr.login_server
    username             = azurerm_container_registry.acr.admin_username
    password_secret_name = "acr-password"
  }

  secret {
    name  = "acr-password"
    value = azurerm_container_registry.acr.admin_password
  }

  template {
    container {
      name   = "frontend"
      image  = "${azurerm_container_registry.acr.login_server}/frontend:latest"
      cpu    = 0.5
      memory = "1.0Gi"

      env {
        # Expose the API URL to the frontend build
        name  = "VITE_API_BASE_URL"
        value = "https://${azurerm_container_app.monolith_app.ingress[0].fqdn}"
      }
    }

    min_replicas = 1
    max_replicas = 5
  }

  ingress {
    allow_insecure_connections = false
    target_port                = 80
    external_enabled           = true

    traffic_weight {
      percentage      = 100
      latest_revision = true
    }
  }
}
