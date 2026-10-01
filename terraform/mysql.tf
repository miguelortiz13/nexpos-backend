# MySQL Flexible Server
resource "azurerm_mysql_flexible_server" "mysql" {
  name                   = "mysql-${var.project_name}-${var.environment}"
  resource_group_name    = azurerm_resource_group.rg.name
  location               = azurerm_resource_group.rg.location
  administrator_login    = var.mysql_admin_username
  administrator_password = var.mysql_admin_password
  sku_name               = "B_Standard_B1ms" # Burstable tier is cost-effective for dev/small workloads
  version                = "8.0.21"

  delegated_subnet_id = azurerm_subnet.mysql_subnet.id
  private_dns_zone_id = azurerm_private_dns_zone.mysql_dns.id

  # Dependencies
  depends_on = [azurerm_private_dns_zone_virtual_network_link.mysql_dns_link]
}

# The primary database used by the monolith
resource "azurerm_mysql_flexible_database" "nexpos_db" {
  name                = var.database_name
  resource_group_name = azurerm_resource_group.rg.name
  server_name         = azurerm_mysql_flexible_server.mysql.name
  charset             = "utf8"
  collation           = "utf8_unicode_ci"
}

# Private DNS Zone for MySQL
resource "azurerm_private_dns_zone" "mysql_dns" {
  name                = "${var.project_name}.mysql.database.azure.com"
  resource_group_name = azurerm_resource_group.rg.name
}

# Link private DNS zone to VNet so AKS can resolve the MySQL server
resource "azurerm_private_dns_zone_virtual_network_link" "mysql_dns_link" {
  name                  = "mysql-dns-vnet-link"
  private_dns_zone_name = azurerm_private_dns_zone.mysql_dns.name
  virtual_network_id    = azurerm_virtual_network.vnet.id
  resource_group_name   = azurerm_resource_group.rg.name
}
