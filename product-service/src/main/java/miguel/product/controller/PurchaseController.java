package miguel.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import miguel.product.dto.PayablesSummaryDTO;
import miguel.product.dto.PurchaseRequest;
import miguel.product.dto.PurchaseResponse;
import miguel.product.model.PurchasePaymentStatus;
import miguel.product.model.PurchaseStatus;
import miguel.product.service.PurchaseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    public ResponseEntity<List<PurchaseResponse>> getPurchases(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) PurchaseStatus status,
            @RequestParam(required = false) PurchasePaymentStatus paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return ResponseEntity.ok(purchaseService.getPurchases(supplierId, status, paymentStatus, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponse> getPurchaseById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.getPurchaseById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    public ResponseEntity<PurchaseResponse> createPurchase(
            @Valid @RequestBody PurchaseRequest request,
            Authentication authentication
    ) {
        String username = (authentication != null) ? authentication.getName() : "admin";
        PurchaseResponse response = purchaseService.createPurchase(request, username);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PurchaseResponse> cancelPurchase(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication
    ) {
        String username = (authentication != null) ? authentication.getName() : "admin";
        String reason = (body != null) ? body.get("reason") : "Anulación de compra";
        return ResponseEntity.ok(purchaseService.cancelPurchase(id, reason, username));
    }

    @GetMapping("/payables-summary")
    public ResponseEntity<PayablesSummaryDTO> getPayablesSummary() {
        return ResponseEntity.ok(purchaseService.getPayablesSummary());
    }
}
