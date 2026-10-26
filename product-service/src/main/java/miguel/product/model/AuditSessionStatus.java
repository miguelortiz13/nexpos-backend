package miguel.product.model;

public enum AuditSessionStatus {
    EN_CONTEO,    // Sesión abierta en proceso de escaneo y conteo físico
    CONCILIADO,   // Auditoría finalizada y ajustes aplicados en Kardex
    CANCELADO     // Sesión descartada sin alterar stock
}
