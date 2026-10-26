-- NexPOS · SQL Server · V12: índices de rendimiento para consultas de inventario (equivalente a mysql/V12)

CREATE INDEX idx_productos_cantidad_min_stock ON productos (cantidad, min_stock);
CREATE INDEX idx_productos_nombre ON productos (nombre);
CREATE INDEX idx_productos_categoria ON productos (categoria);
