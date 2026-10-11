-- ===================================================================
-- Migración V11: Modo Offline / Contingencia en Ventas POS
-- ===================================================================

-- 1. Ampliación de la tabla sales para trazabilidad de ventas offline
ALTER TABLE sales
    ADD COLUMN offline_reference VARCHAR(100) NULL AFTER cash_shift_id,
    ADD COLUMN offline_created_at TIMESTAMP NULL AFTER offline_reference,
    ADD COLUMN is_offline_sync BOOLEAN NOT NULL DEFAULT FALSE AFTER offline_created_at;

-- 2. Índice para consultas idempotentes y búsqueda rápida por referencia offline
CREATE INDEX idx_sales_offline_reference ON sales (offline_reference);
