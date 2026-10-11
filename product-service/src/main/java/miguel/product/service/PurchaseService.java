package miguel.product.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import miguel.product.dto.*;
import miguel.product.model.*;
import miguel.product.repository.InventoryMovementRepository;
import miguel.product.repository.ProductoRepository;
import miguel.product.repository.PurchaseItemRepository;
import miguel.product.repository.PurchaseRepository;
import miguel.product.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductoRepository productoRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Transactional(readOnly = true)
    public List<PurchaseResponse> getPurchases(
            Long supplierId,
            PurchaseStatus status,
            PurchasePaymentStatus paymentStatus,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        List<Purchase> purchases;
        if (supplierId != null || status != null || paymentStatus != null || startDate != null || endDate != null) {
            LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(6);
            LocalDateTime end = endDate != null ? endDate : LocalDateTime.now().plusDays(1);
            purchases = purchaseRepository.findWithFilters(supplierId, status, paymentStatus, start, end);
        } else {
            purchases = purchaseRepository.findAllByOrderByReceivedAtDesc();
        }

        return purchases.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseResponse getPurchaseById(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Factura de compra no encontrada con ID: " + id));
        return toResponse(purchase);
    }

    @Transactional
    public PurchaseResponse createPurchase(PurchaseRequest request, String username) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("La compra debe incluir al menos un producto");
        }

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + request.getSupplierId()));

        if (Boolean.FALSE.equals(supplier.getActive())) {
            throw new IllegalStateException("El proveedor '" + supplier.getCompanyName() + "' se encuentra inactivo");
        }

        String cashier = (username != null && !username.isBlank()) ? username : "admin";
        String purchaseNumber = generateUniquePurchaseNumber();

        BigDecimal subtotalAccum = BigDecimal.ZERO;
        BigDecimal taxAccum = BigDecimal.ZERO;
        BigDecimal totalAccum = BigDecimal.ZERO;

        boolean isCredit = "CREDITO_PROVEEDOR".equalsIgnoreCase(request.getPaymentMethod());
        PurchasePaymentStatus paymentStatus = isCredit ? PurchasePaymentStatus.PENDING : PurchasePaymentStatus.PAID;

        LocalDate paymentDueDate = request.getPaymentDueDate();
        if (isCredit && paymentDueDate == null) {
            int termsDays = supplier.getPaymentTermsDays() != null && supplier.getPaymentTermsDays() > 0 
                    ? supplier.getPaymentTermsDays() : 30;
            paymentDueDate = LocalDate.now().plusDays(termsDays);
        }

        Purchase purchase = Purchase.builder()
                .purchaseNumber(purchaseNumber)
                .supplier(supplier)
                .supplierInvoiceNumber(request.getSupplierInvoiceNumber() != null ? request.getSupplierInvoiceNumber().trim() : null)
                .status(PurchaseStatus.RECEIVED)
                .paymentStatus(paymentStatus)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "EFECTIVO")
                .paymentDueDate(paymentDueDate)
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .registeredBy(cashier)
                .receivedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        List<PurchaseItem> items = new ArrayList<>();

        for (PurchaseItemRequest itemReq : request.getItems()) {
            // Bloqueo pesimista para concurrencia segura
            Producto producto = productoRepository.findByIdForUpdate(itemReq.getProductId())
                    .or(() -> productoRepository.findById(itemReq.getProductId()))
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + itemReq.getProductId()));

            int currentStock = producto.getCantidad();
            BigDecimal currentCost = producto.getCostPrice() != null ? producto.getCostPrice() : BigDecimal.ZERO;
            int receivedQuantity = itemReq.getQuantity();
            BigDecimal unitCost = itemReq.getUnitCost();

            // Cálculo matemático del Costo Promedio Ponderado (PMP)
            BigDecimal newCostPmp;
            if (currentStock <= 0) {
                newCostPmp = unitCost;
            } else {
                BigDecimal currentValuation = currentCost.multiply(BigDecimal.valueOf(currentStock));
                BigDecimal incomingValuation = unitCost.multiply(BigDecimal.valueOf(receivedQuantity));
                BigDecimal totalValuation = currentValuation.add(incomingValuation);
                BigDecimal totalStockQty = BigDecimal.valueOf(currentStock + receivedQuantity);
                newCostPmp = totalValuation.divide(totalStockQty, 2, RoundingMode.HALF_UP);
            }

            // Actualización atómica de stock y costo del producto
            int newStock = currentStock + receivedQuantity;
            producto.setCantidad(newStock);
            producto.setCostPrice(newCostPmp);
            productoRepository.save(producto);

            // Registro en Kardex de inventario (ENTRADA)
            String docRef = request.getSupplierInvoiceNumber() != null && !request.getSupplierInvoiceNumber().isBlank()
                    ? " (Fact: " + request.getSupplierInvoiceNumber().trim() + ")"
                    : "";
            InventoryMovement movement = InventoryMovement.builder()
                    .product(producto)
                    .movementType(InventoryMovementType.ENTRADA)
                    .quantity(receivedQuantity)
                    .previousStock(currentStock)
                    .newStock(newStock)
                    .unitCost(unitCost)
                    .reason("Recepción Compra #" + purchaseNumber + " - Prov: " + supplier.getCompanyName() + docRef)
                    .referenceId(purchaseNumber)
                    .registeredBy(cashier)
                    .build();
            inventoryMovementRepository.save(movement);

            // Liquidación de valores del ítem
            BigDecimal taxRate = itemReq.getTaxRate() != null ? itemReq.getTaxRate() : new BigDecimal("0.19");
            BigDecimal itemSubtotal = unitCost.multiply(BigDecimal.valueOf(receivedQuantity)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemTax = itemSubtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemTotal = itemSubtotal.add(itemTax).setScale(2, RoundingMode.HALF_UP);

            subtotalAccum = subtotalAccum.add(itemSubtotal);
            taxAccum = taxAccum.add(itemTax);
            totalAccum = totalAccum.add(itemTotal);

            PurchaseItem purchaseItem = PurchaseItem.builder()
                    .product(producto)
                    .quantity(receivedQuantity)
                    .unitCost(unitCost)
                    .taxRate(taxRate)
                    .taxAmount(itemTax)
                    .subtotal(itemSubtotal)
                    .total(itemTotal)
                    .build();

            purchase.addItem(purchaseItem);
            items.add(purchaseItem);
        }

        purchase.setSubtotal(subtotalAccum);
        purchase.setTaxAmount(taxAccum);
        purchase.setTotalAmount(totalAccum);

        Purchase saved = purchaseRepository.save(purchase);
        return toResponse(saved);
    }

    @Transactional
    public PurchaseResponse cancelPurchase(Long id, String reason, String username) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Compra no encontrada con ID: " + id));

        if (purchase.getStatus() == PurchaseStatus.CANCELLED) {
            throw new IllegalStateException("La compra #" + purchase.getPurchaseNumber() + " ya fue anulada previamente");
        }

        String cashier = (username != null && !username.isBlank()) ? username : "admin";
        String annulReason = (reason != null && !reason.isBlank()) ? reason.trim() : "Anulación de factura de compra";

        // Reversión de inventario para cada ítem
        for (PurchaseItem item : purchase.getItems()) {
            Producto producto = productoRepository.findByIdForUpdate(item.getProduct().getId())
                    .or(() -> productoRepository.findById(item.getProduct().getId()))
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + item.getProduct().getId()));

            int currentStock = producto.getCantidad();
            int qtyToRevert = item.getQuantity();

            if (currentStock < qtyToRevert) {
                throw new IllegalStateException("No es posible anular la compra #" + purchase.getPurchaseNumber() +
                        ": El producto '" + producto.getNombre() + "' tiene stock actual de " + currentStock +
                        " unidades, menor a las " + qtyToRevert + " unidades recibidas (parte del stock ya fue vendido)");
            }

            int newStock = currentStock - qtyToRevert;
            producto.setCantidad(newStock);
            productoRepository.save(producto);

            // Registro en Kardex de reversión (DEVOLUCION)
            InventoryMovement movement = InventoryMovement.builder()
                    .product(producto)
                    .movementType(InventoryMovementType.DEVOLUCION)
                    .quantity(qtyToRevert)
                    .previousStock(currentStock)
                    .newStock(newStock)
                    .unitCost(item.getUnitCost())
                    .reason("Devolución / Anulación Compra #" + purchase.getPurchaseNumber() + " (" + annulReason + ")")
                    .referenceId(purchase.getPurchaseNumber())
                    .registeredBy(cashier)
                    .build();
            inventoryMovementRepository.save(movement);
        }

        purchase.setStatus(PurchaseStatus.CANCELLED);
        purchase.setCancelledAt(LocalDateTime.now());
        purchase.setCancelledBy(cashier);
        purchase.setCancellationReason(annulReason);

        Purchase updated = purchaseRepository.save(purchase);
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public PayablesSummaryDTO getPayablesSummary() {
        BigDecimal totalPending = purchaseRepository.sumTotalPendingPayables();
        List<Purchase> allPurchases = purchaseRepository.findAll();

        long pendingCount = 0;
        long overdueCount = 0;
        long upcomingCount = 0;
        LocalDate today = LocalDate.now();

        for (Purchase p : allPurchases) {
            if (p.getStatus() == PurchaseStatus.RECEIVED && p.getPaymentStatus() == PurchasePaymentStatus.PENDING) {
                pendingCount++;
                if (p.getPaymentDueDate() != null && p.getPaymentDueDate().isBefore(today)) {
                    overdueCount++;
                } else {
                    upcomingCount++;
                }
            }
        }

        return PayablesSummaryDTO.builder()
                .totalPendingAmount(totalPending != null ? totalPending : BigDecimal.ZERO)
                .pendingInvoicesCount(pendingCount)
                .overdueInvoicesCount(overdueCount)
                .upcomingInvoicesCount(upcomingCount)
                .build();
    }

    private String generateUniquePurchaseNumber() {
        long count = purchaseRepository.count() + 1;
        String number = String.format("COMP-%04d", count);
        while (purchaseRepository.findByPurchaseNumber(number).isPresent()) {
            count++;
            number = String.format("COMP-%04d", count);
        }
        return number;
    }

    public PurchaseResponse toResponse(Purchase p) {
        List<PurchaseItemResponse> itemResponses = p.getItems().stream()
                .map(item -> PurchaseItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProduct() != null ? item.getProduct().getNombre() : "N/A")
                        .productBarcode(item.getProduct() != null ? item.getProduct().getCodigoBarras() : null)
                        .quantity(item.getQuantity())
                        .unitCost(item.getUnitCost())
                        .taxRate(item.getTaxRate())
                        .taxAmount(item.getTaxAmount())
                        .subtotal(item.getSubtotal())
                        .total(item.getTotal())
                        .newCostPmp(item.getProduct() != null ? item.getProduct().getCostPrice() : null)
                        .newStock(item.getProduct() != null ? item.getProduct().getCantidad() : null)
                        .build())
                .collect(Collectors.toList());

        return PurchaseResponse.builder()
                .id(p.getId())
                .purchaseNumber(p.getPurchaseNumber())
                .supplierId(p.getSupplier() != null ? p.getSupplier().getId() : null)
                .supplierName(p.getSupplier() != null ? p.getSupplier().getCompanyName() : "N/A")
                .supplierNit(p.getSupplier() != null ? p.getSupplier().getNit() : "N/A")
                .supplierInvoiceNumber(p.getSupplierInvoiceNumber())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .paymentStatus(p.getPaymentStatus() != null ? p.getPaymentStatus().name() : null)
                .paymentMethod(p.getPaymentMethod())
                .paymentDueDate(p.getPaymentDueDate())
                .subtotal(p.getSubtotal())
                .taxAmount(p.getTaxAmount())
                .totalAmount(p.getTotalAmount())
                .notes(p.getNotes())
                .registeredBy(p.getRegisteredBy())
                .receivedAt(p.getReceivedAt())
                .createdAt(p.getCreatedAt())
                .cancelledAt(p.getCancelledAt())
                .cancelledBy(p.getCancelledBy())
                .cancellationReason(p.getCancellationReason())
                .items(itemResponses)
                .build();
    }
}
