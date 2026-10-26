package miguel.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import miguel.product.model.WasteType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryWasteRequest {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productId;

    @NotNull(message = "La cantidad a dar de baja es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1")
    private Integer quantity;

    @NotNull(message = "El tipo de merma es obligatorio")
    private WasteType wasteType;

    @NotBlank(message = "El motivo de la merma es obligatorio")
    private String reason;

    private Long batchId; // Opcional: si la merma proviene de un lote específico
}
