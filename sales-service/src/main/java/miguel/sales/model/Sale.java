package miguel.sales.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime saleDate;

    private Long customerId;

    @Column(length = 150)
    private String customerName;

    @Column(length = 50)
    private String customerDoc;

    @Column(name = "customer_email", length = 150)
    private String customerEmail;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 30)
    private String paymentMethod; // EFECTIVO, TARJETA, TRANSFERENCIA, MIXTO

    @Column(precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(precision = 12, scale = 2)
    private BigDecimal changeAmount;

    @Column(name = "cash_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cashAmount = BigDecimal.ZERO;

    @Column(name = "card_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cardAmount = BigDecimal.ZERO;

    @Column(name = "transfer_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal transferAmount = BigDecimal.ZERO;

    @Column(name = "other_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal otherAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(name = "cash_shift_id")
    private Long cashShiftId;

    @Column(name = "cashier_username", length = 100)
    private String cashierUsername;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "COMPLETED"; // COMPLETED, ANNULLED

    @Column(name = "payment_status", length = 30)
    @Builder.Default
    private String paymentStatus = "PAID"; // PAID, PENDING_CREDIT, PARTIALLY_PAID, ANNULLED

    @Column(name = "annulled_at")
    private LocalDateTime annulledAt;

    @Column(name = "annulled_by", length = 100)
    private String annulledBy;

    @Column(name = "annulment_reason", length = 255)
    private String annulmentReason;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<SaleItem> items;

    @OneToOne(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Invoice invoice;

    @OneToOne(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private CreditNote creditNote;
}
