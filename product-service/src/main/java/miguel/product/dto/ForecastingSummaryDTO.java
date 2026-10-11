package miguel.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForecastingSummaryDTO {

    private int daysAnalyzed;
    private int targetCoverageDays;
    private int totalProducts;

    // Métricas globales KPI
    private int outOfStockCount;
    private int criticalCount;
    private int lowStockCount;
    private int reorderRequiredCount;
    private BigDecimal totalInvestmentRequired;

    // Lista detallada de productos analizados
    @Builder.Default
    private List<StockForecastDTO> items = new ArrayList<>();

    // Agrupación automática por proveedor
    @Builder.Default
    private List<SupplierOrderSuggestionDTO> supplierGroups = new ArrayList<>();
}
