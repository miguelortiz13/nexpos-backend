# 🛒 MarketCali - Backend System

**MarketCali** es una plataforma de gestión para supermercados diseñada con una arquitectura de **Monolito Modular**, seguridad robusta y un frontend moderno. El sistema consolida los dominios de negocio (inventarios, ventas, autenticación) en una única aplicación Spring Boot para facilitar el despliegue y mantenimiento, respaldado por Nginx para el frontend.

---

## 🏗️ Arquitectura Técnica

El sistema implementa una arquitectura modular donde cada dominio (`auth`, `product`, `sales`) está encapsulado en sus propios paquetes y módulos lógicos dentro del mismo proyecto monolítico.

### Diagrama de Comunicación
```mermaid
graph TD
    Client[Navegador / Frontend React] --> Nginx[Servidor Nginx (Puerto 80)]
    Nginx -. "/api/* y /auth/*" .-> Monolith[Spring Boot Monolith App (Puerto 8088)]
    
    subgraph MonolithApp [Monolith Application]
        AuthModule[Módulo Auth]
        ProductModule[Módulo Productos]
        SalesModule[Módulo Ventas]
    end
    
    Monolith --> DB[(MySQL 8.0 - marketcali_db)]
```

### Componentes del Sistema

| Servicio | Puerto (Docker/Local) | Descripción Técnica |
| :--- | :--- | :--- |
| **Frontend (React + Nginx)** | `80` (Docker) / `5173` (Local) | Aplicación web (React/Vite). Nginx actúa como proxy reverso para delegar llamadas `/api/` y `/auth/` al monolito. |
| **Monolith App** | `8088` | Backend centralizado en Spring Boot. Agrupa la seguridad (JWT) y la lógica de todos los módulos. |
| **MySQL Database** | `3306` (Interno) / `3307` (Local) | Instancia única de MySQL alojando el esquema unificado `marketcali_db`. |

---

## 📂 Estructura del Repositorio (Modular Monolith)

El repositorio sigue las mejores prácticas de arquitectura para **Monolitos Modulares** en Spring Boot:

```
marketcali-backend/
├── pom.xml                         # POM padre (gestión de dependencias y versiones de módulos)
├── docker-compose.yml              # Orquestador del stack: MySQL 8, Monolito Spring Boot, Frontend Nginx
├── docker/
│   └── mysql/init/01-schema.sql    # DDL inicial del esquema unificado de base de datos
├── terraform/                      # Infraestructura como Código (IaC) para Azure Container Apps & ACR
│
├── monolith-app/                   # Módulo host / orquestador Spring Boot
│   ├── Dockerfile                  # Compilación y empaquetado multi-stage (Maven -> Temurin JRE)
│   ├── pom.xml                     # Agregador de dependencias de los módulos de dominio
│   └── src/main/java/miguel/monolith/
│       ├── MarketcaliMonolithApplication.java # Entrypoint con @EntityScan y @EnableJpaRepositories
│       ├── config/                 # SecurityConfig (JWT, BCrypt, CORS) y AppConfig
│       └── exception/              # GlobalExceptionHandler (@ControllerAdvice uniforme)
│
├── auth-service/                   # Módulo de Dominio: Autenticación & Seguridad
│   └── src/main/java/miguel/auth/
│       ├── bootstrap/              # Inicializador de credenciales maestras (Admin)
│       ├── controller/             # Endpoints /auth/login, /auth/register y /api/users
│       ├── dto/                    # DTOs: LoginRequest, AuthResponse
│       ├── model/                  # Entidad Usuario y Enums de Roles (ADMIN, USER)
│       ├── repository/             # UsuarioRepository (Spring Data JPA)
│       ├── security/               # JwtProvider (firma y validación HMAC-SHA256)
│       └── service/                # Lógica de autenticación y cifrado
│
├── product-service/                # Módulo de Dominio: Catálogo & Inventario
│   └── src/main/java/miguel/product/
│       ├── bootstrap/              # Semillado de catálogo inicial y códigos de barras
│       ├── controller/             # Endpoints /api/productos (búsqueda rápida, lector de barras, CRUD)
│       ├── dto/                    # ProductoDTO
│       ├── model/                  # Entidad Producto
│       ├── repository/             # ProductoRepository
│       └── service/                # Servicio de catálogo y validación de inventario
│
└── sales-service/                  # Módulo de Dominio: Ventas, Facturación & Stock
    ├── src/main/java/miguel/sales/
    │   ├── controller/             # Endpoints /api/sales (checkout, transacciones)
    │   ├── dto/                    # SaleRequest, SaleItemRequest
    │   ├── model/                  # Entidades Sale, SaleItem, Invoice
    │   ├── repository/             # Repositorios JPA de transacciones y comprobantes
    │   └── service/                # Descuento atómico de stock y generación de facturas PDF
    └── src/test/java/miguel/sales/ # Pruebas unitarias de generación de comprobantes PDF
```

---

## 🚀 Tecnologías Clave

### Backend
*   **Java 17** (Eclipse Temurin)
*   **Spring Boot 3.2.5** (Web, Data JPA, Security, Validation)
*   **Hibernate** (Mapeo ORM)
*   **Spring Security** (Autenticación JWT)
*   **Lombok** (Generación de código)
*   **Maven** (Gestión de dependencias)

### Frontend
*   **React 18**
*   **Vite**
*   **React Router Dom**
*   **TailwindCSS / CSS Nativo**
*   **Nginx** (Despliegue de producción y Proxy pass)

### Infraestructura
*   **Docker & Docker Compose** (Orquestación local)
*   **MySQL 8.0**
*   **Terraform** (Aprovisionamiento IaC en Azure Container Apps - Ver `/terraform/README.md`)

---

## ⚙️ Configuración y Variables de Entorno

El proyecto consolida la configuración en `monolith-app/src/main/resources/application.yml`. Utiliza perfiles de Spring (`dev`, `docker`).

### Variables Principales en Docker
- `SPRING_DATASOURCE_URL`: url jdbc (`jdbc:mysql://mysql-db:3306/marketcali_db`)
- `SPRING_PROFILES_ACTIVE`: `docker`

### Puertos Expuestos a Localhost
- **Frontend App**: `http://localhost:80` (A través de Nginx)
- **Monolith App**: `http://localhost:8088` (Si se accede directamente al backend)
- **MySQL DB**: `localhost:3307` (Credenciales por defecto: `miguel` / `12345` / DB: `marketcali_db`)

---

## 🛠️ Despliegue y Ejecución

### Opción A: Docker Compose (Recomendado)

Levanta todo el ecosistema (Base de Datos, Backend y Frontend) con un solo comando usando Docker. Antes de esto, requieres haber generado el `.jar` de Spring Boot.

```bash
# 1. Compilar el monolito primero
mvn clean package -DskipTests

# 2. Construir e iniciar contenedores
docker-compose up -d --build
```

Esto iniciará:
1.  **MySQL** (Se inicializa la BBDD automáticamente con `init.sql`).
2.  **Monolith App** (Backend).
3.  **Frontend** (Nginx mapeando al puerto 80).

Accede a la aplicación gráfica desde: **[http://localhost](http://localhost)**. Para apagar el entorno usa `docker-compose down`.

### Opción B: Ejecución Manual para Desarrollo Local

Si necesitas editar el código en vivo (`hot reload`), es recomendable no usar Docker para las aplicaciones.

1.  **Levantar solo la Base de Datos:**
    ```bash
    docker-compose up -d mysql-db
    ```
2.  **Iniciar Backend (Monolith-App) mediante Maven:**
    ```bash
    cd monolith-app
    ./mvnw spring-boot:run
    ```
3.  **Iniciar Frontend (Vite):**
    Abre una nueva terminal.
    ```bash
    cd ../marketcali-react
    npm install
    npm run dev
    ```
    Visita `http://localhost:5173`. Las llamadas a `/api` y `/auth` en el entorno de desarrollo son interceptadas nativamente por Vite Proxy configurado en `vite.config.js`.

---

## 🔌 API Endpoints (Backend: 8088)

Al usar el frontend (puerto 80), todas las llamadas de tipo `/api/*` y `/auth/*` se enrutarán automáticamente al backend. Las siguientes son las familias de endpoints disponibles:

### 🔐 Autenticación (`/auth`)
*   `POST /auth/login`: Autenticación y obtención de JSON Web Token (Bearer).
*   `POST /auth/register`: Registro de usuarios. Requiere `username`, `password`, `email` y `role`.

### 📦 Módulo de Productos (`/api/productos`)
*   `GET /api/productos`: Listar inventario (Público).
*   `GET /api/productos/codigo/{codigoBarras}`: Ubicar producto mediante lector de barras.
*   `POST /api/productos`: Dar de alta un nuevo producto (Requiere Rol Admin).
*   `DELETE /api/productos/{id}`: Eliminar del inventario.

### 💰 Módulo de Ventas (`/api/sales`)
*   `POST /api/sales`: Registrar carrito y concretar factura de venta.

*(Ver la documentación interna del código para más detalle de schemas y DTOs).*

---

## 👥 Roles del Sistema

| Rol | Permisos Otorgados |
| :--- | :--- |
| **ADMIN** | Acceso global. Control Maestro de Inventario (CRUD), Creación/Eliminación de Usuarios, Consultas y Reportes globales. |
| **USER / EMPLEADO** | Emisión de tickets/ventas (Checkout) y lectura de stock de productos. |

> **Nota de inicialización:** Al levantar el sistema por primera vez, Spring Boot inyectará un usuario administrador predeterminado (`admin` / `admin`).

---

## 🤝 Contribución

1.  Hacer Fork del repositorio.
2.  Crear rama (`git checkout -b feature/ImplementarCajaPos`).
3.  Commit de los cambios realizados.
4.  Push a la rama (`git push origin feature/ImplementarCajaPos`).
5.  Crear un Pull Request.

---
**Desarrollado para MarketCali**
