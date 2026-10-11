package miguel.product.model;

public enum PurchasePaymentStatus {
    PAID,            // Factura de compra pagada de contado o saldo liquidado
    PENDING,         // Cuenta por pagar a crédito pendiente
    PARTIALLY_PAID   // Factura con abonos parciales
}
