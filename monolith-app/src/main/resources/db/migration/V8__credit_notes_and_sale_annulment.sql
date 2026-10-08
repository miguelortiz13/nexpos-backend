-- ===================================================================
-- NexPOS Database Migration V8: Credit Notes (Notas Crédito DIAN) & Sale Annulment
-- Author: Miguel Ángel Ortiz Escobar
-- Description: Adds status and annulment audit columns to sales, credit note numbering to company_config, and creates credit_notes table
-- ===================================================================

ALTER TABLE sales ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED';
ALTER TABLE sales ADD COLUMN annulled_at DATETIME NULL;
ALTER TABLE sales ADD COLUMN annulled_by VARCHAR(100) NULL;
ALTER TABLE sales ADD COLUMN annulment_reason VARCHAR(255) NULL;

ALTER TABLE company_config ADD COLUMN dian_nc_prefix VARCHAR(10) NOT NULL DEFAULT 'NC';
ALTER TABLE company_config ADD COLUMN dian_nc_current_number BIGINT NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS credit_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    credit_note_number VARCHAR(100) NOT NULL UNIQUE,
    sale_id BIGINT NOT NULL,
    invoice_number VARCHAR(100) NOT NULL,
    original_cude VARCHAR(255) NULL,
    cude VARCHAR(255) NULL,
    qr_data TEXT NULL,
    factus_bill_id VARCHAR(100) NULL,
    factus_status VARCHAR(50) NOT NULL DEFAULT 'VALIDATED',
    reason VARCHAR(255) NOT NULL,
    concept_code VARCHAR(10) NOT NULL DEFAULT '2',
    concept_description VARCHAR(100) NOT NULL DEFAULT 'Anulación de factura electrónica',
    total_amount DECIMAL(12, 2) NOT NULL,
    refund_cash DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    refund_other DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    cash_shift_id BIGINT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dian_response_message TEXT NULL,
    CONSTRAINT fk_credit_note_sale FOREIGN KEY (sale_id) REFERENCES sales (id) ON DELETE CASCADE,
    INDEX idx_credit_note_sale (sale_id),
    INDEX idx_credit_note_num (credit_note_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
