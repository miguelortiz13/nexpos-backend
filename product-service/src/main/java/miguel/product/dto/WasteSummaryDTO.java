package miguel.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import miguel.product.model.WasteType;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasteSummaryDTO {
    private long totalRecords;
    private int totalUnitsLost;
    private BigDecimal totalCostLost;
    private Map<WasteType, Integer> unitsByType;
    private Map<WasteType, BigDecimal> costByType;
}
