package miguel.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequest {

    @NotNull(message = "El proveedor es obligatorio")
    private Long supplierId;

    private String supplierInvoiceNumber;

    @Builder.Default
    private String paymentMethod = "EFECTIVO"; // EFECTIVO, TRANSFERENCIA, CREDITO_PROVEEDOR, OTRO

    private LocalDate paymentDueDate;

    private String notes;

    @NotEmpty(message = "Debe incluir al menos un producto en la compra")
    @Valid
    private List<PurchaseItemRequest> items;
}
