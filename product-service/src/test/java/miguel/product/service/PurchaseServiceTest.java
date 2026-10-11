package miguel.product.service;

import miguel.product.dto.PurchaseItemRequest;
import miguel.product.dto.PurchaseRequest;
import miguel.product.dto.PurchaseResponse;
import miguel.product.model.*;
import miguel.product.repository.InventoryMovementRepository;
import miguel.product.repository.ProductoRepository;
import miguel.product.repository.PurchaseItemRepository;
import miguel.product.repository.PurchaseRepository;
import miguel.product.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private PurchaseItemRepository purchaseItemRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @InjectMocks
    private PurchaseService purchaseService;

    private Supplier testSupplier;
    private Producto testProduct;

    @BeforeEach
    void setUp() {
        testSupplier = Supplier.builder()
                .id(1L)
                .nit("860005224-6")
                .companyName("Bavaria & Cía S.C.A.")
                .paymentTermsDays(30)
                .active(true)
                .build();

        testProduct = Producto.builder()
                .id(10L)
                .nombre("Cerveza Poker 330ml")
                .precio(new BigDecimal("3500.00"))
                .cantidad(10) // 10 unidades iniciales
                .costPrice(new BigDecimal("1000.00")) // Costo inicial: $1,000
                .build();
    }

    @Test
    void createPurchase_shouldCalculatePmpAndIncreaseStockAndRecordKardex() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(productoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testProduct));
        when(purchaseRepository.count()).thenReturn(0L);
        when(purchaseRepository.findByPurchaseNumber(any())).thenReturn(Optional.empty());
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(i -> {
            Purchase p = i.getArgument(0);
            p.setId(100L);
            return p;
        });

        // Compramos 10 unidades a $2,000 c/u.
        // PMP esperado = ((10 * 1000) + (10 * 2000)) / (10 + 10) = 30000 / 20 = 1500.00
        PurchaseItemRequest itemReq = PurchaseItemRequest.builder()
                .productId(10L)
                .quantity(10)
                .unitCost(new BigDecimal("2000.00"))
                .taxRate(new BigDecimal("0.19"))
                .build();

        PurchaseRequest request = PurchaseRequest.builder()
                .supplierId(1L)
                .supplierInvoiceNumber("FAC-B-992")
                .paymentMethod("EFECTIVO")
                .items(Collections.singletonList(itemReq))
                .build();

        PurchaseResponse response = purchaseService.createPurchase(request, "admin");

        assertNotNull(response);
        assertEquals("COMP-0001", response.getPurchaseNumber());
        assertEquals("RECEIVED", response.getStatus());
        assertEquals("PAID", response.getPaymentStatus());

        // Verificamos stock y PMP en el producto
        assertEquals(20, testProduct.getCantidad()); // 10 + 10 = 20
        assertEquals(0, new BigDecimal("1500.00").compareTo(testProduct.getCostPrice()));

        // Verificamos que se guardó en Kardex con tipo ENTRADA
        verify(inventoryMovementRepository, times(1)).save(argThat(m ->
                m.getMovementType() == InventoryMovementType.ENTRADA &&
                m.getQuantity() == 10 &&
                m.getPreviousStock() == 10 &&
                m.getNewStock() == 20 &&
                m.getUnitCost().compareTo(new BigDecimal("2000.00")) == 0
        ));
    }

    @Test
    void createPurchase_whenInitialStockIsZero_shouldSetPmpToIncomingUnitCost() {
        testProduct.setCantidad(0);
        testProduct.setCostPrice(BigDecimal.ZERO);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(productoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testProduct));
        when(purchaseRepository.count()).thenReturn(0L);
        when(purchaseRepository.findByPurchaseNumber(any())).thenReturn(Optional.empty());
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(i -> i.getArgument(0));

        PurchaseItemRequest itemReq = PurchaseItemRequest.builder()
                .productId(10L)
                .quantity(5)
                .unitCost(new BigDecimal("2500.00"))
                .taxRate(new BigDecimal("0.19"))
                .build();

        PurchaseRequest request = PurchaseRequest.builder()
                .supplierId(1L)
                .paymentMethod("EFECTIVO")
                .items(Collections.singletonList(itemReq))
                .build();

        PurchaseResponse response = purchaseService.createPurchase(request, "admin");

        assertNotNull(response);
        assertEquals(5, testProduct.getCantidad());
        assertEquals(0, new BigDecimal("2500.00").compareTo(testProduct.getCostPrice()));
    }

    @Test
    void createPurchase_withCreditMethod_shouldSetPaymentStatusPendingAndCalculateDueDate() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(productoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testProduct));
        when(purchaseRepository.count()).thenReturn(1L);
        when(purchaseRepository.findByPurchaseNumber(any())).thenReturn(Optional.empty());
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(i -> i.getArgument(0));

        PurchaseItemRequest itemReq = PurchaseItemRequest.builder()
                .productId(10L)
                .quantity(10)
                .unitCost(new BigDecimal("1000.00"))
                .build();

        PurchaseRequest request = PurchaseRequest.builder()
                .supplierId(1L)
                .paymentMethod("CREDITO_PROVEEDOR")
                .items(Collections.singletonList(itemReq))
                .build();

        PurchaseResponse response = purchaseService.createPurchase(request, "admin");

        assertNotNull(response);
        assertEquals("PENDING", response.getPaymentStatus());
        assertEquals("CREDITO_PROVEEDOR", response.getPaymentMethod());
        assertEquals(LocalDate.now().plusDays(30), response.getPaymentDueDate());
    }

    @Test
    void cancelPurchase_withSufficientStock_shouldRevertStockAndRecordDevolucionKardex() {
        Purchase purchase = Purchase.builder()
                .id(50L)
                .purchaseNumber("COMP-0050")
                .supplier(testSupplier)
                .status(PurchaseStatus.RECEIVED)
                .items(Collections.singletonList(
                        PurchaseItem.builder()
                                .id(1L)
                                .product(testProduct)
                                .quantity(5)
                                .unitCost(new BigDecimal("1000.00"))
                                .build()
                ))
                .build();

        testProduct.setCantidad(12); // stock actual es 12

        when(purchaseRepository.findById(50L)).thenReturn(Optional.of(purchase));
        when(productoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testProduct));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(i -> i.getArgument(0));

        PurchaseResponse cancelled = purchaseService.cancelPurchase(50L, "Devolución por producto vencido", "supervisor");

        assertNotNull(cancelled);
        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals(7, testProduct.getCantidad()); // 12 - 5 = 7

        verify(inventoryMovementRepository, times(1)).save(argThat(m ->
                m.getMovementType() == InventoryMovementType.DEVOLUCION &&
                m.getQuantity() == 5 &&
                m.getPreviousStock() == 12 &&
                m.getNewStock() == 7
        ));
    }

    @Test
    void cancelPurchase_withInsufficientStock_shouldThrowIllegalStateException() {
        Purchase purchase = Purchase.builder()
                .id(51L)
                .purchaseNumber("COMP-0051")
                .supplier(testSupplier)
                .status(PurchaseStatus.RECEIVED)
                .items(Collections.singletonList(
                        PurchaseItem.builder()
                                .id(1L)
                                .product(testProduct)
                                .quantity(10) // compramos 10
                                .unitCost(new BigDecimal("1000.00"))
                                .build()
                ))
                .build();

        testProduct.setCantidad(4); // sólo quedan 4 en almacén porque ya se vendieron 6 en el POS

        when(purchaseRepository.findById(51L)).thenReturn(Optional.of(purchase));
        when(productoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testProduct));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                purchaseService.cancelPurchase(51L, "Intento de anulación", "admin")
        );

        assertTrue(ex.getMessage().contains("No es posible anular la compra"));
        assertTrue(ex.getMessage().contains("menor a las 10 unidades"));
    }
}
