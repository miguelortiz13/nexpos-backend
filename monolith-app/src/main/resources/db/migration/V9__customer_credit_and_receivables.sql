-- ===================================================================
-- Migración V9: Cartera de Clientes, Crédito POS (Fiado) y Recaudos
-- ===================================================================

-- 1. Ampliación de la tabla customers para cupo y control de cartera
ALTER TABLE customers 
    ADD COLUMN credit_allowed BOOLEAN NOT NULL DEFAULT FALSE AFTER notes,
    ADD COLUMN credit_limit DECIMAL(12, 2) NOT NULL DEFAULT 0.00 AFTER credit_allowed,
    ADD COLUMN current_debt DECIMAL(12, 2) NOT NULL DEFAULT 0.00 AFTER credit_limit;

-- 2. Ampliación de la tabla sales para porción a crédito y estado de recaudo
ALTER TABLE sales
    ADD COLUMN credit_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 AFTER other_amount,
    ADD COLUMN payment_status VARCHAR(30) NOT NULL DEFAULT 'PAID' AFTER status;

-- 3. Ampliación de turnos de caja para registrar ventas a crédito acumuladas
ALTER TABLE cash_shifts
    ADD COLUMN total_sales_credit DECIMAL(12, 2) NOT NULL DEFAULT 0.00 AFTER total_sales_other;

-- 4. Parámetros de numeración para recibos de caja de cartera en company_config
ALTER TABLE company_config
    ADD COLUMN credit_receipt_prefix VARCHAR(10) NOT NULL DEFAULT 'RC' AFTER dian_nc_current_number,
    ADD COLUMN credit_receipt_current_number BIGINT NOT NULL DEFAULT 1 AFTER credit_receipt_prefix;

-- 5. Tabla de movimientos de cartera / estado de cuenta del cliente (Libro mayor de crédito)
CREATE TABLE IF NOT EXISTS customer_credit_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    sale_id BIGINT NULL,
    movement_type VARCHAR(30) NOT NULL, -- 'CARGO_VENTA', 'ABONO_PAGO', 'AJUSTE_NOTA_CREDITO'
    amount DECIMAL(12, 2) NOT NULL,
    previous_balance DECIMAL(12, 2) NOT NULL,
    new_balance DECIMAL(12, 2) NOT NULL,
    payment_method VARCHAR(30) NULL, -- 'EFECTIVO', 'TRANSFERENCIA', 'TARJETA'
    receipt_number VARCHAR(50) NULL,
    notes VARCHAR(255) NULL,
    cash_shift_id BIGINT NULL,
    registered_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_credit_movements_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_credit_movements_sale FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE SET NULL,
    INDEX idx_credit_movements_customer_id (customer_id),
    INDEX idx_credit_movements_receipt (receipt_number),
    INDEX idx_credit_movements_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
