package miguel.sales.service;

import miguel.product.model.Producto;
import miguel.product.repository.InventoryMovementRepository;
import miguel.product.repository.ProductoRepository;
import miguel.sales.dto.SaleItemRequest;
import miguel.sales.dto.SaleRequest;
import miguel.sales.model.Customer;
import miguel.sales.model.Invoice;
import miguel.sales.model.Sale;
import miguel.sales.repository.InvoiceRepository;
import miguel.sales.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private CashShiftService cashShiftService;

    @Mock
    private CompanyConfigService companyConfigService;

    @Mock
    private FactusService factusService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private SaleService saleService;

    private Producto testProduct;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testProduct = Producto.builder()
                .id(1L)
                .nombre("Producto Prueba")
                .precio(new BigDecimal("25000.00"))
                .cantidad(10)
                .costPrice(new BigDecimal("15000.00"))
                .ivaRate(new BigDecimal("0.19"))
                .build();

        testCustomer = Customer.builder()
                .id(1L)
                .name("Consumidor Final")
                .docNumber("222222222222")
                .email("test@pos.com")
                .build();
    }

    @Test
    void createSale_withSplitPayment_shouldSplitAmountsAccurately() {
        when(customerService.getOrCreateCustomer(anyString(), anyString(), any(), any())).thenReturn(testCustomer);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(companyConfigService.getAndIncrementInvoiceNumber()).thenReturn("POS-100");
        when(factusService.emitElectronicInvoice(any(), anyString())).thenReturn(Invoice.builder().invoiceNumber("POS-100").build());
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SaleRequest request = SaleRequest.builder()
                .customerDoc("222222222222")
                .customerName("Consumidor Final")
                .paymentMethod("MIXTO")
                .cardAmount(new BigDecimal("30000.00"))
                .cashAmount(new BigDecimal("20000.00"))
                .transferAmount(BigDecimal.ZERO)
                .otherAmount(BigDecimal.ZERO)
                .amountPaid(new BigDecimal("50000.00"))
                .items(Collections.singletonList(
                        SaleItemRequest.builder().productId(1L).quantity(2).build()
                ))
                .build();

        Sale created = saleService.createSale(request, "cajero1");

        assertNotNull(created);
        assertEquals(0, new BigDecimal("50000.00").compareTo(created.getTotalAmount()));
        assertEquals("MIXTO", created.getPaymentMethod());
        assertEquals(0, new BigDecimal("20000.00").compareTo(created.getCashAmount()));
        assertEquals(0, new BigDecimal("30000.00").compareTo(created.getCardAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(created.getTransferAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(created.getChangeAmount()));

        verify(cashShiftService, times(1)).addSaleToActiveShift(eq("cajero1"), any(Sale.class));
    }

    @Test
    void createSale_withSplitPaymentAndCashChange_shouldCalculateCashChange() {
        when(customerService.getOrCreateCustomer(anyString(), anyString(), any(), any())).thenReturn(testCustomer);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(companyConfigService.getAndIncrementInvoiceNumber()).thenReturn("POS-101");
        when(factusService.emitElectronicInvoice(any(), anyString())).thenReturn(Invoice.builder().invoiceNumber("POS-101").build());
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Total = 50,000. Card = 20,000. Cash tendered = 50,000 bill.
        // Required cash is 30,000. Change is 20,000.
        SaleRequest request = SaleRequest.builder()
                .customerDoc("222222222222")
                .customerName("Consumidor Final")
                .paymentMethod("MIXTO")
                .cardAmount(new BigDecimal("20000.00"))
                .cashAmount(new BigDecimal("50000.00"))
                .transferAmount(BigDecimal.ZERO)
                .items(Collections.singletonList(
                        SaleItemRequest.builder().productId(1L).quantity(2).build()
                ))
                .build();

        Sale created = saleService.createSale(request, "cajero1");

        assertNotNull(created);
        assertEquals(0, new BigDecimal("50000.00").compareTo(created.getTotalAmount()));
        assertEquals("MIXTO", created.getPaymentMethod());
        assertEquals(0, new BigDecimal("30000.00").compareTo(created.getCashAmount()));
        assertEquals(0, new BigDecimal("20000.00").compareTo(created.getCardAmount()));
        assertEquals(0, new BigDecimal("20000.00").compareTo(created.getChangeAmount()));
    }

    @Test
    void createSale_withSplitPaymentInsufficientFunds_shouldThrowException() {
        when(customerService.getOrCreateCustomer(anyString(), anyString(), any(), any())).thenReturn(testCustomer);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        SaleRequest request = SaleRequest.builder()
                .customerDoc("222222222222")
                .paymentMethod("MIXTO")
                .cardAmount(new BigDecimal("10000.00"))
                .cashAmount(new BigDecimal("10000.00")) // 20k total tendered < 50k
                .items(Collections.singletonList(
                        SaleItemRequest.builder().productId(1L).quantity(2).build()
                ))
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> saleService.createSale(request, "cajero1"));
        assertTrue(ex.getMessage().contains("menor que el total"));
    }
}
