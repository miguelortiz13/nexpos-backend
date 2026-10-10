package miguel.sales.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", length = 255)
    private String invoiceNumber; // Número de factura oficial (e.g. POS-1001)

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    // Campos de Facturación Electrónica DIAN / Factus
    @Column(length = 255)
    private String cude; // Código Único de Documento Electrónico

    @Column(name = "qr_data", columnDefinition = "TEXT")
    private String qrData; // URL oficial DIAN para consulta de comprobante

    @Column(name = "factus_bill_id", length = 100)
    private String factusBillId; // ID en el proveedor Factus

    @Column(name = "factus_status", length = 50)
    private String factusStatus; // VALIDATED, PENDING, REJECTED, LOCAL_OFFLINE

    @Column(name = "xml_url", length = 255)
    private String xmlUrl;

    @Column(name = "pdf_url", length = 255)
    private String pdfUrl;

    @Column(name = "dian_response_message", columnDefinition = "TEXT")
    private String dianResponseMessage;

    @OneToOne
    @JoinColumn(name = "sale_id")
    @JsonIgnore
    // Referencia de vuelta: fuera de equals/hashCode/toString para evitar el
    // ciclo infinito con la entidad padre (StackOverflowError al crear ventas).
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Sale sale;
}
