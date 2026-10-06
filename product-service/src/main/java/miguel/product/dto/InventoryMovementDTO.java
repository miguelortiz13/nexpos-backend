package miguel.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import miguel.product.model.InventoryMovement;
import miguel.product.model.InventoryMovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementDTO {

    private Long id;
    private Long productId;
    private String productName;
    private String productBarcode;
    private InventoryMovementType movementType;
    private Integer quantity;
    private Integer previousStock;
    private Integer newStock;
    private BigDecimal unitCost;
    private String reason;
    private String referenceId;
    private String registeredBy;
    private LocalDateTime createdAt;

    public static InventoryMovementDTO fromEntity(InventoryMovement movement) {
        return InventoryMovementDTO.builder()
                .id(movement.getId())
                .productId(movement.getProduct() != null ? movement.getProduct().getId() : null)
                .productName(movement.getProduct() != null ? movement.getProduct().getNombre() : "N/A")
                .productBarcode(movement.getProduct() != null ? movement.getProduct().getCodigoBarras() : "")
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .previousStock(movement.getPreviousStock())
                .newStock(movement.getNewStock())
                .unitCost(movement.getUnitCost())
                .reason(movement.getReason())
                .referenceId(movement.getReferenceId())
                .registeredBy(movement.getRegisteredBy())
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
