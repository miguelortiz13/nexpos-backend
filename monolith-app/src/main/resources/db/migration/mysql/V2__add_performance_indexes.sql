-- ===================================================================
-- MarketCali Database Migration V2: Performance Indexes for Retail POS
-- Author: Miguel Ángel Ortiz Escobar
-- Description: Indexes for sub-50ms barcode lookups and fast sales reporting
-- ===================================================================

-- Index for instant barcode scan lookups
CREATE INDEX idx_productos_codigo_barras ON productos (codigo_barras);

-- Index for sales reporting range queries
CREATE INDEX idx_sales_sale_date ON sales (sale_date);

-- Index for invoice lookups by receipt number
CREATE INDEX idx_invoices_number ON invoices (invoice_number);
