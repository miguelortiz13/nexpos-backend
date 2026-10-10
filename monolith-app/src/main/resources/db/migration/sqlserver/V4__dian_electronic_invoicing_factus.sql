-- NexPOS · SQL Server · V4: facturación electrónica DIAN y Factus (equivalente a mysql/V4)
-- `ON UPDATE CURRENT_TIMESTAMP` no existe en SQL Server: updated_at lo mantiene la aplicación.

CREATE TABLE company_config (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    nit NVARCHAR(50) NOT NULL,
    business_name NVARCHAR(200) NOT NULL,
    trade_name NVARCHAR(200) NULL,
    address NVARCHAR(200) NOT NULL,
    city NVARCHAR(100) NOT NULL DEFAULT 'Cali',
    department NVARCHAR(100) NOT NULL DEFAULT 'Valle del Cauca',
    phone NVARCHAR(50) NOT NULL,
    email NVARCHAR(150) NOT NULL,
    tax_regime NVARCHAR(50) NOT NULL DEFAULT 'RESPONSABLE_IVA',
    dian_resolution_number NVARCHAR(100) NOT NULL DEFAULT '18764000001',
    dian_prefix NVARCHAR(10) NOT NULL DEFAULT 'POS',
    dian_range_from BIGINT NOT NULL DEFAULT 1,
    dian_range_to BIGINT NOT NULL DEFAULT 50000,
    dian_current_number BIGINT NOT NULL DEFAULT 1,
    dian_technical_key NVARCHAR(255) NULL,
    dian_start_date DATE NULL,
    dian_end_date DATE NULL,
    factus_api_url NVARCHAR(255) NOT NULL DEFAULT 'https://api-sandbox.factus.com.co',
    factus_client_id NVARCHAR(150) NULL,
    factus_client_secret NVARCHAR(255) NULL,
    factus_api_token NVARCHAR(MAX) NULL,
    facturacion_activa BIT NOT NULL DEFAULT 1,
    environment NVARCHAR(20) NOT NULL DEFAULT 'SANDBOX',
    updated_at DATETIME2 DEFAULT SYSDATETIME()
);

SET IDENTITY_INSERT company_config ON;

INSERT INTO company_config (
    id, nit, business_name, trade_name, address, city, department, phone, email,
    tax_regime, dian_resolution_number, dian_prefix, dian_range_from, dian_range_to,
    dian_current_number, dian_technical_key, dian_start_date, dian_end_date,
    factus_api_url, facturacion_activa, environment
) VALUES (
    1, N'900.785.412-8', N'NexPOS Retail Colombia S.A.S.', N'NexPOS Supermarket & Market',
    N'Av. Roosevelt # 34-50', N'Cali', N'Valle del Cauca', N'(602) 889-1234', N'facturacion@nexpos.co',
    N'RESPONSABLE_IVA', N'18764000001', N'POS', 1, 50000,
    1, N'fc8eac422eba16e22ffd8c6f94b3f40a6e38162c', '2026-01-01', '2027-12-31',
    N'https://api-sandbox.factus.com.co', 1, N'SANDBOX'
);

SET IDENTITY_INSERT company_config OFF;

ALTER TABLE productos ADD iva_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.19;
ALTER TABLE productos ADD unit_measure NVARCHAR(20) NOT NULL DEFAULT '94';

ALTER TABLE sale_items ADD iva_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.19;
ALTER TABLE sale_items ADD iva_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE sale_items ADD base_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE invoices ADD cude NVARCHAR(255) NULL;
ALTER TABLE invoices ADD qr_data NVARCHAR(MAX) NULL;
ALTER TABLE invoices ADD factus_bill_id NVARCHAR(100) NULL;
ALTER TABLE invoices ADD factus_status NVARCHAR(50) NOT NULL DEFAULT 'LOCAL_OFFLINE';
ALTER TABLE invoices ADD xml_url NVARCHAR(255) NULL;
ALTER TABLE invoices ADD pdf_url NVARCHAR(255) NULL;
ALTER TABLE invoices ADD dian_response_message NVARCHAR(MAX) NULL;
CREATE INDEX idx_invoices_cude ON invoices (cude);
