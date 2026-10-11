package miguel.product.model;

public enum PurchaseStatus {
    RECEIVED,  // Mercancía recibida e ingresada a stock con cálculo de PMP
    DRAFT,     // Orden de compra o borrador sin afectar existencias
    CANCELLED  // Compra anulada (reversión de inventario)
}
