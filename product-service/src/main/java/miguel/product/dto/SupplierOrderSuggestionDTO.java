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
public class SupplierOrderSuggestionDTO {

    private Long supplierId;
    private String supplierName;
    private String supplierNit;
    private String supplierPhone;
    private String supplierEmail;

    @Builder.Default
    private int itemCount = 0;

    @Builder.Default
    private int totalUnits = 0;

    @Builder.Default
    private BigDecimal totalEstimatedCost = BigDecimal.ZERO;

    @Builder.Default
    private List<StockForecastDTO> items = new ArrayList<>();

    // Mensaje preformateado para envío directo a WhatsApp
    private String suggestedWhatsAppMessage;
}
