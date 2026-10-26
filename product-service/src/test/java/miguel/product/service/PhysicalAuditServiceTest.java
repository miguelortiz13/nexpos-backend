package miguel.product.service;

import miguel.product.dto.CountItemRequest;
import miguel.product.dto.CreateAuditSessionRequest;
import miguel.product.dto.PhysicalAuditItemDTO;
import miguel.product.dto.PhysicalAuditSessionDTO;
import miguel.product.model.*;
import miguel.product.repository.InventoryMovementRepository;
import miguel.product.repository.PhysicalAuditItemRepository;
import miguel.product.repository.PhysicalAuditSessionRepository;
import miguel.product.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhysicalAuditServiceTest {

    @Mock
    private PhysicalAuditSessionRepository sessionRepository;

    @Mock
    private PhysicalAuditItemRepository itemRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @InjectMocks
    private PhysicalAuditService auditService;

    private Producto testProduct;
    private PhysicalAuditSession testSession;

    @BeforeEach
    void setUp() {
        testProduct = new Producto();
        testProduct.setId(1L);
        testProduct.setCodigoBarras("7702001001001");
        testProduct.setNombre("Arroz Blanco 1000g");
        testProduct.setPrecio(new BigDecimal("4000.00"));
        testProduct.setCostPrice(new BigDecimal("3000.00"));
        testProduct.setCantidad(25);

        testSession = PhysicalAuditSession.builder()
                .id(1L)
                .sessionCode("AUD-2026-0001")
                .status(AuditSessionStatus.EN_CONTEO)
                .auditorUsername("auditor_juan")
                .startedAt(LocalDateTime.now())
                .totalItemsCounted(0)
                .totalDifferenceUnits(0)
                .totalCostImpact(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("startAuditSession crea una sesión en estado EN_CONTEO")
    void startAuditSession_shouldCreateSessionInCount() {
        when(sessionRepository.count()).thenReturn(0L);
        when(sessionRepository.save(any(PhysicalAuditSession.class))).thenAnswer(i -> {
            PhysicalAuditSession s = i.getArgument(0);
            s.setId(10L);
            return s;
        });

        PhysicalAuditSessionDTO dto = auditService.startAuditSession(new CreateAuditSessionRequest("Toma semestral"), "auditor1");

        assertNotNull(dto);
        assertEquals(AuditSessionStatus.EN_CONTEO, dto.getStatus());
        assertTrue(dto.getSessionCode().startsWith("AUD-"));
        verify(sessionRepository).save(any(PhysicalAuditSession.class));
    }

    @Test
    @DisplayName("recordItemCount calcula diferencia y actualiza totales")
    void recordItemCount_shouldComputeDifference() {
        CountItemRequest request = CountItemRequest.builder()
                .productId(1L)
                .physicalStock(22) // Sistema tiene 25 -> dif = -3
                .notes("3 unidades faltantes")
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(itemRepository.findBySessionIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        when(itemRepository.save(any(PhysicalAuditItem.class))).thenAnswer(i -> {
            PhysicalAuditItem item = i.getArgument(0);
            item.setId(50L);
            return item;
        });

        PhysicalAuditItemDTO itemDto = auditService.recordItemCount(1L, request);

        assertNotNull(itemDto);
        assertEquals(25, itemDto.getSystemStock());
        assertEquals(22, itemDto.getPhysicalStock());
        assertEquals(-3, itemDto.getDifference());
        assertEquals(new BigDecimal("-9000.00"), itemDto.getCostDifference()); // -3 * 3000
    }

    @Test
    @DisplayName("reconcileAudit aplica diferencias a stock y crea movimiento de AJUSTE")
    void reconcileAudit_shouldUpdateStockAndCreateAjuste() {
        PhysicalAuditItem item = PhysicalAuditItem.builder()
                .id(1L)
                .session(testSession)
                .product(testProduct)
                .systemStock(25)
                .physicalStock(20) // Faltan 5
                .difference(-5)
                .unitCost(new BigDecimal("3000.00"))
                .costDifference(new BigDecimal("-15000.00"))
                .countedAt(LocalDateTime.now())
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(itemRepository.findBySessionIdOrderByCountedAtDesc(1L)).thenReturn(Collections.singletonList(item));
        when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testProduct));
        when(sessionRepository.save(any(PhysicalAuditSession.class))).thenReturn(testSession);

        PhysicalAuditSessionDTO result = auditService.reconcileAudit(1L, "supervisor");

        assertNotNull(result);
        assertEquals(AuditSessionStatus.CONCILIADO, testSession.getStatus());
        assertEquals(20, testProduct.getCantidad()); // Ajustado a 20

        verify(productoRepository).save(testProduct);
        verify(inventoryMovementRepository).save(any(InventoryMovement.class));
    }
}
