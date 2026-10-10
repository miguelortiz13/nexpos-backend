-- NexPOS · SQL Server · V3: turnos de caja y movimientos (equivalente a mysql/V3)

CREATE TABLE cash_shifts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    cashier_id BIGINT NULL,
    cashier_username NVARCHAR(100) NOT NULL,
    opened_at DATETIME2 NOT NULL,
    closed_at DATETIME2 NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'OPEN',
    initial_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    expected_cash_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    actual_cash_amount DECIMAL(12, 2) NULL,
    difference_amount DECIMAL(12, 2) NULL,
    total_sales_cash DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_sales_card DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_sales_transfer DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_sales_other DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_sales_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_sales_count INT NOT NULL DEFAULT 0,
    total_entries_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_exits_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    notes NVARCHAR(255) NULL,
    close_notes NVARCHAR(255) NULL,
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    INDEX idx_shifts_cashier_status (cashier_username, status)
);

CREATE TABLE cash_movements (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    shift_id BIGINT NOT NULL,
    type NVARCHAR(20) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    reason NVARCHAR(255) NOT NULL,
    registered_by NVARCHAR(100) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_cash_movements_shift FOREIGN KEY (shift_id) REFERENCES cash_shifts (id) ON DELETE CASCADE,
    INDEX idx_movements_shift (shift_id)
);

ALTER TABLE sales ADD cash_shift_id BIGINT NULL;
ALTER TABLE sales ADD cashier_username NVARCHAR(100) NULL;
CREATE INDEX idx_sales_shift_id ON sales (cash_shift_id);
