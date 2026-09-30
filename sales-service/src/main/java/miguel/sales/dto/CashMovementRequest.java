package miguel.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import miguel.sales.model.CashMovementType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashMovementRequest {
    private CashMovementType type; // ENTRY, EXIT
    private BigDecimal amount;
    private String reason;
}
