package miguel.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import miguel.product.model.InventoryMovementType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementRequest {

    @NotNull(message = "El tipo de movimiento es obligatorio")
    private InventoryMovementType movementType;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer quantity;

    @NotBlank(message = "El motivo o justificación es obligatorio")
    private String reason;

    private String referenceId;

    private BigDecimal unitCost;
}
