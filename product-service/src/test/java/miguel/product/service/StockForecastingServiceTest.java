package miguel.product.service;

import miguel.product.dto.ForecastingSummaryDTO;
import miguel.product.dto.StockForecastDTO;
import miguel.product.dto.SupplierOrderSuggestionDTO;
import miguel.product.model.InventoryMovementType;
import miguel.product.model.Producto;
import miguel.product.model.Purchase;
import miguel.product.model.PurchaseItem;
import miguel.product.model.Supplier;
import miguel.product.repository.InventoryMovementRepository;
import miguel.product.repository.ProductoRepository;
import miguel.product.repository.PurchaseItemRepository;
import miguel.product.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockForecastingServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private PurchaseItemRepository purchaseItemRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private StockForecastingService forecastingService;

    private Producto prodCritico;
    private Producto prodAgotado;
    private Producto prodSaludable;
    private Supplier distribuidorA;

    @BeforeEach
    void setUp() {
        distribuidorA = Supplier.builder()
                .id(1L)
                .nit("900111222-1")
                .companyName("Distribuidora La 14")
                .phone("3151234567")
                .email("pedidos@la14.com")
                .active(true)
                .build();

        prodCritico = Producto.builder()
                .id(101L)
                .nombre("Arroz Diana 1kg")
                .codigoBarras("7702001001")
                .categoria("Granos")
                .precio(new BigDecimal("4200.00"))
                .costPrice(new BigDecimal("3500.00"))
                .cantidad(2)
                .minStock(5)
                .build();

        prodAgotado = Producto.builder()
                .id(102L)
                .nombre("Aceite Premier 1000ml")
                .codigoBarras("7702001002")
                .categoria("Aceites")
                .precio(new BigDecimal("9800.00"))
                .costPrice(new BigDecimal("8200.00"))
                .cantidad(0)
                .minStock(6)
                .build();

        prodSaludable = Producto.builder()
                .id(103L)
                .nombre("Sal Refinada 500g")
                .codigoBarras("7702001003")
                .categoria("Condimentos")
                .precio(new BigDecimal("1500.00"))
                .costPrice(new BigDecimal("1000.00"))
                .cantidad(50)
                .minStock(5)
                .build();
    }

    @Test
    void generateForecast_withProductsAndSales_shouldCalculateVelocityAndRiskLevels() {
        // Arrange: 14 días de análisis, 15 días de cobertura objetivo
        when(productoRepository.findAll()).thenReturn(List.of(prodCritico, prodAgotado, prodSaludable));

        // Ventas: prodCritico vendió 28 unidades en 14 días (2.0 und/día); prodSaludable vendió 14 unidades (1.0 und/día)
        List<Object[]> salesAggregation = new ArrayList<>();
        salesAggregation.add(new Object[]{101L, 28L});
        salesAggregation.add(new Object[]{103L, 14L});

        when(inventoryMovementRepository.sumQuantityByProductAndMovementTypeSince(
                eq(InventoryMovementType.VENTA), any(LocalDateTime.class)))
                .thenReturn(salesAggregation);

        // Proveedor histórico
        Purchase purchase = Purchase.builder()
                .id(1L)
                .supplier(distribuidorA)
                .build();
        PurchaseItem pi = PurchaseItem.builder()
                .id(1L)
                .product(prodCritico)
                .purchase(purchase)
                .unitCost(new BigDecimal("3500.00"))
                .build();
        when(purchaseItemRepository.findAllWithPurchaseAndSupplier()).thenReturn(List.of(pi));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(distribuidorA));

        // Act
        ForecastingSummaryDTO summary = forecastingService.generateForecast(14, 15);

        // Assert
        assertNotNull(summary);
        assertEquals(14, summary.getDaysAnalyzed());
        assertEquals(15, summary.getTargetCoverageDays());
        assertEquals(3, summary.getTotalProducts());
        assertEquals(1, summary.getOutOfStockCount()); // prodAgotado
        assertEquals(1, summary.getCriticalCount());   // prodCritico
        assertEquals(2, summary.getReorderRequiredCount()); // prodCritico + prodAgotado

        // Validar Producto Crítico (Arroz Diana)
        StockForecastDTO arroz = summary.getItems().stream()
                .filter(i -> i.getProductId().equals(101L))
                .findFirst().orElseThrow();
        assertEquals("CRITICAL", arroz.getStockStatus());
        assertEquals(2.0, arroz.getDailySaleVelocity());
        assertEquals(1.0, arroz.getEstimatedDaysRemaining());
        // Cantidad sugerida: (2.0 * 15) + 5 - 2 = 30 + 5 - 2 = 33
        assertEquals(33, arroz.getSuggestedOrderQty());
        assertEquals("Distribuidora La 14", arroz.getSuggestedSupplierName());

        // Validar Producto Agotado (Aceite Premier)
        StockForecastDTO aceite = summary.getItems().stream()
                .filter(i -> i.getProductId().equals(102L))
                .findFirst().orElseThrow();
        assertEquals("OUT_OF_STOCK", aceite.getStockStatus());
        assertEquals(0, aceite.getCurrentStock());
        assertTrue(aceite.getSuggestedOrderQty() >= 6);

        // Validar Producto Saludable (Sal Refinada)
        StockForecastDTO sal = summary.getItems().stream()
                .filter(i -> i.getProductId().equals(103L))
                .findFirst().orElseThrow();
        assertEquals("HEALTHY", sal.getStockStatus());
        assertEquals(0, sal.getSuggestedOrderQty()); // No requiere pedido
    }

    @Test
    void generateForecast_shouldGroupSuggestionsBySupplierAndGenerateWhatsAppMessage() {
        when(productoRepository.findAll()).thenReturn(List.of(prodCritico));

        List<Object[]> salesAggregation = Collections.singletonList(new Object[]{101L, 14L}); // 1.0 und/día
        when(inventoryMovementRepository.sumQuantityByProductAndMovementTypeSince(any(), any()))
                .thenReturn(salesAggregation);

        Purchase purchase = Purchase.builder().id(1L).supplier(distribuidorA).build();
        PurchaseItem pi = PurchaseItem.builder().product(prodCritico).purchase(purchase).unitCost(new BigDecimal("3500.00")).build();
        when(purchaseItemRepository.findAllWithPurchaseAndSupplier()).thenReturn(List.of(pi));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(distribuidorA));

        // Act
        ForecastingSummaryDTO summary = forecastingService.generateForecast(14, 15);

        // Assert
        assertNotNull(summary.getSupplierGroups());
        assertFalse(summary.getSupplierGroups().isEmpty());

        SupplierOrderSuggestionDTO group = summary.getSupplierGroups().get(0);
        assertEquals("Distribuidora La 14", group.getSupplierName());
        assertEquals("3151234567", group.getSupplierPhone());
        assertTrue(group.getTotalUnits() > 0);
        assertTrue(group.getTotalEstimatedCost().compareTo(BigDecimal.ZERO) > 0);

        // Mensaje de WhatsApp debe contener nombre de producto, unidades y saludo
        assertNotNull(group.getSuggestedWhatsAppMessage());
        assertTrue(group.getSuggestedWhatsAppMessage().contains("Distribuidora La 14"));
        assertTrue(group.getSuggestedWhatsAppMessage().contains("Arroz Diana 1kg"));
        assertTrue(group.getSuggestedWhatsAppMessage().contains("NexPOS Supermercado"));
    }

    @Test
    void generateForecast_withNoSales_shouldHandleGracefullyWithoutDivisionByZero() {
        when(productoRepository.findAll()).thenReturn(List.of(prodSaludable));
        when(inventoryMovementRepository.sumQuantityByProductAndMovementTypeSince(any(), any()))
                .thenReturn(Collections.emptyList());
        when(purchaseItemRepository.findAllWithPurchaseAndSupplier()).thenReturn(Collections.emptyList());

        // Act
        ForecastingSummaryDTO summary = forecastingService.generateForecast(14, 15);

        // Assert
        assertNotNull(summary);
        assertEquals(1, summary.getTotalProducts());
        assertEquals(0, summary.getReorderRequiredCount());
        StockForecastDTO item = summary.getItems().get(0);
        assertEquals("NO_MOVEMENT", item.getStockStatus());
        assertEquals(0.0, item.getDailySaleVelocity());
        assertNull(item.getEstimatedDaysRemaining());
        assertEquals(0, item.getSuggestedOrderQty());
    }
}
