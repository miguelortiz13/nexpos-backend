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
public class PurchaseItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productBarcode;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal subtotal;
    private BigDecimal total;

    // Métricas PMP comparativas
    private BigDecimal previousCost;
    private BigDecimal newCostPmp;
    private Integer previousStock;
    private Integer newStock;
}
