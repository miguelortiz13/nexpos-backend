package miguel.sales.service;

import miguel.sales.dto.CustomerCreditPaymentRequest;
import miguel.sales.dto.CustomerCreditSummaryDTO;
import miguel.sales.model.*;
import miguel.sales.repository.CustomerCreditMovementRepository;
import miguel.sales.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerCreditServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerCreditMovementRepository creditMovementRepository;

    @Mock
    private CompanyConfigService companyConfigService;

    @Mock
    private CashShiftService cashShiftService;

    @InjectMocks
    private CustomerCreditService customerCreditService;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .id(10L)
                .docType("CC")
                .docNumber("1122334455")
                .name("Maria Fernandez")
                .creditAllowed(true)
                .creditLimit(new BigDecimal("1000000.00"))
                .currentDebt(new BigDecimal("200000.00"))
                .build();
    }

    @Test
    void processCreditSale_shouldIncreaseCustomerDebtAndCreateMovement() {
        Sale sale = Sale.builder()
                .id(50L)
                .totalAmount(new BigDecimal("150000.00"))
                .build();

        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
        when(creditMovementRepository.save(any(CustomerCreditMovement.class))).thenAnswer(i -> i.getArgument(0));

        customerCreditService.processCreditSale(testCustomer, sale, new BigDecimal("150000.00"), "cajero1");

        assertEquals(new BigDecimal("350000.00"), testCustomer.getCurrentDebt());
        verify(customerRepository).save(testCustomer);
        verify(creditMovementRepository).save(argThat(m ->
                m.getMovementType() == CreditMovementType.CARGO_VENTA &&
                m.getAmount().compareTo(new BigDecimal("150000.00")) == 0 &&
                m.getNewBalance().compareTo(new BigDecimal("350000.00")) == 0
        ));
    }

    @Test
    void processCreditSale_shouldThrowWhenCreditNotAllowed() {
        testCustomer.setCreditAllowed(false);
        Sale sale = Sale.builder().id(51L).build();

        assertThrows(IllegalStateException.class, () ->
                customerCreditService.processCreditSale(testCustomer, sale, new BigDecimal("50000.00"), "cajero1")
        );
        verify(customerRepository, never()).save(any());
    }

    @Test
    void processCreditSale_shouldThrowWhenLimitExceeded() {
        Sale sale = Sale.builder().id(52L).build();

        // Debt 200.000 + 900.000 = 1.100.000 > limit 1.000.000
        assertThrows(IllegalArgumentException.class, () ->
                customerCreditService.processCreditSale(testCustomer, sale, new BigDecimal("900000.00"), "cajero1")
        );
        verify(customerRepository, never()).save(any());
    }

    @Test
    void processCreditPayment_shouldReduceDebtAndTriggerCashEntryWhenCash() {
        CustomerCreditPaymentRequest request = CustomerCreditPaymentRequest.builder()
                .amount(new BigDecimal("100000.00"))
                .paymentMethod("EFECTIVO")
                .notes("Abono en efectivo de quincena")
                .build();

        when(customerRepository.findById(10L)).thenReturn(Optional.of(testCustomer));
        when(companyConfigService.getAndIncrementCreditReceiptNumber()).thenReturn("RC-00005");
        when(cashShiftService.processCreditPaymentEntry(eq("cajero1"), eq(testCustomer), eq(new BigDecimal("100000.00")), eq("RC-00005")))
                .thenReturn(1L);
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
        when(creditMovementRepository.save(any(CustomerCreditMovement.class))).thenAnswer(i -> i.getArgument(0));

        CustomerCreditMovement movement = customerCreditService.processCreditPayment(10L, request, "cajero1");

        assertNotNull(movement);
        assertEquals(CreditMovementType.ABONO_PAGO, movement.getMovementType());
        assertEquals("RC-00005", movement.getReceiptNumber());
        assertEquals(new BigDecimal("100000.00"), movement.getNewBalance());
        assertEquals(new BigDecimal("100000.00"), testCustomer.getCurrentDebt());

        verify(cashShiftService).processCreditPaymentEntry("cajero1", testCustomer, new BigDecimal("100000.00"), "RC-00005");
        verify(customerRepository).save(testCustomer);
        verify(creditMovementRepository).save(any(CustomerCreditMovement.class));
    }

    @Test
    void processCreditPayment_shouldThrowWhenAmountExceedsCurrentDebt() {
        CustomerCreditPaymentRequest request = CustomerCreditPaymentRequest.builder()
                .amount(new BigDecimal("500000.00")) // Debt is only 200.000
                .paymentMethod("EFECTIVO")
                .build();

        when(customerRepository.findById(10L)).thenReturn(Optional.of(testCustomer));

        assertThrows(IllegalArgumentException.class, () ->
                customerCreditService.processCreditPayment(10L, request, "cajero1")
        );
        verify(creditMovementRepository, never()).save(any());
    }

    @Test
    void revertCreditOnAnnulment_shouldRevertDebtAndCreateMovement() {
        Sale sale = Sale.builder()
                .id(77L)
                .customerId(10L)
                .creditAmount(new BigDecimal("150000.00"))
                .build();

        when(customerRepository.findById(10L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
        when(creditMovementRepository.save(any(CustomerCreditMovement.class))).thenAnswer(i -> i.getArgument(0));

        customerCreditService.revertCreditOnAnnulment(sale, "cajero1", "Garantía cliente");

        // Prev debt was 200.000, reverted 150.000 -> 50.000
        assertEquals(new BigDecimal("50000.00"), testCustomer.getCurrentDebt());
        verify(customerRepository).save(testCustomer);
        verify(creditMovementRepository).save(argThat(m ->
                m.getMovementType() == CreditMovementType.AJUSTE_NOTA_CREDITO &&
                m.getAmount().compareTo(new BigDecimal("150000.00")) == 0 &&
                m.getNewBalance().compareTo(new BigDecimal("50000.00")) == 0
        ));
    }

    @Test
    void getCustomerCreditSummary_shouldReturnCorrectCalculations() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(testCustomer));
        when(creditMovementRepository.findByCustomerIdOrderByCreatedAtDesc(10L))
                .thenReturn(Collections.emptyList());

        CustomerCreditSummaryDTO summary = customerCreditService.getCustomerCreditSummary(10L);

        assertEquals(10L, summary.getCustomerId());
        assertEquals(new BigDecimal("1000000.00"), summary.getCreditLimit());
        assertEquals(new BigDecimal("200000.00"), summary.getCurrentDebt());
        assertEquals(new BigDecimal("800000.00"), summary.getAvailableCredit());
        assertTrue(summary.getCreditAllowed());
    }
}
