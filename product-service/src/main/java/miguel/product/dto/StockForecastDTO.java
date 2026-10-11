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
public class StockForecastDTO {

    private Long productId;
    private String productName;
    private String barcode;
    private String category;
    private int currentStock;
    private int minStock;
    private BigDecimal unitCost;
    private BigDecimal salePrice;

    // Métricas de rotación
    private int totalSoldInPeriod;
    private double dailySaleVelocity; // Unidades vendidas / día
    private Double estimatedDaysRemaining; // Días de cobertura restante (null si no hay ventas)

    // Clasificación de Riesgo: OUT_OF_STOCK, CRITICAL, LOW_STOCK, MODERATE, HEALTHY, NO_MOVEMENT
    private String stockStatus;

    // Recomendación de Reabastecimiento
    private int suggestedOrderQty;
    private BigDecimal estimatedOrderCost;

    // Proveedor sugerido (más reciente)
    private Long suggestedSupplierId;
    private String suggestedSupplierName;
    private String suggestedSupplierPhone;
    private String suggestedSupplierEmail;
}
