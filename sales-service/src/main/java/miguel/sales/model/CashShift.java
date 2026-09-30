package miguel.sales.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cash_shifts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cashier_id")
    private Long cashierId;

    @Column(name = "cashier_username", nullable = false, length = 100)
    private String cashierUsername;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShiftStatus status = ShiftStatus.OPEN;

    @Column(name = "initial_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal initialAmount = BigDecimal.ZERO;

    @Column(name = "expected_cash_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal expectedCashAmount = BigDecimal.ZERO;

    @Column(name = "actual_cash_amount", precision = 12, scale = 2)
    private BigDecimal actualCashAmount;

    @Column(name = "difference_amount", precision = 12, scale = 2)
    private BigDecimal differenceAmount;

    @Column(name = "total_sales_cash", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSalesCash = BigDecimal.ZERO;

    @Column(name = "total_sales_card", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSalesCard = BigDecimal.ZERO;

    @Column(name = "total_sales_transfer", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSalesTransfer = BigDecimal.ZERO;

    @Column(name = "total_sales_other", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSalesOther = BigDecimal.ZERO;

    @Column(name = "total_sales_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSalesAmount = BigDecimal.ZERO;

    @Column(name = "total_sales_count", nullable = false)
    @Builder.Default
    private Integer totalSalesCount = 0;

    @Column(name = "total_entries_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalEntriesAmount = BigDecimal.ZERO;

    @Column(name = "total_exits_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalExitsAmount = BigDecimal.ZERO;

    @Column(length = 255)
    private String notes;

    @Column(name = "close_notes", length = 255)
    private String closeNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CashMovement> movements = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (openedAt == null) {
            openedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ShiftStatus.OPEN;
        }
        if (initialAmount == null) {
            initialAmount = BigDecimal.ZERO;
        }
        if (expectedCashAmount == null) {
            expectedCashAmount = initialAmount;
        }
    }
}
