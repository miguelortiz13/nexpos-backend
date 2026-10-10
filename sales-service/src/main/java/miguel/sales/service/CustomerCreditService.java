package miguel.sales.service;

import lombok.RequiredArgsConstructor;
import miguel.sales.dto.CustomerCreditPaymentRequest;
import miguel.sales.dto.CustomerCreditSummaryDTO;
import miguel.sales.model.*;
import miguel.sales.repository.CustomerCreditMovementRepository;
import miguel.sales.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerCreditService {

    private final CustomerRepository customerRepository;
    private final CustomerCreditMovementRepository creditMovementRepository;
    private final CompanyConfigService companyConfigService;
    private final CashShiftService cashShiftService;

    /**
     * Obtiene el resumen de crédito y deuda de un cliente específico.
     */
    public CustomerCreditSummaryDTO getCustomerCreditSummary(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + customerId));

        List<CustomerCreditMovement> movements = creditMovementRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);

        return CustomerCreditSummaryDTO.builder()
                .customerId(customer.getId())
                .docType(customer.getDocType())
                .docNumber(customer.getDocNumber())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .creditAllowed(customer.getCreditAllowed())
                .creditLimit(customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO)
                .currentDebt(customer.getCurrentDebt() != null ? customer.getCurrentDebt() : BigDecimal.ZERO)
                .availableCredit(customer.getAvailableCredit())
                .totalMovementsCount(movements.size())
                .build();
    }

    /**
     * Lista todos los movimientos históricos de la cuenta de crédito / cartera del cliente.
     */
    public List<CustomerCreditMovement> getCustomerMovements(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new IllegalArgumentException("Cliente no encontrado con ID: " + customerId);
        }
        return creditMovementRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * Obtiene el listado consolidado de cartera de clientes (todos o los que tienen crédito/deuda).
     */
    public List<CustomerCreditSummaryDTO> getAllCreditSummaries() {
        return customerRepository.findAll().stream()
                .map(cust -> CustomerCreditSummaryDTO.builder()
                        .customerId(cust.getId())
                        .docType(cust.getDocType())
                        .docNumber(cust.getDocNumber())
                        .name(cust.getName())
                        .email(cust.getEmail())
                        .phone(cust.getPhone())
                        .creditAllowed(cust.getCreditAllowed())
                        .creditLimit(cust.getCreditLimit() != null ? cust.getCreditLimit() : BigDecimal.ZERO)
                        .currentDebt(cust.getCurrentDebt() != null ? cust.getCurrentDebt() : BigDecimal.ZERO)
                        .availableCredit(cust.getAvailableCredit())
                        .totalMovementsCount(0)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Procesa un abono o pago a la deuda de un cliente y genera el recibo de caja correspondiente.
     */
    @Transactional
    public CustomerCreditMovement processCreditPayment(Long customerId, CustomerCreditPaymentRequest request, String cashierUsername) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + customerId));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor a abonar debe ser superior a 0.");
        }

        BigDecimal currentDebt = customer.getCurrentDebt() != null ? customer.getCurrentDebt() : BigDecimal.ZERO;
        if (currentDebt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El cliente " + customer.getName() + " no registra deuda pendiente por pagar.");
        }

        if (request.getAmount().compareTo(currentDebt) > 0) {
            throw new IllegalArgumentException("El valor a abonar ($" + request.getAmount() +
                    ") no puede ser superior a la deuda actual del cliente ($" + currentDebt + ").");
        }

        String cashier = (cashierUsername != null && !cashierUsername.isBlank()) ? cashierUsername : "cajero_pos";
        String receiptNumber = companyConfigService.getAndIncrementCreditReceiptNumber();

        // 1. Descontar deuda
        BigDecimal previousBalance = currentDebt;
        BigDecimal newBalance = previousBalance.subtract(request.getAmount());
        customer.setCurrentDebt(newBalance);
        customerRepository.save(customer);

        // 2. Si el abono se realiza en efectivo, impactar el arqueo de caja activo (ENTRY)
        Long cashShiftId = null;
        String paymentMethod = (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank())
                ? request.getPaymentMethod().toUpperCase()
                : "EFECTIVO";

        if ("EFECTIVO".equalsIgnoreCase(paymentMethod)) {
            cashShiftId = cashShiftService.processCreditPaymentEntry(cashier, customer, request.getAmount(), receiptNumber);
        }

        // 3. Registrar movimiento en el libro mayor de cartera
        CustomerCreditMovement movement = CustomerCreditMovement.builder()
                .customer(customer)
                .movementType(CreditMovementType.ABONO_PAGO)
                .amount(request.getAmount())
                .previousBalance(previousBalance)
                .newBalance(newBalance)
                .paymentMethod(paymentMethod)
                .receiptNumber(receiptNumber)
                .notes(request.getNotes() != null && !request.getNotes().isBlank()
                        ? request.getNotes()
                        : "Abono / Pago de Cartera Recibo #" + receiptNumber)
                .cashShiftId(cashShiftId)
                .registeredBy(cashier)
                .build();

        return creditMovementRepository.save(movement);
    }

    /**
     * Registra el cargo de deuda cuando una venta se realiza total o parcialmente a crédito.
     */
    @Transactional
    public void processCreditSale(Customer customer, Sale sale, BigDecimal creditAmount, String cashierUsername) {
        if (creditAmount == null || creditAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (customer == null || customer.getId() == null || customer.getId().equals(1L) || "222222222222".equals(customer.getDocNumber())) {
            throw new IllegalArgumentException("Las ventas a crédito requieren un cliente identificado (no es válido Consumidor Final).");
        }

        if (customer.getCreditAllowed() == null || !customer.getCreditAllowed()) {
            throw new IllegalStateException("El cliente " + customer.getName() + " (Doc: " + customer.getDocNumber() + ") no tiene cupo de crédito habilitado.");
        }

        BigDecimal limit = customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal currentDebt = customer.getCurrentDebt() != null ? customer.getCurrentDebt() : BigDecimal.ZERO;
        BigDecimal newDebt = currentDebt.add(creditAmount);

        if (newDebt.compareTo(limit) > 0) {
            BigDecimal available = limit.subtract(currentDebt).max(BigDecimal.ZERO);
            throw new IllegalArgumentException("Cupo de crédito insuficiente para '" + customer.getName() +
                    "'. Cupo total: $" + limit + ", Deuda actual: $" + currentDebt +
                    ", Disponible: $" + available + ", Monto a financiar: $" + creditAmount);
        }

        String cashier = (cashierUsername != null && !cashierUsername.isBlank()) ? cashierUsername : "cajero_pos";

        // 1. Aumentar deuda del cliente
        customer.setCurrentDebt(newDebt);
        customerRepository.save(customer);

        // 2. Registrar movimiento de cargo en la cuenta del cliente
        CustomerCreditMovement movement = CustomerCreditMovement.builder()
                .customer(customer)
                .saleId(sale.getId())
                .movementType(CreditMovementType.CARGO_VENTA)
                .amount(creditAmount)
                .previousBalance(currentDebt)
                .newBalance(newDebt)
                .paymentMethod("CREDITO")
                .notes("Cargo por Venta a Crédito #" + sale.getId() + (sale.getInvoice() != null && sale.getInvoice().getInvoiceNumber() != null ? " (" + sale.getInvoice().getInvoiceNumber() + ")" : ""))
                .registeredBy(cashier)
                .build();

        creditMovementRepository.save(movement);
    }

    /**
     * Revierte la deuda del cliente si se anula una venta que contenía porción financiada a crédito.
     */
    @Transactional
    public void revertCreditOnAnnulment(Sale sale, String cashierUsername, String reason) {
        if (sale == null || sale.getCreditAmount() == null || sale.getCreditAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (sale.getCustomerId() == null) {
            return;
        }

        Customer customer = customerRepository.findById(sale.getCustomerId()).orElse(null);
        if (customer == null) {
            return;
        }

        BigDecimal revertAmount = sale.getCreditAmount();
        BigDecimal currentDebt = customer.getCurrentDebt() != null ? customer.getCurrentDebt() : BigDecimal.ZERO;
        BigDecimal newDebt = currentDebt.subtract(revertAmount).max(BigDecimal.ZERO);

        customer.setCurrentDebt(newDebt);
        customerRepository.save(customer);

        String cashier = (cashierUsername != null && !cashierUsername.isBlank()) ? cashierUsername : "cajero_pos";

        CustomerCreditMovement movement = CustomerCreditMovement.builder()
                .customer(customer)
                .saleId(sale.getId())
                .movementType(CreditMovementType.AJUSTE_NOTA_CREDITO)
                .amount(revertAmount)
                .previousBalance(currentDebt)
                .newBalance(newDebt)
                .paymentMethod("NOTA_CREDITO")
                .notes("Ajuste por Anulación de Venta #" + sale.getId() + (reason != null ? " (" + reason + ")" : ""))
                .registeredBy(cashier)
                .build();

        creditMovementRepository.save(movement);
    }

    public CustomerCreditMovement getMovementById(Long movementId) {
        return creditMovementRepository.findById(movementId)
                .orElseThrow(() -> new IllegalArgumentException("Movimiento de cartera no encontrado con ID: " + movementId));
    }
}
