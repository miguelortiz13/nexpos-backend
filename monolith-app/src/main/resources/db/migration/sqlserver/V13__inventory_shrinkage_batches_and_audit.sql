-- NexPOS · SQL Server · V13: control de mermas, caducidades (FEFO) y conteo físico (equivalente a mysql/V13)

-- 1. Tabla de Mermas y Bajas de Inventario
CREATE TABLE inventory_wastes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    waste_type NVARCHAR(30) NOT NULL,
    unit_cost DECIMAL(38, 2) NOT NULL,
    total_cost DECIMAL(38, 2) NOT NULL,
    reason NVARCHAR(255) NOT NULL,
    registered_by NVARCHAR(100) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_wastes_product FOREIGN KEY (product_id) REFERENCES productos (id)
);

CREATE INDEX idx_wastes_product ON inventory_wastes (product_id);
CREATE INDEX idx_wastes_created_at ON inventory_wastes (created_at);
CREATE INDEX idx_wastes_type ON inventory_wastes (waste_type);

-- 2. Tabla de Lotes y Fechas de Vencimiento (FEFO)
CREATE TABLE product_batches (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    batch_number NVARCHAR(50) NOT NULL,
    expiration_date DATE NOT NULL,
    initial_quantity INT NOT NULL,
    current_quantity INT NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    supplier_id BIGINT NULL,
    received_at DATE NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_batches_product FOREIGN KEY (product_id) REFERENCES productos (id),
    CONSTRAINT fk_batches_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);

CREATE INDEX idx_batches_product ON product_batches (product_id);
CREATE INDEX idx_batches_expiration ON product_batches (expiration_date);
CREATE INDEX idx_batches_status ON product_batches (status);

-- 3. Tabla de Sesiones de Auditoría y Conteo Físico
CREATE TABLE physical_audit_sessions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_code NVARCHAR(50) NOT NULL UNIQUE,
    status NVARCHAR(20) NOT NULL DEFAULT 'EN_CONTEO',
    auditor_username NVARCHAR(100) NOT NULL,
    notes NVARCHAR(500) NULL,
    total_items_counted INT NOT NULL DEFAULT 0,
    total_difference_units INT NOT NULL DEFAULT 0,
    total_cost_impact DECIMAL(38, 2) NOT NULL DEFAULT 0.00,
    started_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    closed_at DATETIME2 NULL
);

CREATE INDEX idx_audit_sessions_status ON physical_audit_sessions (status);
CREATE INDEX idx_audit_sessions_code ON physical_audit_sessions (session_code);

-- 4. Tabla de Ítems Contados en la Auditoría
CREATE TABLE physical_audit_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    system_stock INT NOT NULL,
    physical_stock INT NOT NULL,
    difference INT NOT NULL,
    unit_cost DECIMAL(38, 2) NOT NULL,
    cost_difference DECIMAL(38, 2) NOT NULL,
    notes NVARCHAR(255) NULL,
    counted_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_audit_items_session FOREIGN KEY (session_id) REFERENCES physical_audit_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_items_product FOREIGN KEY (product_id) REFERENCES productos (id)
);

CREATE INDEX idx_audit_items_session ON physical_audit_items (session_id);
CREATE INDEX idx_audit_items_product ON physical_audit_items (product_id);
