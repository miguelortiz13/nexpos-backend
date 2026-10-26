package miguel.product.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import miguel.product.dto.CountItemRequest;
import miguel.product.dto.CreateAuditSessionRequest;
import miguel.product.dto.PhysicalAuditItemDTO;
import miguel.product.dto.PhysicalAuditSessionDTO;
import miguel.product.model.*;
import miguel.product.repository.InventoryMovementRepository;
import miguel.product.repository.PhysicalAuditItemRepository;
import miguel.product.repository.PhysicalAuditSessionRepository;
import miguel.product.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhysicalAuditService {

    private final PhysicalAuditSessionRepository sessionRepository;
    private final PhysicalAuditItemRepository itemRepository;
    private final ProductoRepository productoRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Transactional
    public PhysicalAuditSessionDTO startAuditSession(CreateAuditSessionRequest request, String auditorUsername) {
        long count = sessionRepository.count() + 1;
        String sessionCode = String.format("AUD-%d-%04d", LocalDateTime.now().getYear(), count);

        PhysicalAuditSession session = PhysicalAuditSession.builder()
                .sessionCode(sessionCode)
                .status(AuditSessionStatus.EN_CONTEO)
                .auditorUsername(auditorUsername != null ? auditorUsername : "ADMIN")
                .notes(request != null ? request.getNotes() : null)
                .startedAt(LocalDateTime.now())
                .totalItemsCounted(0)
                .totalDifferenceUnits(0)
                .totalCostImpact(BigDecimal.ZERO)
                .build();

        PhysicalAuditSession saved = sessionRepository.save(session);
        return PhysicalAuditSessionDTO.fromEntity(saved, false);
    }

    @Transactional
    public PhysicalAuditItemDTO recordItemCount(Long sessionId, CountItemRequest request) {
        PhysicalAuditSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Sesión de auditoría no encontrada con ID: " + sessionId));

        if (session.getStatus() != AuditSessionStatus.EN_CONTEO) {
            throw new IllegalStateException("No se pueden registrar conteos en una sesión con estado: " + session.getStatus());
        }

        Producto producto = productoRepository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + request.getProductId()));

        BigDecimal unitCost = producto.getCostPrice() != null && producto.getCostPrice().compareTo(BigDecimal.ZERO) > 0
                ? producto.getCostPrice()
                : producto.getPrecio();

        int systemStock = producto.getCantidad();
        int physicalStock = request.getPhysicalStock();
        int difference = physicalStock - systemStock;
        BigDecimal costDifference = unitCost.multiply(BigDecimal.valueOf(difference));

        // Verificar si ya se había contado este producto en la sesión actual
        Optional<PhysicalAuditItem> existingItemOpt = itemRepository.findBySessionIdAndProductId(sessionId, producto.getId());
        PhysicalAuditItem item;
        if (existingItemOpt.isPresent()) {
            item = existingItemOpt.get();
            item.setPhysicalStock(physicalStock);
            item.setDifference(difference);
            item.setCostDifference(costDifference);
            item.setNotes(request.getNotes());
            item.setCountedAt(LocalDateTime.now());
        } else {
            item = PhysicalAuditItem.builder()
                    .session(session)
                    .product(producto)
                    .systemStock(systemStock)
                    .physicalStock(physicalStock)
                    .difference(difference)
                    .unitCost(unitCost)
                    .costDifference(costDifference)
                    .notes(request.getNotes())
                    .countedAt(LocalDateTime.now())
                    .build();
        }

        PhysicalAuditItem savedItem = itemRepository.save(item);

        // Recalcular totales de la sesión
        recalculateSessionTotals(session);

        return PhysicalAuditItemDTO.fromEntity(savedItem);
    }

    @Transactional
    public PhysicalAuditSessionDTO reconcileAudit(Long sessionId, String username) {
        PhysicalAuditSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Sesión de auditoría no encontrada con ID: " + sessionId));

        if (session.getStatus() != AuditSessionStatus.EN_CONTEO) {
            throw new IllegalStateException("Solo sesiones con estado EN_CONTEO pueden ser conciliadas.");
        }

        List<PhysicalAuditItem> items = itemRepository.findBySessionIdOrderByCountedAtDesc(sessionId);

        // Aplicar los ajustes en inventario para productos que presenten diferencias
        for (PhysicalAuditItem item : items) {
            if (item.getDifference() != 0) {
                // Bloqueo pesimista para actualizar el stock real atómicamente
                Producto producto = productoRepository.findByIdForUpdate(item.getProduct().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + item.getProduct().getId()));

                int previousStock = producto.getCantidad();
                int newStock = item.getPhysicalStock();

                producto.setCantidad(newStock);
                productoRepository.save(producto);

                // Asentar el movimiento de AJUSTE en el Kardex
                InventoryMovement movement = InventoryMovement.builder()
                        .product(producto)
                        .movementType(InventoryMovementType.AJUSTE)
                        .quantity(Math.abs(item.getDifference()))
                        .previousStock(previousStock)
                        .newStock(newStock)
                        .unitCost(item.getUnitCost())
                        .reason(String.format("Conciliación Auditoría %s: Dif %s%d uds",
                                session.getSessionCode(),
                                item.getDifference() > 0 ? "+" : "",
                                item.getDifference()))
                        .referenceId(session.getSessionCode())
                        .registeredBy(username != null ? username : session.getAuditorUsername())
                        .createdAt(LocalDateTime.now())
                        .build();
                inventoryMovementRepository.save(movement);
            }
        }

        session.setStatus(AuditSessionStatus.CONCILIADO);
        session.setClosedAt(LocalDateTime.now());
        recalculateSessionTotals(session);
        PhysicalAuditSession reconciled = sessionRepository.save(session);

        return PhysicalAuditSessionDTO.fromEntity(reconciled, true);
    }

    @Transactional
    public PhysicalAuditSessionDTO cancelAudit(Long sessionId) {
        PhysicalAuditSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Sesión de auditoría no encontrada con ID: " + sessionId));

        if (session.getStatus() != AuditSessionStatus.EN_CONTEO) {
            throw new IllegalStateException("Solo sesiones en conteo pueden cancelarse.");
        }

        session.setStatus(AuditSessionStatus.CANCELADO);
        session.setClosedAt(LocalDateTime.now());
        PhysicalAuditSession saved = sessionRepository.save(session);
        return PhysicalAuditSessionDTO.fromEntity(saved, false);
    }

    @Transactional(readOnly = true)
    public PhysicalAuditSessionDTO getSessionById(Long sessionId) {
        PhysicalAuditSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Sesión no encontrada con ID: " + sessionId));
        session.setItems(itemRepository.findBySessionIdOrderByCountedAtDesc(sessionId));
        return PhysicalAuditSessionDTO.fromEntity(session, true);
    }

    @Transactional(readOnly = true)
    public List<PhysicalAuditSessionDTO> listSessions() {
        return sessionRepository.findAllByOrderByStartedAtDesc()
                .stream()
                .map(s -> PhysicalAuditSessionDTO.fromEntity(s, false))
                .collect(Collectors.toList());
    }

    private void recalculateSessionTotals(PhysicalAuditSession session) {
        List<PhysicalAuditItem> items = itemRepository.findBySessionIdOrderByCountedAtDesc(session.getId());
        int totalItems = items.size();
        int totalDiff = 0;
        BigDecimal totalCostImpact = BigDecimal.ZERO;

        for (PhysicalAuditItem item : items) {
            totalDiff += item.getDifference();
            totalCostImpact = totalCostImpact.add(item.getCostDifference());
        }

        session.setTotalItemsCounted(totalItems);
        session.setTotalDifferenceUnits(totalDiff);
        session.setTotalCostImpact(totalCostImpact);
        sessionRepository.save(session);
    }
}
