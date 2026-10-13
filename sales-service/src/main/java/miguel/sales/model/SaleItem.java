package miguel.sales.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;

    // Desglose Tributario DIAN
    @Column(name = "iva_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal ivaRate = new BigDecimal("0.19"); // 0.19, 0.05, 0.00

    @Column(name = "iva_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal ivaAmount = BigDecimal.ZERO;

    @Column(name = "base_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal baseAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    @JsonIgnore
    // Referencia de vuelta: fuera de equals/hashCode/toString para evitar el
    // ciclo infinito con la entidad padre (StackOverflowError al crear ventas).
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Sale sale;
}
