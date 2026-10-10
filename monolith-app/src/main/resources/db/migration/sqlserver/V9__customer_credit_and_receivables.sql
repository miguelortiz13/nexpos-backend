-- NexPOS · SQL Server · V9: cartera de clientes, crédito POS y recaudos (equivalente a mysql/V9)

ALTER TABLE customers ADD credit_allowed BIT NOT NULL DEFAULT 0;
ALTER TABLE customers ADD credit_limit DECIMAL(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE customers ADD current_debt DECIMAL(12, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE sales ADD credit_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE sales ADD payment_status NVARCHAR(30) NOT NULL DEFAULT 'PAID';

ALTER TABLE cash_shifts ADD total_sales_credit DECIMAL(12, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE company_config ADD credit_receipt_prefix NVARCHAR(10) NOT NULL DEFAULT 'RC';
ALTER TABLE company_config ADD credit_receipt_current_number BIGINT NOT NULL DEFAULT 1;

CREATE TABLE customer_credit_movements (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    sale_id BIGINT NULL,
    movement_type NVARCHAR(30) NOT NULL, -- 'CARGO_VENTA', 'ABONO_PAGO', 'AJUSTE_NOTA_CREDITO'
    amount DECIMAL(12, 2) NOT NULL,
    previous_balance DECIMAL(12, 2) NOT NULL,
    new_balance DECIMAL(12, 2) NOT NULL,
    payment_method NVARCHAR(30) NULL, -- 'EFECTIVO', 'TRANSFERENCIA', 'TARJETA'
    receipt_number NVARCHAR(50) NULL,
    notes NVARCHAR(255) NULL,
    cash_shift_id BIGINT NULL,
    registered_by NVARCHAR(100) NOT NULL,
    created_at DATETIME2 NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_credit_movements_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_credit_movements_sale FOREIGN KEY (sale_id) REFERENCES sales (id) ON DELETE SET NULL,
    INDEX idx_credit_movements_customer_id (customer_id),
    INDEX idx_credit_movements_receipt (receipt_number),
    INDEX idx_credit_movements_created_at (created_at)
);
