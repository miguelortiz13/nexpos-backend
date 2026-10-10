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

    @Test
    void generateCreditNotePdf_shouldGenerateValidPdfDocument() throws IOException, DocumentException {
        PdfService pdfService = new PdfService();

        SaleItem item = SaleItem.builder()
                .productName("Café Especial 500g")
                .quantity(2)
                .unitPrice(new BigDecimal("25000.00"))
                .subTotal(new BigDecimal("50000.00"))
                .ivaRate(new BigDecimal("0.19"))
                .ivaAmount(new BigDecimal("7983.19"))
                .baseAmount(new BigDecimal("42016.81"))
                .build();

        Sale sale = Sale.builder()
                .id(10L)
                .customerId(102L)
                .customerName("Ana Gomez")
                .customerDoc("1020304050")
                .saleDate(LocalDateTime.now())
                .paymentMethod("EFECTIVO")
                .totalAmount(new BigDecimal("50000.00"))
                .items(Collections.singletonList(item))
                .build();

        miguel.sales.model.CreditNote creditNote = miguel.sales.model.CreditNote.builder()
                .creditNoteNumber("NC-POS-10")
                .invoiceNumber("POS-10")
                .originalCude("sample-original-cude")
                .cude("sample-nc-cude-sha384-hash")
                .qrData("https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=sample-nc-cude")
                .factusStatus("VALIDATED")
                .reason("Garantía por producto defectuoso")
                .conceptCode("2")
                .conceptDescription("Anulación de factura electrónica")
                .totalAmount(new BigDecimal("50000.00"))
                .refundCash(new BigDecimal("50000.00"))
                .refundOther(BigDecimal.ZERO)
                .createdBy("cajero1")
                .createdAt(LocalDateTime.now())
                .dianResponseMessage("Nota Crédito validada exitosamente")
                .build();

        byte[] pdfBytes = pdfService.generateCreditNotePdf(creditNote, sale);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void generateCreditPaymentReceiptPdf_shouldGenerateValidPdf() throws IOException, DocumentException {
        PdfService pdfService = new PdfService();

        miguel.sales.model.Customer customer = miguel.sales.model.Customer.builder()
                .id(105L)
                .docType("CC")
                .docNumber("1144556677")
                .name("Carlos Alberto Ruiz")
                .email("carlos.ruiz@gmail.com")
                .phone("3155551234")
                .address("Calle 5 # 34-12")
                .city("Cali")
                .creditAllowed(true)
                .creditLimit(new BigDecimal("500000.00"))
                .currentDebt(new BigDecimal("150000.00"))
                .build();

        miguel.sales.model.CustomerCreditMovement movement = miguel.sales.model.CustomerCreditMovement.builder()
                .id(1L)
                .customer(customer)
                .movementType(miguel.sales.model.CreditMovementType.ABONO_PAGO)
                .amount(new BigDecimal("100000.00"))
                .previousBalance(new BigDecimal("250000.00"))
                .newBalance(new BigDecimal("150000.00"))
                .paymentMethod("EFECTIVO")
                .receiptNumber("RC-00001")
                .notes("Abono parcial de quincena")
                .registeredBy("cajero_pos")
                .createdAt(LocalDateTime.now())
                .build();

        miguel.sales.model.CompanyConfig config = miguel.sales.model.CompanyConfig.builder()
                .businessName("NexPOS Retail Colombia S.A.S.")
                .nit("900.785.412-8")
                .address("Av. Roosevelt # 34-50")
                .phone("(602) 889-1234")
                .build();

        byte[] pdfBytes = pdfService.generateCreditPaymentReceiptPdf(movement, customer, config);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
