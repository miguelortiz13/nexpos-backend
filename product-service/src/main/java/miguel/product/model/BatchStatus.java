package miguel.product.model;

public enum BatchStatus {
    ACTIVO,       // Lote vigente con stock disponible
    POR_VENCER,   // Próximo a vencer (alerta preventiva)
    VENCIDO,      // Fecha de expiración superada
    AGOTADO       // Sin unidades restantes
}
