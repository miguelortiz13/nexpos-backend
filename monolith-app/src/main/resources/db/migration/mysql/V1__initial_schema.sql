-- ===================================================================
-- MarketCali Database Migration V1: Initial Normalized Schema
-- Author: Miguel Ángel Ortiz Escobar
-- Description: Creates core tables for Users, Products, Sales, Items and Invoices
-- ===================================================================

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS productos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_barras VARCHAR(50) UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    marca VARCHAR(50),
    precio DECIMAL(38, 2) NOT NULL,
    cantidad INT NOT NULL,
    categoria VARCHAR(50),
    descripcion VARCHAR(500),
    imagen VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT,
    customer_name VARCHAR(150),
    customer_doc VARCHAR(50),
    total_amount DECIMAL(12, 2) NOT NULL,
    amount_paid DECIMAL(12, 2),
    change_amount DECIMAL(12, 2),
    payment_method VARCHAR(30),
    sale_date DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sale_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id BIGINT,
    product_id BIGINT,
    product_name VARCHAR(255),
    quantity INT,
    unit_price DECIMAL(38, 2),
    sub_total DECIMAL(38, 2),
    CONSTRAINT fk_sale_items_sales FOREIGN KEY (sale_id) REFERENCES sales (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id BIGINT UNIQUE,
    invoice_number VARCHAR(255),
    issued_at DATETIME(6),
    CONSTRAINT fk_invoices_sales FOREIGN KEY (sale_id) REFERENCES sales (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
