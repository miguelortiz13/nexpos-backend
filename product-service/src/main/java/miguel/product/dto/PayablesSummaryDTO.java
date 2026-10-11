package miguel.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayablesSummaryDTO {

    private BigDecimal totalPendingAmount;
    private long pendingInvoicesCount;
    private long overdueInvoicesCount;
    private long upcomingInvoicesCount;
}
