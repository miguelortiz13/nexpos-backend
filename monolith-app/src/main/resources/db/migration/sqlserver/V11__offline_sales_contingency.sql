-- NexPOS · SQL Server · V11: modo offline / contingencia en ventas POS (equivalente a mysql/V11)

ALTER TABLE sales ADD offline_reference NVARCHAR(100) NULL;
ALTER TABLE sales ADD offline_created_at DATETIME2 NULL;
ALTER TABLE sales ADD is_offline_sync BIT NOT NULL DEFAULT 0;

CREATE INDEX idx_sales_offline_reference ON sales (offline_reference);
