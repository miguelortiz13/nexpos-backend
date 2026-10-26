-- ===================================================================
-- Migración V12: Índices de rendimiento para consultas de inventario
-- ===================================================================

-- 1. Índice compuesto para acelerar alertas de bajo stock y quiebres de inventario
CREATE INDEX idx_productos_cantidad_min_stock ON productos (cantidad, min_stock);

-- 2. Índice para búsquedas por nombre comercial en POS y catálogo
CREATE INDEX idx_productos_nombre ON productos (nombre);

-- 3. Índice para filtrado ágil por categoría
CREATE INDEX idx_productos_categoria ON productos (categoria);
