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
    private String customerEmail;
    private String paymentMethod;
    private BigDecimal amountPaid;
    private BigDecimal cashAmount;
    private BigDecimal cardAmount;
    private BigDecimal transferAmount;
    private BigDecimal otherAmount;
    private List<SaleItemRequest> items;
}
