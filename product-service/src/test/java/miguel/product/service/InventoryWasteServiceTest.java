package miguel.product.service;

import jakarta.persistence.EntityNotFoundException;
import miguel.product.dto.InventoryWasteDTO;
import miguel.product.dto.InventoryWasteRequest;
import miguel.product.dto.WasteSummaryDTO;
import miguel.product.model.InventoryMovement;
import miguel.product.model.InventoryWaste;
import miguel.product.model.Producto;
import miguel.product.model.WasteType;
import miguel.product.repository.InventoryMovementRepository;
import miguel.product.repository.InventoryWasteRepository;
import miguel.product.repository.ProductBatchRepository;
import miguel.product.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryWasteServiceTest {

    @Mock
    private InventoryWasteRepository inventoryWasteRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private ProductBatchRepository productBatchRepository;

    @InjectMocks
    private InventoryWasteService wasteService;

    private Producto testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Producto();
        testProduct.setId(1L);
        testProduct.setCodigoBarras("7702001001001");
        testProduct.setNombre("Leche Entera 1000ml");
        testProduct.setPrecio(new BigDecimal("3800.00"));
        testProduct.setCostPrice(new BigDecimal("3000.00"));
        testProduct.setCantidad(20);
        testProduct.setMinStock(5);
    }

    @Test
    @DisplayName("Registrar merma exitosamente descuenta stock y registra en Kardex")
    void registerWaste_success_shouldDeductStockAndSaveKardex() {
        InventoryWasteRequest request = InventoryWasteRequest.builder()
                .productId(1L)
                .quantity(3)
                .wasteType(WasteType.VENCIMIENTO)
                .reason("Vencimiento en góndola")
                .build();

        when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testProduct));
        when(inventoryWasteRepository.save(any(InventoryWaste.class))).thenAnswer(invocation -> {
            InventoryWaste w = invocation.getArgument(0);
            w.setId(10L);
            return w;
        });

        InventoryWasteDTO result = wasteService.registerWaste(request, "admin_user");

        assertNotNull(result);
        assertEquals(3, result.getQuantity());
        assertEquals(WasteType.VENCIMIENTO, result.getWasteType());
        assertEquals(new BigDecimal("9000.00"), result.getTotalCost());
        assertEquals(17, testProduct.getCantidad()); // 20 - 3

        verify(productoRepository).save(testProduct);
        verify(inventoryWasteRepository).save(any(InventoryWaste.class));
        verify(inventoryMovementRepository).save(any(InventoryMovement.class));
    }

    @Test
    @DisplayName("Registrar merma con cantidad mayor al stock disponible debe lanzar IllegalArgumentException")
    void registerWaste_insufficientStock_shouldThrowException() {
        InventoryWasteRequest request = InventoryWasteRequest.builder()
                .productId(1L)
                .quantity(50) // disponible solo 20
                .wasteType(WasteType.DANO_ROTURA)
                .reason("Caja rota")
                .build();

        when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testProduct));

        assertThrows(IllegalArgumentException.class, () -> wasteService.registerWaste(request, "cajero"));
        verify(productoRepository, never()).save(any());
        verify(inventoryWasteRepository, never()).save(any());
    }

    @Test
    @DisplayName("getWasteSummary calcula métricas agrupadas correctamente")
    void getWasteSummary_shouldAggregateMetrics() {
        InventoryWaste waste1 = InventoryWaste.builder()
                .id(1L)
                .product(testProduct)
                .quantity(2)
                .wasteType(WasteType.DANO_ROTURA)
                .unitCost(new BigDecimal("3000.00"))
                .totalCost(new BigDecimal("6000.00"))
                .build();

        InventoryWaste waste2 = InventoryWaste.builder()
                .id(2L)
                .product(testProduct)
                .quantity(5)
                .wasteType(WasteType.VENCIMIENTO)
                .unitCost(new BigDecimal("3000.00"))
                .totalCost(new BigDecimal("15000.00"))
                .build();

        when(inventoryWasteRepository.findAll()).thenReturn(List.of(waste1, waste2));

        WasteSummaryDTO summary = wasteService.getWasteSummary();

        assertNotNull(summary);
        assertEquals(2, summary.getTotalRecords());
        assertEquals(7, summary.getTotalUnitsLost());
        assertEquals(new BigDecimal("21000.00"), summary.getTotalCostLost());
        assertEquals(2, summary.getUnitsByType().get(WasteType.DANO_ROTURA));
        assertEquals(5, summary.getUnitsByType().get(WasteType.VENCIMIENTO));
    }
}
