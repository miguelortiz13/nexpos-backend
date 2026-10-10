package miguel.sales.model;

public enum CreditMovementType {
    CARGO_VENTA,          // Cargo por venta a crédito (Aumenta la deuda)
    ABONO_PAGO,           // Abono / Pago realizado por el cliente (Disminuye la deuda)
    AJUSTE_NOTA_CREDITO   // Reversión por anulación de venta / Nota Crédito (Disminuye la deuda)
}
