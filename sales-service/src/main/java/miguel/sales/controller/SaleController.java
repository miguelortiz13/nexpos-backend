package miguel.sales.controller;

import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import miguel.sales.dto.SaleRequest;
import miguel.sales.model.CreditNote;
import miguel.sales.model.Invoice;
import miguel.sales.model.Sale;
import miguel.sales.service.PdfService;
import miguel.sales.service.SaleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final PdfService pdfService;

    @PostMapping
    public ResponseEntity<Sale> createSale(@RequestBody SaleRequest request, java.security.Principal principal) {
        String cashier = (principal != null) ? principal.getName() : "cajero_pos";
        return new ResponseEntity<>(saleService.createSale(request, cashier), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sale> getSale(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getSaleById(id));
    }

    @GetMapping
    public ResponseEntity<List<Sale>> getSales() {
        return ResponseEntity.ok(saleService.getAllSales());
    }

    @GetMapping("/{id}/invoice")
    public void downloadInvoice(@PathVariable Long id, HttpServletResponse response) throws IOException, DocumentException {
        Sale sale = saleService.getSaleById(id);
        byte[] pdfBytes = pdfService.generateInvoicePdf(sale);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=invoice_" + id + ".pdf");
        response.setContentLength(pdfBytes.length);

        try (OutputStream os = response.getOutputStream()) {
            os.write(pdfBytes);
            os.flush();
        }
    }

    @GetMapping("/{id}/electronic-invoice")
    public ResponseEntity<Invoice> getElectronicInvoice(@PathVariable Long id) {
        Sale sale = saleService.getSaleById(id);
        return ResponseEntity.ok(sale.getInvoice());
    }

    @PostMapping("/{id}/annul")
    public ResponseEntity<CreditNote> annulSale(@PathVariable Long id,
                                                @RequestBody(required = false) miguel.sales.dto.AnnulSaleRequest request,
                                                java.security.Principal principal) {
        String username = (principal != null) ? principal.getName() : "cajero_pos";
        CreditNote creditNote = saleService.annulSale(id, request, username);
        return ResponseEntity.ok(creditNote);
    }

    @GetMapping("/{id}/credit-note")
    public ResponseEntity<CreditNote> getCreditNote(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getCreditNoteBySaleId(id));
    }

    @GetMapping("/credit-notes")
    public ResponseEntity<List<CreditNote>> getAllCreditNotes() {
        return ResponseEntity.ok(saleService.getAllCreditNotes());
    }

    @GetMapping("/{id}/credit-note/pdf")
    public void downloadCreditNotePdf(@PathVariable Long id, HttpServletResponse response) throws IOException, DocumentException {
        Sale sale = saleService.getSaleById(id);
        CreditNote creditNote = saleService.getCreditNoteBySaleId(id);
        byte[] pdfBytes = pdfService.generateCreditNotePdf(creditNote, sale);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=credit_note_" + creditNote.getCreditNoteNumber() + ".pdf");
        response.setContentLength(pdfBytes.length);

        try (OutputStream os = response.getOutputStream()) {
            os.write(pdfBytes);
            os.flush();
        }
    }
}
