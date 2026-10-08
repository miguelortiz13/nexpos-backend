package miguel.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnulSaleRequest {
    private String reason;
    private String conceptCode; // 2: Anulación total, 1: Devolución
    private Boolean refundCash; // True si se reembolsa en efectivo de caja
}
