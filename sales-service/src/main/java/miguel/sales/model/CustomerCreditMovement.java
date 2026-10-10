package miguel.sales.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_credit_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCreditMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"notes", "createdAt", "updatedAt"})
    private Customer customer;

    @Column(name = "sale_id")
    private Long saleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private CreditMovementType movementType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "previous_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal previousBalance;

    @Column(name = "new_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal newBalance;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod; // EFECTIVO, TRANSFERENCIA, TARJETA

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    @Column(length = 255)
    private String notes;

    @Column(name = "cash_shift_id")
    private Long cashShiftId;

    @Column(name = "registered_by", nullable = false, length = 100)
    private String registeredBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
