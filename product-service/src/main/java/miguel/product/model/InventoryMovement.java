package miguel.product.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Producto product;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", length = 20, nullable = false)
    private InventoryMovementType movementType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "previous_stock", nullable = false)
    private Integer previousStock;

    @Column(name = "new_stock", nullable = false)
    private Integer newStock;

    @Column(name = "unit_cost", precision = 38, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "registered_by", nullable = false, length = 100)
    private String registeredBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
