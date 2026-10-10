-- NexPOS · SQL Server · V2: índices de rendimiento (equivalente a mysql/V2)

CREATE INDEX idx_productos_codigo_barras ON productos (codigo_barras);
CREATE INDEX idx_sales_sale_date ON sales (sale_date);
CREATE INDEX idx_invoices_number ON invoices (invoice_number);
