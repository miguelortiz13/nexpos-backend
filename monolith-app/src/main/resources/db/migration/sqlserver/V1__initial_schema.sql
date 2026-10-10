-- ===================================================================
-- NexPOS · SQL Server / Azure SQL · V1: esquema inicial
-- Equivalente a mysql/V1__initial_schema.sql
-- ===================================================================

CREATE TABLE usuarios (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(255) NOT NULL CONSTRAINT uq_usuarios_username UNIQUE,
    password NVARCHAR(255) NOT NULL,
    email NVARCHAR(255) NOT NULL,
    role NVARCHAR(255) NOT NULL
);

CREATE TABLE productos (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    codigo_barras NVARCHAR(50) NULL,
    nombre NVARCHAR(100) NOT NULL,
    marca NVARCHAR(50),
    precio DECIMAL(38, 2) NOT NULL,
    cantidad INT NOT NULL,
    categoria NVARCHAR(50),
    descripcion NVARCHAR(500),
    imagen NVARCHAR(255)
);

-- En SQL Server un UNIQUE admite un solo NULL; MySQL admite varios.
-- El índice filtrado reproduce el comportamiento de MySQL.
CREATE UNIQUE INDEX uq_productos_codigo_barras ON productos (codigo_barras) WHERE codigo_barras IS NOT NULL;

CREATE TABLE sales (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id BIGINT,
    customer_name NVARCHAR(150),
    customer_doc NVARCHAR(50),
    total_amount DECIMAL(12, 2) NOT NULL,
    amount_paid DECIMAL(12, 2),
    change_amount DECIMAL(12, 2),
    payment_method NVARCHAR(30),
    sale_date DATETIME2(6)
);

CREATE TABLE sale_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sale_id BIGINT,
    product_id BIGINT,
    product_name NVARCHAR(255),
    quantity INT,
    unit_price DECIMAL(38, 2),
    sub_total DECIMAL(38, 2),
    CONSTRAINT fk_sale_items_sales FOREIGN KEY (sale_id) REFERENCES sales (id) ON DELETE CASCADE
);

CREATE TABLE invoices (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sale_id BIGINT NULL,
    invoice_number NVARCHAR(255),
    issued_at DATETIME2(6),
    CONSTRAINT fk_invoices_sales FOREIGN KEY (sale_id) REFERENCES sales (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_invoices_sale_id ON invoices (sale_id) WHERE sale_id IS NOT NULL;
