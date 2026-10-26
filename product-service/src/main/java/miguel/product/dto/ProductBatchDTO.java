package miguel.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import miguel.product.model.BatchStatus;
import miguel.product.model.ProductBatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductBatchDTO {
    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private String productCategory;
    private String batchNumber;
    private LocalDate expirationDate;
    private Integer initialQuantity;
    private Integer currentQuantity;
    private BatchStatus status;
    private Long supplierId;
    private String supplierName;
    private LocalDate receivedAt;
    private LocalDateTime createdAt;
    private long daysRemaining;
    private String urgencyLevel; // VENCIDO, CRITICO (<=7d), ALERTA (<=15d), NORMAL

    public static ProductBatchDTO fromEntity(ProductBatch batch) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpirationDate());
        String urgency;
        if (days < 0) {
            urgency = "VENCIDO";
        } else if (days <= 7) {
            urgency = "CRITICO";
        } else if (days <= 15) {
            urgency = "ALERTA";
        } else {
            urgency = "NORMAL";
        }

        return ProductBatchDTO.builder()
                .id(batch.getId())
                .productId(batch.getProduct().getId())
                .productCode(batch.getProduct().getCodigoBarras())
                .productName(batch.getProduct().getNombre())
                .productCategory(batch.getProduct().getCategoria())
                .batchNumber(batch.getBatchNumber())
                .expirationDate(batch.getExpirationDate())
                .initialQuantity(batch.getInitialQuantity())
                .currentQuantity(batch.getCurrentQuantity())
                .status(batch.getStatus())
                .supplierId(batch.getSupplier() != null ? batch.getSupplier().getId() : null)
                .supplierName(batch.getSupplier() != null ? batch.getSupplier().getCompanyName() : null)
                .receivedAt(batch.getReceivedAt())
                .createdAt(batch.getCreatedAt())
                .daysRemaining(days)
                .urgencyLevel(urgency)
                .build();
    }
}
