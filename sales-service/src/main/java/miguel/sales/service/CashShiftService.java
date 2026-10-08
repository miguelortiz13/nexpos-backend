package miguel.sales.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import miguel.sales.dto.CashMovementRequest;
import miguel.sales.dto.CloseShiftRequest;
import miguel.sales.dto.OpenShiftRequest;
import miguel.sales.dto.ShiftSummaryResponse;
import miguel.sales.model.*;
import miguel.sales.repository.CashMovementRepository;
import miguel.sales.repository.CashShiftRepository;
import miguel.sales.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CashShiftService {

    private final CashShiftRepository cashShiftRepository;
    private final CashMovementRepository cashMovementRepository;
    private final SaleRepository saleRepository;

    /**
     * Obtiene el turno de caja actualmente abierto para un usuario, o cualquier turno abierto en la terminal.
     */
    public Optional<CashShift> getActiveShift(String username) {
        Optional<CashShift> userShift = cashShiftRepository
                .findFirstByCashierUsernameAndStatusOrderByOpenedAtDesc(username, ShiftStatus.OPEN);
        if (userShift.isPresent()) {
            return userShift;
        }
        // Fallback: verificar si hay un turno abierto global en la terminal
        return cashShiftRepository.findFirstByStatusOrderByOpenedAtDesc(ShiftStatus.OPEN);
    }

    /**
     * Abre un nuevo turno de caja con la base inicial de efectivo.
     */
    @Transactional
    public CashShift openShift(String username, OpenShiftRequest request) {
        Optional<CashShift> existing = getActiveShift(username);
        if (existing.isPresent()) {
            throw new IllegalStateException("Ya existe un turno de caja abierto (ID #" + existing.get().getId() + ") para este usuario o terminal.");
        }

        BigDecimal initialAmount = (request.getInitialAmount() != null && request.getInitialAmount().compareTo(BigDecimal.ZERO) >= 0)
                ? request.getInitialAmount()
                : BigDecimal.ZERO;

        CashShift shift = CashShift.builder()
                .cashierUsername(username != null && !username.isBlank() ? username : "cajero_pos")
                .openedAt(LocalDateTime.now())
                .status(ShiftStatus.OPEN)
                .initialAmount(initialAmount)
                .expectedCashAmount(initialAmount)
                .totalSalesCash(BigDecimal.ZERO)
                .totalSalesCard(BigDecimal.ZERO)
                .totalSalesTransfer(BigDecimal.ZERO)
                .totalSalesOther(BigDecimal.ZERO)
                .totalSalesAmount(BigDecimal.ZERO)
                .totalSalesCount(0)
                .totalEntriesAmount(BigDecimal.ZERO)
                .totalExitsAmount(BigDecimal.ZERO)
                .notes(request.getNotes())
                .build();

        return cashShiftRepository.save(shift);
    }

    /**
     * Registra un movimiento de efectivo (Entrada extra o Retiro/Gasto) en el turno activo.
     */
    @Transactional
    public CashMovement registerMovement(Long shiftId, String username, CashMovementRequest request) {
        CashShift shift = cashShiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Turno de caja no encontrado con ID: " + shiftId));

        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new IllegalStateException("No se pueden registrar movimientos en un turno de caja que ya está cerrado.");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del movimiento debe ser superior a 0.");
        }

        if (request.getType() == null) {
            throw new IllegalArgumentException("El tipo de movimiento (ENTRY o EXIT) es obligatorio.");
        }

        if (request.getType() == CashMovementType.EXIT) {
            if (shift.getExpectedCashAmount().compareTo(request.getAmount()) < 0) {
                throw new IllegalArgumentException("Fondos insuficientes: El monto de retiro ($" +
                        request.getAmount() + ") supera el efectivo disponible en caja ($" +
                        shift.getExpectedCashAmount() + ").");
            }
            shift.setTotalExitsAmount(shift.getTotalExitsAmount().add(request.getAmount()));
            shift.setExpectedCashAmount(shift.getExpectedCashAmount().subtract(request.getAmount()));
        } else {
            shift.setTotalEntriesAmount(shift.getTotalEntriesAmount().add(request.getAmount()));
            shift.setExpectedCashAmount(shift.getExpectedCashAmount().add(request.getAmount()));
        }

        cashShiftRepository.save(shift);

        CashMovement movement = CashMovement.builder()
                .shift(shift)
                .type(request.getType())
                .amount(request.getAmount())
                .reason(request.getReason() != null && !request.getReason().isBlank() ? request.getReason() : "Sin descripción")
                .registeredBy(username != null ? username : "usuario")
                .createdAt(LocalDateTime.now())
                .build();

        return cashMovementRepository.save(movement);
    }

    /**
     * Acumula una venta al turno de caja activo (llamado desde SaleService).
     */
    @Transactional
    public void addSaleToActiveShift(String username, Sale sale) {
        Optional<CashShift> shiftOpt = getActiveShift(username);
        if (shiftOpt.isEmpty()) {
            return; // Si no hay turno abierto, no falla la venta, pero no se asocia a arqueo
        }

        CashShift shift = shiftOpt.get();
        sale.setCashShiftId(shift.getId());
        sale.setCashierUsername(shift.getCashierUsername());

        shift.setTotalSalesCount(shift.getTotalSalesCount() + 1);
        shift.setTotalSalesAmount(shift.getTotalSalesAmount().add(sale.getTotalAmount()));

        BigDecimal cash = sale.getCashAmount() != null ? sale.getCashAmount() : BigDecimal.ZERO;
        BigDecimal card = sale.getCardAmount() != null ? sale.getCardAmount() : BigDecimal.ZERO;
        BigDecimal transfer = sale.getTransferAmount() != null ? sale.getTransferAmount() : BigDecimal.ZERO;
        BigDecimal other = sale.getOtherAmount() != null ? sale.getOtherAmount() : BigDecimal.ZERO;

        // Fallback si por alguna razón no se llenaron las porciones individuales
        if (cash.compareTo(BigDecimal.ZERO) == 0 && card.compareTo(BigDecimal.ZERO) == 0 &&
                transfer.compareTo(BigDecimal.ZERO) == 0 && other.compareTo(BigDecimal.ZERO) == 0) {
            String method = sale.getPaymentMethod() != null ? sale.getPaymentMethod().toUpperCase() : "EFECTIVO";
            switch (method) {
                case "TARJETA":
                    card = sale.getTotalAmount();
                    break;
                case "TRANSFERENCIA":
                    transfer = sale.getTotalAmount();
                    break;
                case "EFECTIVO":
                    cash = sale.getTotalAmount();
                    break;
                default:
                    other = sale.getTotalAmount();
                    break;
            }
        }

        shift.setTotalSalesCash(shift.getTotalSalesCash().add(cash));
        shift.setExpectedCashAmount(shift.getExpectedCashAmount().add(cash));
        shift.setTotalSalesCard(shift.getTotalSalesCard().add(card));
        shift.setTotalSalesTransfer(shift.getTotalSalesTransfer().add(transfer));
        shift.setTotalSalesOther(shift.getTotalSalesOther().add(other));

        cashShiftRepository.save(shift);
    }

    /**
     * Genera el arqueo actual (Reporte X o Reporte Z) con rango de facturas y movimientos.
     */
    public ShiftSummaryResponse getShiftSummary(Long shiftId) {
        CashShift shift = cashShiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Turno de caja no encontrado con ID: " + shiftId));

        List<CashMovement> movements = cashMovementRepository.findByShiftIdOrderByCreatedAtDesc(shiftId);
        List<Sale> sales = saleRepository.findByCashShiftIdOrderByIdAsc(shiftId);

        String firstInvoice = null;
        String lastInvoice = null;
        if (!sales.isEmpty()) {
            Sale first = sales.get(0);
            firstInvoice = (first.getInvoice() != null && first.getInvoice().getInvoiceNumber() != null)
                    ? first.getInvoice().getInvoiceNumber()
                    : "FAC-" + first.getId();
            Sale last = sales.get(sales.size() - 1);
            lastInvoice = (last.getInvoice() != null && last.getInvoice().getInvoiceNumber() != null)
                    ? last.getInvoice().getInvoiceNumber()
                    : "FAC-" + last.getId();
        }

        return ShiftSummaryResponse.builder()
                .shiftId(shift.getId())
                .cashierUsername(shift.getCashierUsername())
                .openedAt(shift.getOpenedAt())
                .closedAt(shift.getClosedAt())
                .status(shift.getStatus())
                .initialAmount(shift.getInitialAmount())
                .totalSalesCash(shift.getTotalSalesCash())
                .totalSalesCard(shift.getTotalSalesCard())
                .totalSalesTransfer(shift.getTotalSalesTransfer())
                .totalSalesOther(shift.getTotalSalesOther())
                .totalSalesAmount(shift.getTotalSalesAmount())
                .totalSalesCount(shift.getTotalSalesCount())
                .totalEntriesAmount(shift.getTotalEntriesAmount())
                .totalExitsAmount(shift.getTotalExitsAmount())
                .expectedCashAmount(shift.getExpectedCashAmount())
                .actualCashAmount(shift.getActualCashAmount())
                .differenceAmount(shift.getDifferenceAmount())
                .notes(shift.getNotes())
                .closeNotes(shift.getCloseNotes())
                .firstInvoiceNumber(firstInvoice)
                .lastInvoiceNumber(lastInvoice)
                .movements(movements)
                .build();
    }

    /**
     * Cierra el turno de caja (Reporte Z) calculando diferencias entre lo esperado y lo contado.
     */
    @Transactional
    public ShiftSummaryResponse closeShift(Long shiftId, String username, CloseShiftRequest request) {
        CashShift shift = cashShiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Turno de caja no encontrado con ID: " + shiftId));

        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new IllegalStateException("El turno de caja #" + shiftId + " ya se encuentra cerrado.");
        }

        BigDecimal actualCash = (request.getActualCashAmount() != null)
                ? request.getActualCashAmount()
                : BigDecimal.ZERO;

        BigDecimal difference = actualCash.subtract(shift.getExpectedCashAmount());

        shift.setClosedAt(LocalDateTime.now());
        shift.setStatus(ShiftStatus.CLOSED);
        shift.setActualCashAmount(actualCash);
        shift.setDifferenceAmount(difference);
        shift.setCloseNotes(request.getCloseNotes());

        cashShiftRepository.save(shift);

        return getShiftSummary(shiftId);
    }

    /**
     * Lista todos los turnos ordenados por fecha descendente.
     */
    public List<CashShift> getAllShifts() {
        return cashShiftRepository.findAllByOrderByOpenedAtDesc();
    }
}
