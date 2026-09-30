package miguel.sales.controller;

import lombok.RequiredArgsConstructor;
import miguel.sales.dto.CashMovementRequest;
import miguel.sales.dto.CloseShiftRequest;
import miguel.sales.dto.OpenShiftRequest;
import miguel.sales.dto.ShiftSummaryResponse;
import miguel.sales.model.CashMovement;
import miguel.sales.model.CashShift;
import miguel.sales.service.CashShiftService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cash-shifts")
@RequiredArgsConstructor
public class CashShiftController {

    private final CashShiftService cashShiftService;

    @GetMapping("/active")
    public ResponseEntity<CashShift> getActiveShift(Principal principal) {
        String username = principal != null ? principal.getName() : "cajero_pos";
        Optional<CashShift> shift = cashShiftService.getActiveShift(username);
        return shift.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/open")
    public ResponseEntity<CashShift> openShift(@RequestBody OpenShiftRequest request, Principal principal) {
        String username = principal != null ? principal.getName() : "cajero_pos";
        CashShift shift = cashShiftService.openShift(username, request);
        return new ResponseEntity<>(shift, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/movements")
    public ResponseEntity<CashMovement> registerMovement(
            @PathVariable Long id,
            @RequestBody CashMovementRequest request,
            Principal principal) {
        String username = principal != null ? principal.getName() : "cajero_pos";
        CashMovement movement = cashShiftService.registerMovement(id, username, request);
        return new ResponseEntity<>(movement, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<ShiftSummaryResponse> getShiftSummary(@PathVariable Long id) {
        return ResponseEntity.ok(cashShiftService.getShiftSummary(id));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ShiftSummaryResponse> closeShift(
            @PathVariable Long id,
            @RequestBody CloseShiftRequest request,
            Principal principal) {
        String username = principal != null ? principal.getName() : "cajero_pos";
        return ResponseEntity.ok(cashShiftService.closeShift(id, username, request));
    }

    @GetMapping
    public ResponseEntity<List<CashShift>> getAllShifts() {
        return ResponseEntity.ok(cashShiftService.getAllShifts());
    }
}
