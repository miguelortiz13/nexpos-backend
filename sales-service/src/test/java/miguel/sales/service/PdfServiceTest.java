package miguel.sales.service;

import com.lowagie.text.DocumentException;
import miguel.sales.model.Invoice;
import miguel.sales.model.Sale;
import miguel.sales.model.SaleItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class PdfServiceTest {

    @Test
    void generateInvoicePdfWithoutInvoice() throws IOException, DocumentException {
        PdfService pdfService = new PdfService();

        SaleItem item = SaleItem.builder()
                .productName("Test Product")
                .quantity(2)
                .unitPrice(new BigDecimal("10.00"))
                .subTotal(new BigDecimal("20.00"))
                .ivaRate(new BigDecimal("0.19"))
                .ivaAmount(new BigDecimal("3.19"))
                .baseAmount(new BigDecimal("16.81"))
                .build();

        Sale sale = Sale.builder()
                .id(1L)
                .customerId(100L)
                .customerName("Juan Perez")
                .customerDoc("11223344")
                .saleDate(LocalDateTime.now())
                .paymentMethod("EFECTIVO")
                .amountPaid(new BigDecimal("50.00"))
                .changeAmount(new BigDecimal("30.00"))
                .totalAmount(new BigDecimal("20.00"))
                .items(Collections.singletonList(item))
                .build();

        byte[] pdfBytes = pdfService.generateInvoicePdf(sale);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void generateInvoicePdfWithElectronicInvoice() throws IOException, DocumentException {
        PdfService pdfService = new PdfService();

        SaleItem item = SaleItem.builder()
                .productName("Arroz Diana 1kg")
                .quantity(3)
                .unitPrice(new BigDecimal("4500.00"))
                .subTotal(new BigDecimal("13500.00"))
                .ivaRate(new BigDecimal("0.00"))
                .ivaAmount(BigDecimal.ZERO)
                .baseAmount(new BigDecimal("13500.00"))
                .build();

        Invoice invoice = Invoice.builder()
                .invoiceNumber("POS-1001")
                .issuedAt(LocalDateTime.now())
                .cude("a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890")
                .qrData("https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=test_cude_sample_qr")
                .factusStatus("VALIDATED")
                .build();

        Sale sale = Sale.builder()
                .id(2L)
                .customerId(101L)
                .customerName("Supermercado Cliente")
                .customerDoc("900123456")
                .saleDate(LocalDateTime.now())
                .paymentMethod("TARJETA")
                .amountPaid(new BigDecimal("13500.00"))
                .changeAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("13500.00"))
                .items(Collections.singletonList(item))
                .invoice(invoice)
                .build();

        byte[] pdfBytes = pdfService.generateInvoicePdf(sale);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void generateInvoicePdfWithSplitPayment() throws IOException, DocumentException {
        PdfService pdfService = new PdfService();

        SaleItem item1 = SaleItem.builder()
                .productName("Café Especial 500g")
                .quantity(2)
                .unitPrice(new BigDecimal("25000.00"))
                .subTotal(new BigDecimal("50000.00"))
                .ivaRate(new BigDecimal("0.19"))
                .ivaAmount(new BigDecimal("7983.19"))
                .baseAmount(new BigDecimal("42016.81"))
                .build();

        Sale sale = Sale.builder()
                .id(3L)
                .customerId(102L)
                .customerName("Ana Gomez")
                .customerDoc("1020304050")
                .saleDate(LocalDateTime.now())
                .paymentMethod("MIXTO")
                .cashAmount(new BigDecimal("20000.00"))
                .cardAmount(new BigDecimal("30000.00"))
                .transferAmount(BigDecimal.ZERO)
                .otherAmount(BigDecimal.ZERO)
                .amountPaid(new BigDecimal("70000.00"))
                .changeAmount(new BigDecimal("20000.00"))
                .totalAmount(new BigDecimal("50000.00"))
                .items(Collections.singletonList(item1))
                .build();

        byte[] pdfBytes = pdfService.generateInvoicePdf(sale);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
