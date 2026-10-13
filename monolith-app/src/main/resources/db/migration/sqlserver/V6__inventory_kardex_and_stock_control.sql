-- NexPOS · SQL Server · V6: kardex de inventario (equivalente a mysql/V6)

ALTER TABLE productos ADD min_stock INT NOT NULL DEFAULT 5;
ALTER TABLE productos ADD cost_price DECIMAL(38, 2) NOT NULL DEFAULT 0.00;

CREATE TABLE inventory_movements (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    movement_type NVARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    previous_stock INT NOT NULL,
    new_stock INT NOT NULL,
    unit_cost DECIMAL(38, 2) NULL,
    reason NVARCHAR(255) NOT NULL,
    reference_id NVARCHAR(100) NULL,
    registered_by NVARCHAR(100) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_inv_mov_product FOREIGN KEY (product_id) REFERENCES productos (id) ON DELETE CASCADE,
    INDEX idx_inv_mov_product_date (product_id, created_at),
    INDEX idx_inv_mov_type (movement_type)
);
