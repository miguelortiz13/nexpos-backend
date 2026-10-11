package miguel.product.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_number", nullable = false, unique = true, length = 50)
    private String purchaseNumber; // Ej: COMP-0001

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Supplier supplier;

    @Column(name = "supplier_invoice_number", length = 100)
    private String supplierInvoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PurchaseStatus status = PurchaseStatus.RECEIVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private PurchasePaymentStatus paymentStatus = PurchasePaymentStatus.PAID;

    @Column(name = "payment_method", nullable = false, length = 30)
    @Builder.Default
    private String paymentMethod = "EFECTIVO"; // EFECTIVO, TRANSFERENCIA, CREDITO_PROVEEDOR, OTRO

    @Column(name = "payment_due_date")
    private LocalDate paymentDueDate;

    @Column(precision = 38, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 38, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 38, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @Column(name = "registered_by", nullable = false, length = 100)
    private String registeredBy;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private List<PurchaseItem> items = new ArrayList<>();

    public void addItem(PurchaseItem item) {
        items.add(item);
        item.setPurchase(this);
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PurchaseStatus.RECEIVED;
        }
        if (paymentStatus == null) {
            paymentStatus = PurchasePaymentStatus.PAID;
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            paymentMethod = "EFECTIVO";
        }
        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }
        if (taxAmount == null) {
            taxAmount = BigDecimal.ZERO;
        }
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
    }
}
