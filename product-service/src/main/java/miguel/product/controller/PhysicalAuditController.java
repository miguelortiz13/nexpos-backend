package miguel.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import miguel.product.dto.CountItemRequest;
import miguel.product.dto.CreateAuditSessionRequest;
import miguel.product.dto.PhysicalAuditItemDTO;
import miguel.product.dto.PhysicalAuditSessionDTO;
import miguel.product.service.PhysicalAuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/audits")
@RequiredArgsConstructor
public class PhysicalAuditController {

    private final PhysicalAuditService auditService;

    // POST: Iniciar nueva sesión de auditoría / conteo físico
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PhysicalAuditSessionDTO> startSession(
            @RequestBody(required = false) CreateAuditSessionRequest request,
            Principal principal) {
        String username = principal != null ? principal.getName() : "ADMIN";
        PhysicalAuditSessionDTO session = auditService.startAuditSession(request, username);
        return new ResponseEntity<>(session, HttpStatus.CREATED);
    }

    // GET: Listar todas las sesiones de auditoría
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PhysicalAuditSessionDTO>> listSessions() {
        return ResponseEntity.ok(auditService.listSessions());
    }

    // GET: Obtener detalle completo de una sesión con sus ítems contados
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PhysicalAuditSessionDTO> getSession(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.getSessionById(id));
    }

    // POST: Registrar conteo de un producto en la sesión
    @PostMapping("/{id}/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PhysicalAuditItemDTO> recordCount(
            @PathVariable Long id,
            @Valid @RequestBody CountItemRequest request) {
        PhysicalAuditItemDTO item = auditService.recordItemCount(id, request);
        return ResponseEntity.ok(item);
    }

    // POST: Conciliar auditoría (actualiza stock y genera movimientos en Kardex)
    @PostMapping("/{id}/reconcile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PhysicalAuditSessionDTO> reconcileSession(
            @PathVariable Long id,
            Principal principal) {
        String username = principal != null ? principal.getName() : "ADMIN";
        PhysicalAuditSessionDTO session = auditService.reconcileAudit(id, username);
        return ResponseEntity.ok(session);
    }

    // POST: Cancelar sesión de auditoría
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PhysicalAuditSessionDTO> cancelSession(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.cancelAudit(id));
    }
}
