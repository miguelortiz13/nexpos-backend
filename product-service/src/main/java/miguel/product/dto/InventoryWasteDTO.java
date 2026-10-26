package miguel.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import miguel.product.model.InventoryWaste;
import miguel.product.model.WasteType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryWasteDTO {
    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private String productCategory;
    private Integer quantity;
    private WasteType wasteType;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String reason;
    private String registeredBy;
    private LocalDateTime createdAt;

    public static InventoryWasteDTO fromEntity(InventoryWaste waste) {
        return InventoryWasteDTO.builder()
                .id(waste.getId())
                .productId(waste.getProduct().getId())
                .productCode(waste.getProduct().getCodigoBarras())
                .productName(waste.getProduct().getNombre())
                .productCategory(waste.getProduct().getCategoria())
                .quantity(waste.getQuantity())
                .wasteType(waste.getWasteType())
                .unitCost(waste.getUnitCost())
                .totalCost(waste.getTotalCost())
                .reason(waste.getReason())
                .registeredBy(waste.getRegisteredBy())
                .createdAt(waste.getCreatedAt())
                .build();
    }
}
