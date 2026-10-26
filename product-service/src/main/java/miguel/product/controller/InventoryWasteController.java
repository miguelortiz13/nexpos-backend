package miguel.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import miguel.product.dto.InventoryWasteDTO;
import miguel.product.dto.InventoryWasteRequest;
import miguel.product.dto.WasteSummaryDTO;
import miguel.product.model.WasteType;
import miguel.product.service.InventoryWasteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/wastes")
@RequiredArgsConstructor
public class InventoryWasteController {

    private final InventoryWasteService wasteService;

    // POST: Registrar merma / baja de producto
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<InventoryWasteDTO> registerWaste(
            @Valid @RequestBody InventoryWasteRequest request,
            Principal principal) {
        String username = principal != null ? principal.getName() : "ADMIN";
        InventoryWasteDTO waste = wasteService.registerWaste(request, username);
        return new ResponseEntity<>(waste, HttpStatus.CREATED);
    }

    // GET: Listar mermas con filtros opcionales por tipo o producto
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<InventoryWasteDTO>> getWastes(
            @RequestParam(required = false) WasteType type,
            @RequestParam(required = false) Long productId) {
        return ResponseEntity.ok(wasteService.getAllWastes(type, productId));
    }

    // GET: Resumen consolidado y valorización económica de mermas
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<WasteSummaryDTO> getWasteSummary() {
        return ResponseEntity.ok(wasteService.getWasteSummary());
    }
}
