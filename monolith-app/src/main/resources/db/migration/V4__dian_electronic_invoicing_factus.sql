-- ===================================================================
-- NexPOS Database Migration V4: DIAN Electronic Invoicing & Factus
-- Author: Miguel Ángel Ortiz Escobar
-- Description: Adds company_config, DIAN fields to invoices, productos, and sale_items
-- ===================================================================

CREATE TABLE IF NOT EXISTS company_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nit VARCHAR(50) NOT NULL,
    business_name VARCHAR(200) NOT NULL,
    trade_name VARCHAR(200) NULL,
    address VARCHAR(200) NOT NULL,
    city VARCHAR(100) NOT NULL DEFAULT 'Cali',
    department VARCHAR(100) NOT NULL DEFAULT 'Valle del Cauca',
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(150) NOT NULL,
    tax_regime VARCHAR(50) NOT NULL DEFAULT 'RESPONSABLE_IVA',
    dian_resolution_number VARCHAR(100) NOT NULL DEFAULT '18764000001',
    dian_prefix VARCHAR(10) NOT NULL DEFAULT 'POS',
    dian_range_from BIGINT NOT NULL DEFAULT 1,
    dian_range_to BIGINT NOT NULL DEFAULT 50000,
    dian_current_number BIGINT NOT NULL DEFAULT 1,
    dian_technical_key VARCHAR(255) NULL,
    dian_start_date DATE NULL,
    dian_end_date DATE NULL,
    factus_api_url VARCHAR(255) NOT NULL DEFAULT 'https://api-sandbox.factus.com.co',
    factus_client_id VARCHAR(150) NULL,
    factus_client_secret VARCHAR(255) NULL,
    factus_api_token TEXT NULL,
    facturacion_activa BOOLEAN NOT NULL DEFAULT TRUE,
    environment VARCHAR(20) NOT NULL DEFAULT 'SANDBOX',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Configuración inicial por defecto para NexPOS Supermercado Demo
INSERT INTO company_config (
    id, nit, business_name, trade_name, address, city, department, phone, email,
    tax_regime, dian_resolution_number, dian_prefix, dian_range_from, dian_range_to,
    dian_current_number, dian_technical_key, dian_start_date, dian_end_date,
    factus_api_url, facturacion_activa, environment
) VALUES (
    1, '900.785.412-8', 'NexPOS Retail Colombia S.A.S.', 'NexPOS Supermarket & Market',
    'Av. Roosevelt # 34-50', 'Cali', 'Valle del Cauca', '(602) 889-1234', 'facturacion@nexpos.co',
    'RESPONSABLE_IVA', '18764000001', 'POS', 1, 50000,
    1, 'fc8eac422eba16e22ffd8c6f94b3f40a6e38162c', '2026-01-01', '2027-12-31',
    'https://api-sandbox.factus.com.co', TRUE, 'SANDBOX'
) ON DUPLICATE KEY UPDATE id=1;

-- Campos tributarios en Productos (Tarifa IVA y código unidad de medida DIAN)
ALTER TABLE productos ADD COLUMN iva_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.19;
ALTER TABLE productos ADD COLUMN unit_measure VARCHAR(20) NOT NULL DEFAULT '94';

-- Campos tributarios en Ítems de Venta
ALTER TABLE sale_items ADD COLUMN iva_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.19;
ALTER TABLE sale_items ADD COLUMN iva_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE sale_items ADD COLUMN base_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;

-- Campos de Facturación Electrónica DIAN en Invoices
ALTER TABLE invoices ADD COLUMN cude VARCHAR(255) NULL;
ALTER TABLE invoices ADD COLUMN qr_data TEXT NULL;
ALTER TABLE invoices ADD COLUMN factus_bill_id VARCHAR(100) NULL;
ALTER TABLE invoices ADD COLUMN factus_status VARCHAR(50) NOT NULL DEFAULT 'LOCAL_OFFLINE';
ALTER TABLE invoices ADD COLUMN xml_url VARCHAR(255) NULL;
ALTER TABLE invoices ADD COLUMN pdf_url VARCHAR(255) NULL;
ALTER TABLE invoices ADD COLUMN dian_response_message TEXT NULL;
CREATE INDEX idx_invoices_cude ON invoices (cude);
