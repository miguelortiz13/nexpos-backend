-- ===================================================================
-- Migración V10: Proveedores, Recepción de Mercancía y Compras (PMP)
-- ===================================================================

-- 1. Tabla de Proveedores (Catálogo Comercial y Cuentas por Pagar)
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nit VARCHAR(50) NOT NULL UNIQUE,
    company_name VARCHAR(150) NOT NULL,
    contact_name VARCHAR(100) NULL,
    phone VARCHAR(30) NULL,
    email VARCHAR(100) NULL,
    address VARCHAR(200) NULL,
    city VARCHAR(50) NOT NULL DEFAULT 'Cali',
    payment_terms_days INT NOT NULL DEFAULT 0, -- 0 = Contado, 15, 30, 60 días crédito
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_suppliers_nit (nit),
    INDEX idx_suppliers_company_name (company_name),
    INDEX idx_suppliers_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tabla de Facturas de Compra / Órdenes de Recepción
CREATE TABLE IF NOT EXISTS purchases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_number VARCHAR(50) NOT NULL UNIQUE, -- Ej: COMP-0001
    supplier_id BIGINT NOT NULL,
    supplier_invoice_number VARCHAR(100) NULL, -- Número de factura física del distribuidor
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED', -- 'RECEIVED', 'DRAFT', 'CANCELLED'
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PAID', -- 'PAID', 'PENDING', 'PARTIALLY_PAID'
    payment_method VARCHAR(30) NOT NULL DEFAULT 'EFECTIVO', -- 'EFECTIVO', 'TRANSFERENCIA', 'CREDITO_PROVEEDOR', 'OTRO'
    payment_due_date DATE NULL,
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    notes VARCHAR(500) NULL,
    registered_by VARCHAR(100) NOT NULL,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP NULL,
    cancelled_by VARCHAR(100) NULL,
    cancellation_reason VARCHAR(255) NULL,
    CONSTRAINT fk_purchases_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE RESTRICT,
    INDEX idx_purchases_supplier_id (supplier_id),
    INDEX idx_purchases_number (purchase_number),
    INDEX idx_purchases_status (status),
    INDEX idx_purchases_payment_status (payment_status),
    INDEX idx_purchases_received_at (received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Detalle de Ítems Comprados
CREATE TABLE IF NOT EXISTS purchase_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_cost DECIMAL(12, 2) NOT NULL, -- Costo unitario sin IVA
    tax_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.19,
    tax_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_purchase_items_purchase FOREIGN KEY (purchase_id) REFERENCES purchases(id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_items_product FOREIGN KEY (product_id) REFERENCES productos(id) ON DELETE RESTRICT,
    INDEX idx_purchase_items_purchase (purchase_id),
    INDEX idx_purchase_items_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Inserción de Proveedores Semilla para Retail Colombiano
INSERT INTO suppliers (nit, company_name, contact_name, phone, email, address, city, payment_terms_days, active, notes)
VALUES 
('860005224-6', 'Bavaria & Cía S.C.A.', 'Asesor Comercial Preventa', '3104567890', 'pedidos@bavaria.co', 'Carrera 8 # 34-12', 'Cali', 30, TRUE, 'Distribuidor oficial de cervezas y maltas'),
('890903939-5', 'Postobón S.A.', 'Preventista Zona Sur', '3157891234', 'ventas@postobon.com.co', 'Calle 15 # 45-80', 'Cali', 15, TRUE, 'Gaseosas, aguas, tés y jugos Hit'),
('900543210-1', 'Distribuidora Mayorista del Valle S.A.S.', 'Carlos Restrepo', '3189998877', 'ventas@distrivalle.com', 'Calle 25 # 12-40', 'Cali', 0, TRUE, 'Abarrotes, granos, aceites y lácteos')
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);
