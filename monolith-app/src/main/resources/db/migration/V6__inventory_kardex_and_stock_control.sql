-- ===================================================================
-- NexPOS Database Migration V6: Inventory Kardex & Stock Control
-- Author: Miguel Ángel Ortiz Escobar
-- Description: Adds min_stock, cost_price to productos and creates inventory_movements table
-- ===================================================================

ALTER TABLE productos ADD COLUMN min_stock INT NOT NULL DEFAULT 5;
ALTER TABLE productos ADD COLUMN cost_price DECIMAL(38, 2) NOT NULL DEFAULT 0.00;

CREATE TABLE IF NOT EXISTS inventory_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    previous_stock INT NOT NULL,
    new_stock INT NOT NULL,
    unit_cost DECIMAL(38, 2) NULL,
    reason VARCHAR(255) NOT NULL,
    reference_id VARCHAR(100) NULL,
    registered_by VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_mov_product FOREIGN KEY (product_id) REFERENCES productos (id) ON DELETE CASCADE,
    INDEX idx_inv_mov_product_date (product_id, created_at),
    INDEX idx_inv_mov_type (movement_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
