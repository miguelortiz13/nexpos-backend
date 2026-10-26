package miguel.product.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import miguel.product.dto.InventoryWasteDTO;
import miguel.product.dto.InventoryWasteRequest;
import miguel.product.dto.WasteSummaryDTO;
import miguel.product.model.*;
import miguel.product.repository.InventoryMovementRepository;
import miguel.product.repository.InventoryWasteRepository;
import miguel.product.repository.ProductBatchRepository;
import miguel.product.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryWasteService {

    private final InventoryWasteRepository inventoryWasteRepository;
    private final ProductoRepository productoRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final ProductBatchRepository productBatchRepository;

    @Transactional
    public InventoryWasteDTO registerWaste(InventoryWasteRequest request, String username) {
        // Bloqueo pesimista para evitar colisiones concurrentes de inventario
        Producto producto = productoRepository.findByIdForUpdate(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + request.getProductId()));

        int previousStock = producto.getCantidad();
        if (previousStock < request.getQuantity()) {
            throw new IllegalArgumentException(String.format(
                    "Stock insuficiente para registrar merma de '%s'. Disponible: %d, Solicitado: %d",
                    producto.getNombre(), previousStock, request.getQuantity()));
        }

        int newStock = previousStock - request.getQuantity();
        producto.setCantidad(newStock);
        productoRepository.save(producto);

        // Si se especificó un lote, descontar las unidades del lote correspondiente
        if (request.getBatchId() != null) {
            ProductBatch batch = productBatchRepository.findById(request.getBatchId()).orElse(null);
            if (batch != null && batch.getProduct().getId().equals(producto.getId())) {
                int batchQty = Math.max(0, batch.getCurrentQuantity() - request.getQuantity());
                batch.setCurrentQuantity(batchQty);
                if (batchQty == 0) {
                    batch.setStatus(BatchStatus.AGOTADO);
                }
                productBatchRepository.save(batch);
            }
        }

        BigDecimal unitCost = producto.getCostPrice() != null && producto.getCostPrice().compareTo(BigDecimal.ZERO) > 0
                ? producto.getCostPrice()
                : producto.getPrecio();
        BigDecimal totalCost = unitCost.multiply(BigDecimal.valueOf(request.getQuantity()));

        InventoryWaste waste = InventoryWaste.builder()
                .product(producto)
                .quantity(request.getQuantity())
                .wasteType(request.getWasteType())
                .unitCost(unitCost)
                .totalCost(totalCost)
                .reason(request.getReason())
                .registeredBy(username != null && !username.isBlank() ? username : "SISTEMA")
                .createdAt(LocalDateTime.now())
                .build();

        InventoryWaste savedWaste = inventoryWasteRepository.save(waste);

        // Registrar en el Kardex oficial de inventario como SALIDA
        InventoryMovement movement = InventoryMovement.builder()
                .product(producto)
                .movementType(InventoryMovementType.SALIDA)
                .quantity(request.getQuantity())
                .previousStock(previousStock)
                .newStock(newStock)
                .unitCost(unitCost)
                .reason(String.format("Baja por Merma [%s]: %s", request.getWasteType(), request.getReason()))
                .referenceId("MERMA-" + savedWaste.getId())
                .registeredBy(waste.getRegisteredBy())
                .createdAt(LocalDateTime.now())
                .build();
        inventoryMovementRepository.save(movement);

        return InventoryWasteDTO.fromEntity(savedWaste);
    }

    @Transactional(readOnly = true)
    public List<InventoryWasteDTO> getAllWastes(WasteType type, Long productId) {
        List<InventoryWaste> wastes;
        if (productId != null) {
            wastes = inventoryWasteRepository.findByProductIdOrderByCreatedAtDesc(productId);
        } else if (type != null) {
            wastes = inventoryWasteRepository.findByWasteTypeOrderByCreatedAtDesc(type);
        } else {
            wastes = inventoryWasteRepository.findAllByOrderByCreatedAtDesc();
        }

        return wastes.stream()
                .map(InventoryWasteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WasteSummaryDTO getWasteSummary() {
        List<InventoryWaste> all = inventoryWasteRepository.findAll();

        int totalUnits = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        Map<WasteType, Integer> unitsByType = new EnumMap<>(WasteType.class);
        Map<WasteType, BigDecimal> costByType = new EnumMap<>(WasteType.class);

        for (WasteType type : WasteType.values()) {
            unitsByType.put(type, 0);
            costByType.put(type, BigDecimal.ZERO);
        }

        for (InventoryWaste w : all) {
            totalUnits += w.getQuantity();
            totalCost = totalCost.add(w.getTotalCost());

            unitsByType.put(w.getWasteType(), unitsByType.get(w.getWasteType()) + w.getQuantity());
            costByType.put(w.getWasteType(), costByType.get(w.getWasteType()).add(w.getTotalCost()));
        }

        return WasteSummaryDTO.builder()
                .totalRecords(all.size())
                .totalUnitsLost(totalUnits)
                .totalCostLost(totalCost)
                .unitsByType(unitsByType)
                .costByType(costByType)
                .build();
    }
}
