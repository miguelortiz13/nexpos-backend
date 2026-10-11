package miguel.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseItemRequest {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productId;

    @NotNull(message = "La cantidad recibida es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1")
    private Integer quantity;

    @NotNull(message = "El costo unitario de compra es obligatorio")
    @DecimalMin(value = "0.01", message = "El costo unitario debe ser mayor a 0")
    private BigDecimal unitCost;

    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("0.19"); // 0.00, 0.05, 0.19
}
