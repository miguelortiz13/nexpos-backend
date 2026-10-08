package miguel.product.model;

public enum InventoryMovementType {
    ENTRADA,   // Recepción de mercancía, compra a proveedor
    SALIDA,    // Merma, vencimiento, avería, pérdida, consumo interno
    AJUSTE,    // Conteo físico, cuadre de inventario
    VENTA,     // Salida automática generada al facturar en punto de venta
    DEVOLUCION // Reingreso a inventario por anulación o devolución de venta
}
