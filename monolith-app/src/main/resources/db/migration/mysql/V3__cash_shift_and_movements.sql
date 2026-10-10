-- ===================================================================
-- NexPOS Database Migration V3: Cash Register Shifts & Cash Movements
-- Author: Miguel Ángel Ortiz Escobar
-- Description: Creates cash_shifts and cash_movements tables, connects sales with shift
-- ===================================================================

CREATE TABLE IF NOT EXISTS cash_shifts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cashier_id BIGINT NULL,
    cashier_username VARCHAR(100) NOT NULL,
    opened_at DATETIME NOT NULL,
    closed_at DATETIME NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
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
    notes VARCHAR(255) NULL,
    close_notes VARCHAR(255) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_shifts_cashier_status (cashier_username, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cash_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shift_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    registered_by VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cash_movements_shift FOREIGN KEY (shift_id) REFERENCES cash_shifts (id) ON DELETE CASCADE,
    INDEX idx_movements_shift (shift_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Relación de ventas con turno de caja
ALTER TABLE sales ADD COLUMN cash_shift_id BIGINT NULL;
ALTER TABLE sales ADD COLUMN cashier_username VARCHAR(100) NULL;
CREATE INDEX idx_sales_shift_id ON sales (cash_shift_id);
