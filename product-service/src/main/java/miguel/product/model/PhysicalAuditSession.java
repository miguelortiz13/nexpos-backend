package miguel.product.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "physical_audit_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhysicalAuditSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_code", length = 50, nullable = false, unique = true)
    private String sessionCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private AuditSessionStatus status = AuditSessionStatus.EN_CONTEO;

    @Column(name = "auditor_username", length = 100, nullable = false)
    private String auditorUsername;

    @Column(length = 500)
    private String notes;

    @Column(name = "total_items_counted", nullable = false)
    @Builder.Default
    private Integer totalItemsCounted = 0;

    @Column(name = "total_difference_units", nullable = false)
    @Builder.Default
    private Integer totalDifferenceUnits = 0;

    @Column(name = "total_cost_impact", precision = 38, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalCostImpact = BigDecimal.ZERO;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<PhysicalAuditItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = AuditSessionStatus.EN_CONTEO;
        }
    }
}
