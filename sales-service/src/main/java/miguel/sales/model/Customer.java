package miguel.sales.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_type", nullable = false, length = 10)
    @Builder.Default
    private String docType = "CC"; // CC, NIT, CE, PASAPORTE, TI

    @Column(name = "doc_number", nullable = false, unique = true, length = 50)
    private String docNumber;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String department;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "credit_allowed", nullable = false)
    @Builder.Default
    private Boolean creditAllowed = false;

    @Column(name = "credit_limit", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal creditLimit = java.math.BigDecimal.ZERO;

    @Column(name = "current_debt", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal currentDebt = java.math.BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public java.math.BigDecimal getAvailableCredit() {
        java.math.BigDecimal limit = (creditLimit != null) ? creditLimit : java.math.BigDecimal.ZERO;
        java.math.BigDecimal debt = (currentDebt != null) ? currentDebt : java.math.BigDecimal.ZERO;
        return limit.subtract(debt).max(java.math.BigDecimal.ZERO);
    }
}
