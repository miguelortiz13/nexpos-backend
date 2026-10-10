-- ===================================================================
-- NexPOS Database Migration V7: Split Payment (Pago Mixto) and Shift Reconciliation
-- Author: Miguel Ángel Ortiz Escobar
-- Description: Adds split payment columns to sales table and backfills existing records
-- ===================================================================

ALTER TABLE sales ADD COLUMN cash_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE sales ADD COLUMN card_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE sales ADD COLUMN transfer_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE sales ADD COLUMN other_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;

-- Backfill historical sales data for data integrity
UPDATE sales
SET cash_amount = total_amount
WHERE (payment_method = 'EFECTIVO' OR payment_method IS NULL)
  AND cash_amount = 0.00;

UPDATE sales
SET card_amount = total_amount
WHERE payment_method = 'TARJETA'
  AND card_amount = 0.00;

UPDATE sales
SET transfer_amount = total_amount
WHERE payment_method = 'TRANSFERENCIA'
  AND transfer_amount = 0.00;

UPDATE sales
SET other_amount = total_amount
WHERE payment_method NOT IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'MIXTO')
  AND other_amount = 0.00;
