package miguel.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import miguel.product.dto.InventoryWasteDTO;
import miguel.product.dto.ProductBatchDTO;
import miguel.product.dto.ProductBatchRequest;
import miguel.product.service.ProductBatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/batches")
@RequiredArgsConstructor
public class ProductBatchController {

    private final ProductBatchService batchService;

    // POST: Registrar nuevo lote con fecha de vencimiento
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductBatchDTO> createBatch(@Valid @RequestBody ProductBatchRequest request) {
        ProductBatchDTO created = batchService.createBatch(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // GET: Listar lotes de un producto específico (ordenados por FEFO)
    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<ProductBatchDTO>> getBatchesByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(batchService.getBatchesByProduct(productId));
    }

    // GET: Listar lotes que vencen próximamente (alertas FEFO)
    @GetMapping("/expiring")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<ProductBatchDTO>> getExpiringBatches(
            @RequestParam(required = false, defaultValue = "15") Integer daysAhead) {
        return ResponseEntity.ok(batchService.getExpiringBatches(daysAhead));
    }

    // GET: Listar todos los lotes
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<ProductBatchDTO>> getAllBatches() {
        return ResponseEntity.ok(batchService.getAllActiveBatches());
    }

    // POST: Dar de baja lote completo por vencimiento
    @PostMapping("/{batchId}/discharge-waste")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<InventoryWasteDTO> dischargeBatchAsWaste(
            @PathVariable Long batchId,
            @RequestParam(required = false) String reason,
            Principal principal) {
        String username = principal != null ? principal.getName() : "ADMIN";
        InventoryWasteDTO waste = batchService.dischargeBatchAsWaste(batchId, reason, username);
        return ResponseEntity.ok(waste);
    }
}
