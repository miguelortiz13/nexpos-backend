package miguel.product.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import miguel.product.dto.InventoryWasteDTO;
import miguel.product.dto.InventoryWasteRequest;
import miguel.product.dto.ProductBatchDTO;
import miguel.product.dto.ProductBatchRequest;
import miguel.product.model.*;
import miguel.product.repository.ProductBatchRepository;
import miguel.product.repository.ProductoRepository;
import miguel.product.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductBatchService {

    private final ProductBatchRepository productBatchRepository;
    private final ProductoRepository productoRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryWasteService inventoryWasteService;

    @Transactional
    public ProductBatchDTO createBatch(ProductBatchRequest request) {
        Producto producto = productoRepository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + request.getProductId()));

        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplierId()).orElse(null);
        }

        LocalDate expiration = request.getExpirationDate();
        BatchStatus initialStatus = BatchStatus.ACTIVO;
        if (expiration.isBefore(LocalDate.now())) {
            initialStatus = BatchStatus.VENCIDO;
        } else if (expiration.isBefore(LocalDate.now().plusDays(7))) {
            initialStatus = BatchStatus.POR_VENCER;
        }

        ProductBatch batch = ProductBatch.builder()
                .product(producto)
                .batchNumber(request.getBatchNumber().trim())
                .expirationDate(expiration)
                .initialQuantity(request.getInitialQuantity())
                .currentQuantity(request.getInitialQuantity())
                .status(initialStatus)
                .supplier(supplier)
                .receivedAt(request.getReceivedAt() != null ? request.getReceivedAt() : LocalDate.now())
                .createdAt(LocalDateTime.now())
                .build();

        ProductBatch saved = productBatchRepository.save(batch);
        return ProductBatchDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> getBatchesByProduct(Long productId) {
        return productBatchRepository.findByProductIdOrderByExpirationDateAsc(productId)
                .stream()
                .map(ProductBatchDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> getExpiringBatches(Integer daysAhead) {
        LocalDate threshold = LocalDate.now().plusDays(daysAhead != null ? daysAhead : 15);
        return productBatchRepository.findExpiringBatches(threshold)
                .stream()
                .map(ProductBatchDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> getAllActiveBatches() {
        return productBatchRepository.findAll()
                .stream()
                .map(ProductBatchDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public InventoryWasteDTO dischargeBatchAsWaste(Long batchId, String reason, String username) {
        ProductBatch batch = productBatchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Lote no encontrado con ID: " + batchId));

        if (batch.getCurrentQuantity() <= 0) {
            throw new IllegalArgumentException("El lote ya no cuenta con existencias disponibles.");
        }

        InventoryWasteRequest wasteRequest = InventoryWasteRequest.builder()
                .productId(batch.getProduct().getId())
                .quantity(batch.getCurrentQuantity())
                .wasteType(WasteType.VENCIMIENTO)
                .reason(reason != null && !reason.isBlank() ? reason : "Baja automática por caducidad de lote " + batch.getBatchNumber())
                .batchId(batch.getId())
                .build();

        return inventoryWasteService.registerWaste(wasteRequest, username);
    }
}
