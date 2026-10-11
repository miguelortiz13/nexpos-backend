package miguel.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import miguel.product.dto.ForecastingSummaryDTO;
import miguel.product.dto.StockForecastDTO;
import miguel.product.dto.SupplierOrderSuggestionDTO;
import miguel.product.model.InventoryMovementType;
import miguel.product.model.Producto;
import miguel.product.model.PurchaseItem;
import miguel.product.model.Supplier;
import miguel.product.repository.InventoryMovementRepository;
import miguel.product.repository.ProductoRepository;
import miguel.product.repository.PurchaseItemRepository;
import miguel.product.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockForecastingService {

    private final ProductoRepository productoRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SupplierRepository supplierRepository;

    private static final NumberFormat COP_FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    static {
        COP_FORMAT.setMaximumFractionDigits(0);
    }

    @Transactional(readOnly = true)
    public ForecastingSummaryDTO generateForecast(int daysAnalyzed, int targetCoverageDays) {
        int days = daysAnalyzed > 0 ? daysAnalyzed : 14;
        int targetDays = targetCoverageDays > 0 ? targetCoverageDays : 15;

        List<Producto> products = productoRepository.findAll();
        LocalDateTime sinceDate = LocalDateTime.now().minusDays(days);

        // 1. Obtener agregación de ventas por producto en el período analizado
        List<Object[]> salesAggregation = inventoryMovementRepository
                .sumQuantityByProductAndMovementTypeSince(InventoryMovementType.VENTA, sinceDate);

        Map<Long, Integer> salesMap = new HashMap<>();
        for (Object[] row : salesAggregation) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                Long pId = ((Number) row[0]).longValue();
                Integer totalSold = ((Number) row[1]).intValue();
                salesMap.put(pId, totalSold);
            }
        }

        // 2. Mapear compras históricas para identificar al proveedor más reciente y su costo unitario
        List<PurchaseItem> purchaseItems = purchaseItemRepository.findAllWithPurchaseAndSupplier();
        Map<Long, Supplier> latestSupplierMap = new HashMap<>();
        Map<Long, BigDecimal> latestCostMap = new HashMap<>();

        for (PurchaseItem item : purchaseItems) {
            if (item.getProduct() != null && item.getPurchase() != null && item.getPurchase().getSupplier() != null) {
                Long pId = item.getProduct().getId();
                latestSupplierMap.putIfAbsent(pId, item.getPurchase().getSupplier());
                if (item.getUnitCost() != null) {
                    latestCostMap.putIfAbsent(pId, item.getUnitCost());
                }
            }
        }

        // 3. Procesar pronóstico por producto
        List<StockForecastDTO> forecastItems = new ArrayList<>();
        int outOfStock = 0;
        int critical = 0;
        int lowStock = 0;
        int reorderRequired = 0;
        BigDecimal totalInvestment = BigDecimal.ZERO;

        for (Producto p : products) {
            int currentStock = p.getCantidad();
            int minStock = p.getMinStock() > 0 ? p.getMinStock() : 5;
            int totalSold = salesMap.getOrDefault(p.getId(), 0);

            double rawVelocity = (double) totalSold / days;
            double dailyVelocity = Math.round(rawVelocity * 100.0) / 100.0;

            Double daysRemaining = null;
            if (dailyVelocity > 0) {
                double rawDays = (double) Math.max(0, currentStock) / dailyVelocity;
                daysRemaining = Math.round(rawDays * 10.0) / 10.0;
            }

            // Costo de referencia: el asignado al producto o el de la última compra
            BigDecimal unitCost = (p.getCostPrice() != null && p.getCostPrice().compareTo(BigDecimal.ZERO) > 0)
                    ? p.getCostPrice()
                    : latestCostMap.getOrDefault(p.getId(), BigDecimal.ZERO);

            // Clasificación de estado de riesgo
            String status;
            if (currentStock <= 0) {
                status = "OUT_OF_STOCK";
                outOfStock++;
            } else if ((daysRemaining != null && daysRemaining <= 2.0) || currentStock <= Math.max(1, minStock / 2)) {
                status = "CRITICAL";
                critical++;
            } else if ((daysRemaining != null && daysRemaining <= 5.0) || currentStock <= minStock) {
                status = "LOW_STOCK";
                lowStock++;
            } else if (daysRemaining != null && daysRemaining <= 14.0) {
                status = "MODERATE";
            } else if (dailyVelocity > 0) {
                status = "HEALTHY";
            } else {
                status = "NO_MOVEMENT";
            }

            // Cálculo de cantidad recomendada a comprar
            int suggestedOrderQty = 0;
            if (dailyVelocity > 0) {
                int targetStock = (int) Math.ceil((dailyVelocity * targetDays) + minStock);
                int needed = targetStock - currentStock;
                suggestedOrderQty = Math.max(0, needed);
            } else {
                // Producto sin ventas registradas en la ventana: abastecer hasta el stock de seguridad si está bajo
                if (currentStock <= 0) {
                    suggestedOrderQty = Math.max(minStock, 10);
                } else if (currentStock < minStock) {
                    suggestedOrderQty = minStock - currentStock;
                }
            }

            BigDecimal estimatedOrderCost = unitCost.multiply(BigDecimal.valueOf(suggestedOrderQty))
                    .setScale(2, RoundingMode.HALF_UP);

            if (suggestedOrderQty > 0) {
                reorderRequired++;
                totalInvestment = totalInvestment.add(estimatedOrderCost);
            }

            Supplier supp = latestSupplierMap.get(p.getId());

            StockForecastDTO itemDTO = StockForecastDTO.builder()
                    .productId(p.getId())
                    .productName(p.getNombre())
                    .barcode(p.getCodigoBarras())
                    .category(p.getCategoria())
                    .currentStock(currentStock)
                    .minStock(minStock)
                    .unitCost(unitCost)
                    .salePrice(p.getPrecio())
                    .totalSoldInPeriod(totalSold)
                    .dailySaleVelocity(dailyVelocity)
                    .estimatedDaysRemaining(daysRemaining)
                    .stockStatus(status)
                    .suggestedOrderQty(suggestedOrderQty)
                    .estimatedOrderCost(estimatedOrderCost)
                    .suggestedSupplierId(supp != null ? supp.getId() : null)
                    .suggestedSupplierName(supp != null ? supp.getCompanyName() : "Sin Proveedor Asignado")
                    .suggestedSupplierPhone(supp != null ? supp.getPhone() : null)
                    .suggestedSupplierEmail(supp != null ? supp.getEmail() : null)
                    .build();

            forecastItems.add(itemDTO);
        }

        // 4. Ordenar: primero los más urgentes (Agotados -> Críticos -> Bajo Stock -> Moderados -> Saludables)
        forecastItems.sort((a, b) -> {
            int scoreA = getStatusPriority(a.getStockStatus());
            int scoreB = getStatusPriority(b.getStockStatus());
            if (scoreA != scoreB) {
                return Integer.compare(scoreA, scoreB);
            }
            if (a.getEstimatedDaysRemaining() != null && b.getEstimatedDaysRemaining() != null) {
                return Double.compare(a.getEstimatedDaysRemaining(), b.getEstimatedDaysRemaining());
            }
            return Integer.compare(b.getSuggestedOrderQty(), a.getSuggestedOrderQty());
        });

        // 5. Agrupar sugerencias por proveedor (solo productos que requieren reorden)
        List<SupplierOrderSuggestionDTO> supplierGroups = buildSupplierGroups(forecastItems);

        return ForecastingSummaryDTO.builder()
                .daysAnalyzed(days)
                .targetCoverageDays(targetDays)
                .totalProducts(products.size())
                .outOfStockCount(outOfStock)
                .criticalCount(critical)
                .lowStockCount(lowStock)
                .reorderRequiredCount(reorderRequired)
                .totalInvestmentRequired(totalInvestment)
                .items(forecastItems)
                .supplierGroups(supplierGroups)
                .build();
    }

    private int getStatusPriority(String status) {
        return switch (status) {
            case "OUT_OF_STOCK" -> 1;
            case "CRITICAL" -> 2;
            case "LOW_STOCK" -> 3;
            case "MODERATE" -> 4;
            case "HEALTHY" -> 5;
            case "NO_MOVEMENT" -> 6;
            default -> 7;
        };
    }

    private List<SupplierOrderSuggestionDTO> buildSupplierGroups(List<StockForecastDTO> items) {
        Map<Long, List<StockForecastDTO>> grouped = items.stream()
                .filter(i -> i.getSuggestedOrderQty() > 0)
                .collect(Collectors.groupingBy(i -> i.getSuggestedSupplierId() != null ? i.getSuggestedSupplierId() : 0L));

        List<SupplierOrderSuggestionDTO> result = new ArrayList<>();

        for (Map.Entry<Long, List<StockForecastDTO>> entry : grouped.entrySet()) {
            Long suppId = entry.getKey();
            List<StockForecastDTO> groupItems = entry.getValue();

            String suppName = "Sin Proveedor Asignado";
            String suppNit = "N/A";
            String suppPhone = null;
            String suppEmail = null;

            if (suppId > 0) {
                Supplier s = supplierRepository.findById(suppId).orElse(null);
                if (s != null) {
                    suppName = s.getCompanyName();
                    suppNit = s.getNit();
                    suppPhone = s.getPhone();
                    suppEmail = s.getEmail();
                }
            }

            int totalUnits = groupItems.stream().mapToInt(StockForecastDTO::getSuggestedOrderQty).sum();
            BigDecimal totalCost = groupItems.stream()
                    .map(StockForecastDTO::getEstimatedOrderCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Generar plantilla de WhatsApp profesional
            String waMessage = buildWhatsAppMessage(suppName, groupItems, totalCost);

            SupplierOrderSuggestionDTO dto = SupplierOrderSuggestionDTO.builder()
                    .supplierId(suppId > 0 ? suppId : null)
                    .supplierName(suppName)
                    .supplierNit(suppNit)
                    .supplierPhone(suppPhone)
                    .supplierEmail(suppEmail)
                    .itemCount(groupItems.size())
                    .totalUnits(totalUnits)
                    .totalEstimatedCost(totalCost)
                    .items(groupItems)
                    .suggestedWhatsAppMessage(waMessage)
                    .build();

            result.add(dto);
        }

        // Ordenar grupos de proveedores por mayor costo estimado de compra
        result.sort((a, b) -> b.getTotalEstimatedCost().compareTo(a.getTotalEstimatedCost()));
        return result;
    }

    private String buildWhatsAppMessage(String supplierName, List<StockForecastDTO> items, BigDecimal totalCost) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hola *").append(supplierName).append("*, te escribo desde *NexPOS Supermercado* ");
        sb.append("para cotizar y solicitar el siguiente pedido de reabastecimiento:\n\n");
        sb.append("📦 *Detalle del Pedido Sugerido:*\n");

        for (StockForecastDTO item : items) {
            sb.append("• ").append(item.getProductName())
                    .append(" x ").append(item.getSuggestedOrderQty()).append(" und");
            if (item.getBarcode() != null && !item.getBarcode().isBlank()) {
                sb.append(" (Cód: ").append(item.getBarcode()).append(")");
            }
            sb.append("\n");
        }

        sb.append("\n💰 *Inversión Total Estimada:* ")
                .append(COP_FORMAT.format(totalCost))
                .append(" COP\n\n");
        sb.append("¿Me confirmas disponibilidad de stock y fecha estimada de entrega? ¡Muchas gracias!");

        return sb.toString();
    }
}
