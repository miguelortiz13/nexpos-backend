-- NexPOS · SQL Server · V8: notas crédito y anulación (equivalente a mysql/V8)

ALTER TABLE sales ADD status NVARCHAR(30) NOT NULL DEFAULT 'COMPLETED';
ALTER TABLE sales ADD annulled_at DATETIME2 NULL;
ALTER TABLE sales ADD annulled_by NVARCHAR(100) NULL;
ALTER TABLE sales ADD annulment_reason NVARCHAR(255) NULL;

ALTER TABLE company_config ADD dian_nc_prefix NVARCHAR(10) NOT NULL DEFAULT 'NC';
ALTER TABLE company_config ADD dian_nc_current_number BIGINT NOT NULL DEFAULT 1;

CREATE TABLE credit_notes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    credit_note_number NVARCHAR(100) NOT NULL CONSTRAINT uq_credit_notes_number UNIQUE,
    sale_id BIGINT NOT NULL,
    invoice_number NVARCHAR(100) NOT NULL,
    original_cude NVARCHAR(255) NULL,
    cude NVARCHAR(255) NULL,
    qr_data NVARCHAR(MAX) NULL,
    factus_bill_id NVARCHAR(100) NULL,
    factus_status NVARCHAR(50) NOT NULL DEFAULT 'VALIDATED',
    reason NVARCHAR(255) NOT NULL,
    concept_code NVARCHAR(10) NOT NULL DEFAULT '2',
    concept_description NVARCHAR(100) NOT NULL DEFAULT N'Anulación de factura electrónica',
    total_amount DECIMAL(12, 2) NOT NULL,
    refund_cash DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    refund_other DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    cash_shift_id BIGINT NULL,
    created_by NVARCHAR(100) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    dian_response_message NVARCHAR(MAX) NULL,
    CONSTRAINT fk_credit_note_sale FOREIGN KEY (sale_id) REFERENCES sales (id) ON DELETE CASCADE,
    INDEX idx_credit_note_sale (sale_id),
    INDEX idx_credit_note_num (credit_note_number)
);
