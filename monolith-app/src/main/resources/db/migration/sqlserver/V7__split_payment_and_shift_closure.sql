-- NexPOS · SQL Server · V7: pago mixto (equivalente a mysql/V7)

ALTER TABLE sales ADD cash_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE sales ADD card_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE sales ADD transfer_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE sales ADD other_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;
GO

UPDATE sales SET cash_amount = total_amount
WHERE (payment_method = 'EFECTIVO' OR payment_method IS NULL) AND cash_amount = 0.00;

UPDATE sales SET card_amount = total_amount
WHERE payment_method = 'TARJETA' AND card_amount = 0.00;

UPDATE sales SET transfer_amount = total_amount
WHERE payment_method = 'TRANSFERENCIA' AND transfer_amount = 0.00;

UPDATE sales SET other_amount = total_amount
WHERE payment_method NOT IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'MIXTO') AND other_amount = 0.00;
