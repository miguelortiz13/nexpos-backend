# NexPOS Azure Infrastructure (Modular Monolith)

Este directorio contiene la Infraestructura como Código (IaC) en Terraform para desplegar el proyecto NexPOS en **Microsoft Azure** utilizando **Azure Container Apps (ACA)**.

Esta infraestructura está diseñada para soportar la arquitectura de **Monolito Modular** del proyecto, simplificando el despliegue al reemplazar Kubernetes y API Gateway por servicios administrados más ligeros e integrados.

## Arquitectura Creada

1. **Resource Group:** Grupo de recursos lógico en Azure (`rg-nexpos-prod`).
2. **Virtual Network (VNet) y Subnets:** Redes privadas para los servicios de backend, bases de datos y el entorno de ACA.
3. **Azure Container Registry (ACR):** Repositorio privado para hospedar tus imágenes de Docker (`monolith-app` y `frontend`).
4. **Azure Database for MySQL (Flexible Server):** Base de datos relacional administrada y segura, con la base de datos unificada `nexpos_db` creada automáticamente.
5. **Log Analytics Workspace:** Espacio de trabajo centralizado para monitoreo y recolección de logs de todos los servicios.
6. **Azure Container Apps Environment:** Entorno serverless donde se despliegan los contenedores, compartiendo la misma red virtual y espacio de trabajo de loggings.
7. **Azure Container Apps:** Micro-entornos escalables para alojar la aplicación Spring Boot (`monolith-app`) y la aplicación React (`frontend`).

## Requisitos Previos

- Tener instalada la herramienta [Terraform](https://www.terraform.io/downloads.html).
- Tener instalada la herramienta [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli).
- Tener instalado `Docker` (recomendable Docker Desktop) para la creación de imágenes y comunicación con el ACR.

## Pasos de Despliegue

### 1. Iniciar sesión en Azure

Autentícate en tu cuenta de Azure utilizando la consola:
```bash
az login
```

### 2. Desplegar la Infraestructura (Terraform)

Inicializa tu entorno de Terraform y aplica los cambios. Durante la ejecución, debes proveer la contraseña de administrador de MySQL (obligatorio).

```bash
cd terraform/
terraform init

# Validar y ver los cambios planificados
terraform plan -var="mysql_admin_password=TuPasswordSeguro123!"

# Aplicar los cambios
terraform apply -var="mysql_admin_password=TuPasswordSeguro123!" --auto-approve
```

> **NOTA IMPORTANTE:** Es posible que en el primer despliegue que realices con `terraform apply`, las Container Apps notifiquen una alerta de que no encontraron la imagen de Docker en el ACR (puesto que el registro está recién creado y vacío). Terraform finalizará la creación de recursos o se demorará. Debes continuar con el siguiente paso para subir las imágenes rápidamente.

### 3. Construir y Subir las Imágenes al ACR

Una vez que Terraform finalice, te entregará un output llamado `acr_login_server`. Utiliza este valor para etiquetar y subir las dos imágenes correspondientes al proyecto:

```bash
# Iniciar sesión en el servicio de Container Registry nuevo
az acr login --name <TU_ACR_NAME>

# Regresar a la raíz del proyecto para realizar el build
cd ..

# 1. Construir y subir el Monolito Backend
docker build -f monolith-app/Dockerfile -t <acr_login_server>/monolith-app:latest .
docker push <acr_login_server>/monolith-app:latest

# 2. Construir y subir el Frontend
cd ../nexpos-frontend
docker build -t <acr_login_server>/frontend:latest .
docker push <acr_login_server>/frontend:latest
```

*(Recuerda reemplazar `<TU_ACR_NAME>` con el nombre de tu registro y `<acr_login_server>` con el link completo que entregó Terraform en la consola).*

### 4. Actualizar o Reiniciar los Servicios (Solo si es necesario)

Si las Container Apps estaban intentando arrancar contenedores vacíos, al detectar la nueva imagen Docker deberían actualizarse por sí solas en unos pocos minutos. Si quieres forzar una revisión o reinicio rápido, desde la línea de comandos puedes hacer lo siguiente:

```bash
az containerapp update \
  --name ca-monolith-nexpos-prod \
  --resource-group rg-nexpos-prod \
  --image <acr_login_server>/monolith-app:latest

az containerapp update \
  --name ca-frontend-nexpos-prod \
  --resource-group rg-nexpos-prod \
  --image <acr_login_server>/frontend:latest
```
*(Asegúrate de cambiar los sufijos `-prod` por el ambiente que utilizaste y el nombre de los Resource Groups correctos).*

## Monitoreo y Salidas

En cualquier momento, si estás en el directorio de `terraform/` puedes correr `terraform output` (o visualizar la salida de consola que se originó en el `apply`). Tendrás variables clave para continuar trabajando o probar tus despliegues:

- `frontend_url`: La URL pública segura (HTTPS) para visitar tu aplicación React.
- `monolith_backend_url`: La URL base de la Application Programming Interface (API) pública de tu backend unificado.
- `mysql_server_fqdn`: La dirección expuesta (con Firewall privado si aplica) de tu base de datos MySQL en Azure.
- `acr_login_server`: Servidor de registro privado, donde vivirán tus imágenes para el proyecto.
