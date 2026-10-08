package miguel.sales.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "credit_note_number", nullable = false, unique = true, length = 100)
    private String creditNoteNumber; // e.g. NC-POS-1

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber; // Referencia a factura original

    @Column(name = "original_cude", length = 255)
    private String originalCude;

    @Column(name = "cude", length = 255)
    private String cude; // CUDE de la Nota Crédito según estándar DIAN

    @Column(name = "qr_data", columnDefinition = "TEXT")
    private String qrData;

    @Column(name = "factus_bill_id", length = 100)
    private String factusBillId;

    @Column(name = "factus_status", nullable = false, length = 50)
    @Builder.Default
    private String factusStatus = "VALIDATED";

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "concept_code", nullable = false, length = 10)
    @Builder.Default
    private String conceptCode = "2"; // 2 = Anulación de factura electrónica

    @Column(name = "concept_description", nullable = false, length = 100)
    @Builder.Default
    private String conceptDescription = "Anulación de factura electrónica";

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "refund_cash", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal refundCash = BigDecimal.ZERO;

    @Column(name = "refund_other", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal refundOther = BigDecimal.ZERO;

    @Column(name = "cash_shift_id")
    private Long cashShiftId;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "dian_response_message", columnDefinition = "TEXT")
    private String dianResponseMessage;

    @OneToOne
    @JoinColumn(name = "sale_id", nullable = false)
    @JsonIgnore
    private Sale sale;
}
