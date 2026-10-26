package miguel.product.service;

import miguel.product.dto.ProductBatchDTO;
import miguel.product.dto.ProductBatchRequest;
import miguel.product.model.BatchStatus;
import miguel.product.model.ProductBatch;
import miguel.product.model.Producto;
import miguel.product.repository.ProductBatchRepository;
import miguel.product.repository.ProductoRepository;
import miguel.product.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductBatchServiceTest {

    @Mock
    private ProductBatchRepository productBatchRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private InventoryWasteService inventoryWasteService;

    @InjectMocks
    private ProductBatchService batchService;

    private Producto testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Producto();
        testProduct.setId(1L);
        testProduct.setCodigoBarras("7702001001001");
        testProduct.setNombre("Yogurt Fresa 1000g");
        testProduct.setPrecio(new BigDecimal("6000.00"));
        testProduct.setCostPrice(new BigDecimal("4500.00"));
        testProduct.setCantidad(50);
    }

    @Test
    @DisplayName("Crear lote con fecha futura asigna estado ACTIVO")
    void createBatch_futureDate_shouldBeActive() {
        ProductBatchRequest request = ProductBatchRequest.builder()
                .productId(1L)
                .batchNumber("LOTE-YOG-01")
                .expirationDate(LocalDate.now().plusMonths(2))
                .initialQuantity(30)
                .build();

        when(productoRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productBatchRepository.save(any(ProductBatch.class))).thenAnswer(i -> {
            ProductBatch b = i.getArgument(0);
            b.setId(100L);
            return b;
        });

        ProductBatchDTO result = batchService.createBatch(request);

        assertNotNull(result);
        assertEquals("LOTE-YOG-01", result.getBatchNumber());
        assertEquals(BatchStatus.ACTIVO, result.getStatus());
        assertEquals(30, result.getCurrentQuantity());
        verify(productBatchRepository).save(any(ProductBatch.class));
    }

    @Test
    @DisplayName("Consultar lotes que vencen próximamente delega en findExpiringBatches")
    void getExpiringBatches_shouldCallRepository() {
        ProductBatch batch = ProductBatch.builder()
                .id(1L)
                .product(testProduct)
                .batchNumber("LOTE-001")
                .expirationDate(LocalDate.now().plusDays(5))
                .initialQuantity(10)
                .currentQuantity(8)
                .status(BatchStatus.POR_VENCER)
                .build();

        when(productBatchRepository.findExpiringBatches(any(LocalDate.class)))
                .thenReturn(Collections.singletonList(batch));

        List<ProductBatchDTO> list = batchService.getExpiringBatches(7);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("CRITICO", list.get(0).getUrgencyLevel());
        verify(productBatchRepository).findExpiringBatches(any(LocalDate.class));
    }
}
