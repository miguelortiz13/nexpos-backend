package miguel.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import miguel.product.dto.SupplierDTO;
import miguel.product.service.SupplierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<List<SupplierDTO>> getSuppliers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        return ResponseEntity.ok(supplierService.getSuppliers(query, activeOnly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierDTO> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    public ResponseEntity<SupplierDTO> createSupplier(@Valid @RequestBody SupplierDTO dto) {
        return new ResponseEntity<>(supplierService.createSupplier(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    public ResponseEntity<SupplierDTO> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierDTO dto
    ) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, dto));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupplierDTO> toggleSupplierStatus(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.toggleSupplierStatus(id));
    }
}
