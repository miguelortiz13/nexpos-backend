package miguel.sales.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class CustomerCreditPaymentRequest {

    @NotNull(message = "El monto a abonar es obligatorio")
    @DecimalMin(value = "1.00", message = "El monto a abonar debe ser superior a 0")
    private BigDecimal amount;

    @NotBlank(message = "El medio de pago es obligatorio")
    private String paymentMethod; // EFECTIVO, TRANSFERENCIA, TARJETA

    private String notes;
}
