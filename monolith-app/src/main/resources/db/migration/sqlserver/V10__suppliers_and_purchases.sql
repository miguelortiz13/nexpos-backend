-- NexPOS · SQL Server · V10: proveedores, recepción de mercancía y compras (equivalente a mysql/V10)

CREATE TABLE suppliers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    nit NVARCHAR(50) NOT NULL CONSTRAINT uq_suppliers_nit UNIQUE,
    company_name NVARCHAR(150) NOT NULL,
    contact_name NVARCHAR(100) NULL,
    phone NVARCHAR(30) NULL,
    email NVARCHAR(100) NULL,
    address NVARCHAR(200) NULL,
    city NVARCHAR(50) NOT NULL DEFAULT 'Cali',
    payment_terms_days INT NOT NULL DEFAULT 0, -- 0 = Contado, 15, 30, 60 días crédito
    active BIT NOT NULL DEFAULT 1,
    notes NVARCHAR(500) NULL,
    created_at DATETIME2 NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NULL DEFAULT SYSDATETIME(), -- lo actualiza la aplicación
    INDEX idx_suppliers_nit (nit),
    INDEX idx_suppliers_company_name (company_name),
    INDEX idx_suppliers_active (active)
);

CREATE TABLE purchases (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    purchase_number NVARCHAR(50) NOT NULL CONSTRAINT uq_purchases_number UNIQUE, -- Ej: COMP-0001
    supplier_id BIGINT NOT NULL,
    supplier_invoice_number NVARCHAR(100) NULL,
    status NVARCHAR(30) NOT NULL DEFAULT 'RECEIVED', -- 'RECEIVED', 'DRAFT', 'CANCELLED'
    payment_status NVARCHAR(30) NOT NULL DEFAULT 'PAID', -- 'PAID', 'PENDING', 'PARTIALLY_PAID'
    payment_method NVARCHAR(30) NOT NULL DEFAULT 'EFECTIVO', -- 'EFECTIVO', 'TRANSFERENCIA', 'CREDITO_PROVEEDOR', 'OTRO'
    payment_due_date DATE NULL,
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    notes NVARCHAR(500) NULL,
    registered_by NVARCHAR(100) NOT NULL,
    received_at DATETIME2 NULL DEFAULT SYSDATETIME(),
    created_at DATETIME2 NULL DEFAULT SYSDATETIME(),
    cancelled_at DATETIME2 NULL,
    cancelled_by NVARCHAR(100) NULL,
    cancellation_reason NVARCHAR(255) NULL,
    CONSTRAINT fk_purchases_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    INDEX idx_purchases_supplier_id (supplier_id),
    INDEX idx_purchases_number (purchase_number),
    INDEX idx_purchases_status (status),
    INDEX idx_purchases_payment_status (payment_status),
    INDEX idx_purchases_received_at (received_at)
);

CREATE TABLE purchase_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    purchase_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_cost DECIMAL(12, 2) NOT NULL, -- Costo unitario sin IVA
    tax_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.19,
    tax_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_purchase_items_purchase FOREIGN KEY (purchase_id) REFERENCES purchases (id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_items_product FOREIGN KEY (product_id) REFERENCES productos (id),
    INDEX idx_purchase_items_purchase (purchase_id),
    INDEX idx_purchase_items_product (product_id)
);

INSERT INTO suppliers (nit, company_name, contact_name, phone, email, address, city, payment_terms_days, active, notes)
VALUES
(N'860005224-6', N'Bavaria & Cía S.C.A.', N'Asesor Comercial Preventa', N'3104567890', N'pedidos@bavaria.co', N'Carrera 8 # 34-12', N'Cali', 30, 1, N'Distribuidor oficial de cervezas y maltas'),
(N'890903939-5', N'Postobón S.A.', N'Preventista Zona Sur', N'3157891234', N'ventas@postobon.com.co', N'Calle 15 # 45-80', N'Cali', 15, 1, N'Gaseosas, aguas, tés y jugos Hit'),
(N'900543210-1', N'Distribuidora Mayorista del Valle S.A.S.', N'Carlos Restrepo', N'3189998877', N'ventas@distrivalle.com', N'Calle 25 # 12-40', N'Cali', 0, 1, N'Abarrotes, granos, aceites y lácteos');
