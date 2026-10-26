package miguel.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import miguel.product.model.PhysicalAuditItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhysicalAuditItemDTO {
    private Long id;
    private Long sessionId;
    private Long productId;
    private String productCode;
    private String productName;
    private String productCategory;
    private Integer systemStock;
    private Integer physicalStock;
    private Integer difference;
    private BigDecimal unitCost;
    private BigDecimal costDifference;
    private String notes;
    private LocalDateTime countedAt;

    public static PhysicalAuditItemDTO fromEntity(PhysicalAuditItem item) {
        return PhysicalAuditItemDTO.builder()
                .id(item.getId())
                .sessionId(item.getSession().getId())
                .productId(item.getProduct().getId())
                .productCode(item.getProduct().getCodigoBarras())
                .productName(item.getProduct().getNombre())
                .productCategory(item.getProduct().getCategoria())
                .systemStock(item.getSystemStock())
                .physicalStock(item.getPhysicalStock())
                .difference(item.getDifference())
                .unitCost(item.getUnitCost())
                .costDifference(item.getCostDifference())
                .notes(item.getNotes())
                .countedAt(item.getCountedAt())
                .build();
    }
}
