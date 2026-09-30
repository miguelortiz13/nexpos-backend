package miguel.sales.controller;

import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import miguel.sales.dto.SaleRequest;
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
}
