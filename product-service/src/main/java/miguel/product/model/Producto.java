package miguel.product.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_barras", unique = true, length = 50)
    private String codigoBarras;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 50)
    private String marca;

    @Column(nullable = false)
    private BigDecimal precio;

    private int cantidad;

    @Column(length = 50)
    private String categoria;

    @Column(length = 500)
    private String descripcion;

    @Column(length = 255)
    private String imagen;

    // Campos tributarios DIAN
    @Column(name = "iva_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal ivaRate = new BigDecimal("0.19"); // 0.19, 0.05, 0.00

    @Column(name = "unit_measure", length = 20, nullable = false)
    @Builder.Default
    private String unitMeasure = "94"; // 94 = Unidad estándar DIAN, KGM = Kilogramo

    // Control de Inventario y Kardex
    @Column(name = "min_stock", nullable = false)
    @Builder.Default
    private int minStock = 5;

    @Column(name = "cost_price", precision = 38, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal costPrice = BigDecimal.ZERO;

    public Producto(String nombre, String marca, BigDecimal precio, int cantidad, String categoria) {
        this.nombre = nombre;
        this.marca = marca;
        this.precio = precio;
        this.cantidad = cantidad;
        this.categoria = categoria;
        this.ivaRate = new BigDecimal("0.19");
        this.unitMeasure = "94";
        this.minStock = 5;
        this.costPrice = BigDecimal.ZERO;
    }
}