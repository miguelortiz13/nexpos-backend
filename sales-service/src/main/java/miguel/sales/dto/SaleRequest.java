package miguel.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleRequest {
    private Long customerId;
    private String customerName;
    private String customerDoc;
    private String paymentMethod;
    private BigDecimal amountPaid;
    private List<SaleItemRequest> items;
}
