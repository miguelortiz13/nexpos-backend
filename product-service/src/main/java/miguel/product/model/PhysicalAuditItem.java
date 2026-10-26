package miguel.product.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "physical_audit_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhysicalAuditItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PhysicalAuditSession session;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Producto product;

    @Column(name = "system_stock", nullable = false)
    private Integer systemStock;

    @Column(name = "physical_stock", nullable = false)
    private Integer physicalStock;

    @Column(nullable = false)
    private Integer difference; // physical_stock - system_stock

    @Column(name = "unit_cost", precision = 38, scale = 2, nullable = false)
    private BigDecimal unitCost;

    @Column(name = "cost_difference", precision = 38, scale = 2, nullable = false)
    private BigDecimal costDifference; // difference * unit_cost

    @Column(length = 255)
    private String notes;

    @Column(name = "counted_at", nullable = false)
    private LocalDateTime countedAt;

    @PrePersist
    public void prePersist() {
        if (countedAt == null) {
            countedAt = LocalDateTime.now();
        }
        if (difference == null) {
            difference = physicalStock - systemStock;
        }
        if (costDifference == null && unitCost != null && difference != null) {
            costDifference = unitCost.multiply(BigDecimal.valueOf(difference));
        }
    }
}
