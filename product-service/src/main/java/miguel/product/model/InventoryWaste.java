package miguel.product.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_wastes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryWaste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Producto product;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "waste_type", length = 30, nullable = false)
    private WasteType wasteType;

    @Column(name = "unit_cost", precision = 38, scale = 2, nullable = false)
    private BigDecimal unitCost;

    @Column(name = "total_cost", precision = 38, scale = 2, nullable = false)
    private BigDecimal totalCost;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "registered_by", nullable = false, length = 100)
    private String registeredBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (totalCost == null && unitCost != null && quantity != null) {
            totalCost = unitCost.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
