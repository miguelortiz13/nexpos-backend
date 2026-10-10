package miguel.sales.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import miguel.sales.dto.CustomerCreditPaymentRequest;
import miguel.sales.dto.CustomerCreditSummaryDTO;
import miguel.sales.model.CompanyConfig;
import miguel.sales.model.CustomerCreditMovement;
import miguel.sales.service.CompanyConfigService;
import miguel.sales.service.CustomerCreditService;
import miguel.sales.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerCreditController {

    private final CustomerCreditService customerCreditService;
    private final PdfService pdfService;
    private final CompanyConfigService companyConfigService;

    /**
     * Obtiene el resumen de cupo, deuda y estado de crédito de un cliente.
     */
    @GetMapping("/{id}/credit")
    public ResponseEntity<CustomerCreditSummaryDTO> getCustomerCreditSummary(@PathVariable Long id) {
        return ResponseEntity.ok(customerCreditService.getCustomerCreditSummary(id));
    }

    /**
     * Obtiene el historial completo de movimientos de cartera / estado de cuenta del cliente.
     */
    @GetMapping("/{id}/credit/movements")
    public ResponseEntity<List<CustomerCreditMovement>> getCustomerCreditMovements(@PathVariable Long id) {
        return ResponseEntity.ok(customerCreditService.getCustomerMovements(id));
    }

    /**
     * Obtiene el consolidado general de cartera y cuentas por cobrar de todos los clientes.
     */
    @GetMapping("/credit/summary")
    public ResponseEntity<List<CustomerCreditSummaryDTO>> getAllCustomersCreditSummary() {
        return ResponseEntity.ok(customerCreditService.getAllCreditSummaries());
    }

    /**
     * Registra un abono / pago de cartera a la deuda de un cliente.
     */
    @PostMapping("/{id}/credit/payment")
    public ResponseEntity<CustomerCreditMovement> registerCreditPayment(
            @PathVariable Long id,
            @Valid @RequestBody CustomerCreditPaymentRequest request,
            @RequestHeader(value = "X-Cashier-Username", required = false, defaultValue = "cajero_pos") String cashierUsername
    ) {
        CustomerCreditMovement movement = customerCreditService.processCreditPayment(id, request, cashierUsername);
        return new ResponseEntity<>(movement, HttpStatus.CREATED);
    }

    /**
     * Genera y descarga el comprobante en formato PDF oficial de un Recibo de Caja / Abono a Cartera.
     */
    @GetMapping("/credit/movements/{movementId}/receipt-pdf")
    public ResponseEntity<byte[]> getCreditPaymentReceiptPdf(@PathVariable Long movementId) {
        try {
            CustomerCreditMovement movement = customerCreditService.getMovementById(movementId);
            CompanyConfig config = companyConfigService.getConfig();
            byte[] pdfBytes = pdfService.generateCreditPaymentReceiptPdf(movement, movement.getCustomer(), config);

            String receiptNum = movement.getReceiptNumber() != null ? movement.getReceiptNumber() : "RC-" + movement.getId();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "recibo_caja_" + receiptNum + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
