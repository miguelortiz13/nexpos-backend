package miguel.sales.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nit;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "trade_name", length = 200)
    private String tradeName;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "tax_regime", nullable = false, length = 50)
    private String taxRegime; // RESPONSABLE_IVA, NO_RESPONSABLE_IVA

    // Configuración de Resolución DIAN
    @Column(name = "dian_resolution_number", nullable = false, length = 100)
    private String dianResolutionNumber;

    @Column(name = "dian_prefix", nullable = false, length = 10)
    private String dianPrefix;

    @Column(name = "dian_range_from", nullable = false)
    private Long dianRangeFrom;

    @Column(name = "dian_range_to", nullable = false)
    private Long dianRangeTo;

    @Column(name = "dian_current_number", nullable = false)
    private Long dianCurrentNumber;

    @Column(name = "dian_nc_prefix", length = 10)
    @Builder.Default
    private String dianNcPrefix = "NC";

    @Column(name = "dian_nc_current_number")
    @Builder.Default
    private Long dianNcCurrentNumber = 1L;

    @Column(name = "credit_receipt_prefix", length = 10)
    @Builder.Default
    private String creditReceiptPrefix = "RC";

    @Column(name = "credit_receipt_current_number")
    @Builder.Default
    private Long creditReceiptCurrentNumber = 1L;

    @Column(name = "dian_technical_key", length = 255)
    private String dianTechnicalKey;

    @Column(name = "dian_start_date")
    private LocalDate dianStartDate;

    @Column(name = "dian_end_date")
    private LocalDate dianEndDate;

    // Conexión Proveedor Tecnológico Factus API
    @Column(name = "factus_api_url", nullable = false, length = 255)
    private String factusApiUrl;

    @Column(name = "factus_client_id", length = 150)
    private String factusClientId;

    @Column(name = "factus_client_secret", length = 255)
    private String factusClientSecret;

    @Column(name = "factus_api_token", columnDefinition = "TEXT")
    private String factusApiToken;

    @Column(name = "facturacion_activa", nullable = false)
    private Boolean facturacionActiva;

    @Column(nullable = false, length = 20)
    private String environment; // SANDBOX, PRODUCTION

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
