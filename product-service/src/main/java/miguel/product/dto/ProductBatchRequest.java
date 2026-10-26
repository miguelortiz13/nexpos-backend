package miguel.product.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductBatchRequest {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productId;

    @NotBlank(message = "El número o código de lote es obligatorio")
    private String batchNumber;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate expirationDate;

    @NotNull(message = "La cantidad inicial es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer initialQuantity;

    private Long supplierId;

    private LocalDate receivedAt;
}
