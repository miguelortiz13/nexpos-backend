package miguel.product.model;

public enum WasteType {
    VENCIMIENTO,       // Producto expirado / caducado
    DANO_ROTURA,       // Daño físico en góndola o almacén, rotura de empaque
    ROBO_HURTO,        // Merma desconocida, hurto o descuadre operativo
    DESCOMPOSICION,    // Frutas, verduras o alimentos perecederos descompuestos
    CONSUMO_INTERNO,   // Uso administrativo / degustación autorizada
    OTRO               // Otras bajas justificadas
}
