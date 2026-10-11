package miguel.product.controller;

import lombok.RequiredArgsConstructor;
import miguel.product.dto.ForecastingSummaryDTO;
import miguel.product.dto.SupplierOrderSuggestionDTO;
import miguel.product.service.StockForecastingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forecasting")
@RequiredArgsConstructor
public class StockForecastingController {

    private final StockForecastingService forecastingService;

    @GetMapping("/analysis")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ForecastingSummaryDTO> getAnalysis(
            @RequestParam(defaultValue = "14") int days,
            @RequestParam(defaultValue = "15") int targetDays
    ) {
        return ResponseEntity.ok(forecastingService.generateForecast(days, targetDays));
    }

    @GetMapping("/supplier-order/{supplierId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SupplierOrderSuggestionDTO> getSupplierOrder(
            @PathVariable Long supplierId,
            @RequestParam(defaultValue = "14") int days,
            @RequestParam(defaultValue = "15") int targetDays
    ) {
        ForecastingSummaryDTO summary = forecastingService.generateForecast(days, targetDays);
        return summary.getSupplierGroups().stream()
                .filter(g -> supplierId.equals(g.getSupplierId()))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
