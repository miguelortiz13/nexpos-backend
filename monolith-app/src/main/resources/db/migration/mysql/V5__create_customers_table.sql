-- ===================================================================
-- Migración V5: Directorio de Clientes y Facturación Electrónica Nominal
-- ===================================================================

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_type VARCHAR(10) NOT NULL DEFAULT 'CC', -- CC, NIT, CE, PASAPORTE, TI
    doc_number VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NULL,
    phone VARCHAR(50) NULL,
    address VARCHAR(255) NULL,
    city VARCHAR(100) DEFAULT 'Cali',
    department VARCHAR(100) DEFAULT 'Valle del Cauca',
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_customers_doc_number (doc_number),
    INDEX idx_customers_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Registrar Consumidor Final predeterminado estándar DIAN
INSERT INTO customers (id, doc_type, doc_number, name, email, phone, address, city, department)
VALUES (1, 'CC', '222222222222', 'Consumidor Final', 'facturacion@nexpos.com.co', '3000000000', 'Venta en Mostrador POS', 'Cali', 'Valle del Cauca')
ON DUPLICATE KEY UPDATE name = 'Consumidor Final';

-- Alterar sales para incluir customer_email
ALTER TABLE sales ADD COLUMN customer_email VARCHAR(150) NULL AFTER customer_doc;
