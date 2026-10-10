package miguel.sales.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import miguel.sales.model.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public byte[] generateInvoicePdf(Sale sale) throws DocumentException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);

        document.open();

        // Encabezado Punto de Venta
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(5, 150, 105));
        Paragraph title = new Paragraph("NEXPOS RETAIL & COMMERCE", headerFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Paragraph subTitle = new Paragraph("NIT: 900.785.412-8 • Régimen Común • Cali - Colombia\nDocumento Equivalente Electrónico POS (Res. DIAN 18764000001)", subHeaderFont);
        subTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subTitle);

        document.add(Chunk.NEWLINE);

        // Datos de la Venta, Factura y Cliente
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font valFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Font cudeFont = FontFactory.getFont(FontFactory.COURIER, 7, Color.DARK_GRAY);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1, 1});

        Invoice invoice = sale.getInvoice();
        String invoiceNum = (invoice != null && invoice.getInvoiceNumber() != null)
                ? invoice.getInvoiceNumber()
                : "FAC-" + sale.getId();

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("Tiquete / Factura POS: " + invoiceNum, labelFont));
        String formattedDate = sale.getSaleDate() != null ? sale.getSaleDate().format(DATE_FORMATTER) : "N/A";
        leftCell.addElement(new Paragraph("Fecha y Hora: " + formattedDate, valFont));
        leftCell.addElement(new Paragraph("Cajero: " + (sale.getCashierUsername() != null ? sale.getCashierUsername() : "Caja 1"), valFont));
        String methodDisplay = (sale.getPaymentMethod() != null ? sale.getPaymentMethod() : "EFECTIVO");
        if ("MIXTO".equalsIgnoreCase(methodDisplay)) {
            methodDisplay = "MIXTO (Pago Combinado)";
        }
        leftCell.addElement(new Paragraph("Medio de Pago: " + methodDisplay, valFont));
        infoTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.addElement(new Paragraph("Cliente: " + (sale.getCustomerName() != null ? sale.getCustomerName() : "Consumidor Final"), valFont));
        rightCell.addElement(new Paragraph("C.C. / NIT: " + (sale.getCustomerDoc() != null ? sale.getCustomerDoc() : "222222222222"), valFont));
        if (invoice != null && invoice.getFactusStatus() != null) {
            rightCell.addElement(new Paragraph("Estado DIAN: " + invoice.getFactusStatus(), labelFont));
        }
        infoTable.addCell(rightCell);

        document.add(infoTable);
        document.add(Chunk.NEWLINE);

        // Tabla de Productos
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new int[]{4, 1, 2, 1, 2});

        addTableHeader(table);

        BigDecimal totalIva = BigDecimal.ZERO;
        BigDecimal totalBase = BigDecimal.ZERO;

        if (sale.getItems() != null) {
            for (SaleItem item : sale.getItems()) {
                addTableRow(table, item);
                if (item.getIvaAmount() != null) {
                    totalIva = totalIva.add(item.getIvaAmount());
                }
                if (item.getBaseAmount() != null) {
                    totalBase = totalBase.add(item.getBaseAmount());
                }
            }
        }

        document.add(table);
        document.add(Chunk.NEWLINE);

        // Resumen Financiero y Desglose de Impuestos
        Font summaryFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(5, 150, 105));

        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(55);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        if (totalBase.compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(totalTable, "Base Gravable:", "$" + totalBase, summaryFont);
            addSummaryRow(totalTable, "IVA Total (Discriminado):", "$" + totalIva, summaryFont);
        }

        addSummaryRow(totalTable, "TOTAL A PAGAR:", "$" + sale.getTotalAmount(), totalFont);

        if ("MIXTO".equalsIgnoreCase(sale.getPaymentMethod())) {
            Font splitFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
            if (sale.getCashAmount() != null && sale.getCashAmount().compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(totalTable, "• Efectivo:", "$" + sale.getCashAmount(), splitFont);
            }
            if (sale.getCardAmount() != null && sale.getCardAmount().compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(totalTable, "• Tarjeta / Datáfono:", "$" + sale.getCardAmount(), splitFont);
            }
            if (sale.getTransferAmount() != null && sale.getTransferAmount().compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(totalTable, "• Transferencia / QR:", "$" + sale.getTransferAmount(), splitFont);
            }
            if (sale.getOtherAmount() != null && sale.getOtherAmount().compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(totalTable, "• Otros Medios:", "$" + sale.getOtherAmount(), splitFont);
            }
        }

        if (sale.getAmountPaid() != null) {
            addSummaryRow(totalTable, "Monto Recibido:", "$" + sale.getAmountPaid(), summaryFont);
        }
        if (sale.getChangeAmount() != null && sale.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(totalTable, "Cambio / Vuelto:", "$" + sale.getChangeAmount(), summaryFont);
        }

        document.add(totalTable);

        // Información Oficial DIAN / CUDE
        if (invoice != null && invoice.getCude() != null) {
            document.add(Chunk.NEWLINE);
            PdfPTable dianBox = new PdfPTable(1);
            dianBox.setWidthPercentage(100);
            PdfPCell dianCell = new PdfPCell();
            dianCell.setBackgroundColor(new Color(245, 247, 250));
            dianCell.setPadding(6);
            dianCell.addElement(new Paragraph("CUDE (Código Único de Documento Electrónico):", labelFont));
            dianCell.addElement(new Paragraph(invoice.getCude(), cudeFont));
            if (invoice.getQrData() != null) {
                dianCell.addElement(new Paragraph("Verificación previa DIAN: " + invoice.getQrData(), cudeFont));
            }
            dianBox.addCell(dianCell);
            document.add(dianBox);
        }

        // Pie de página
        document.add(Chunk.NEWLINE);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
        Paragraph footer = new Paragraph("¡Gracias por su compra! Documento Equivalente Electrónico generado por NexPOS Cloud v2.0.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private void addTableHeader(PdfPTable table) {
        Object[][] headers = {
                {"Producto", Element.ALIGN_LEFT},
                {"Cant.", Element.ALIGN_CENTER},
                {"Precio Unit.", Element.ALIGN_RIGHT},
                {"IVA", Element.ALIGN_CENTER},
                {"Subtotal", Element.ALIGN_RIGHT}
        };

        for (Object[] header : headers) {
            PdfPCell headerCell = new PdfPCell(new Phrase((String) header[0], FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
            headerCell.setHorizontalAlignment((Integer) header[1]);
            headerCell.setBackgroundColor(new Color(5, 150, 105));
            headerCell.setPadding(5);
            table.addCell(headerCell);
        }
    }

    private void addTableRow(PdfPTable table, SaleItem item) {
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

        PdfPCell c1 = new PdfPCell(new Phrase(item.getProductName(), cellFont));
        c1.setPadding(4);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), cellFont));
        c2.setHorizontalAlignment(Element.ALIGN_CENTER);
        c2.setPadding(4);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase("$" + item.getUnitPrice(), cellFont));
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c3.setPadding(4);
        table.addCell(c3);

        String ivaLabel = item.getIvaRate() != null
                ? (item.getIvaRate().multiply(BigDecimal.valueOf(100)).intValue() + "%")
                : "19%";
        PdfPCell c4 = new PdfPCell(new Phrase(ivaLabel, cellFont));
        c4.setHorizontalAlignment(Element.ALIGN_CENTER);
        c4.setPadding(4);
        table.addCell(c4);

        PdfPCell c5 = new PdfPCell(new Phrase("$" + item.getSubTotal(), cellFont));
        c5.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c5.setPadding(4);
        table.addCell(c5);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, font));
        cellLabel.setBorder(Rectangle.NO_BORDER);
        cellLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellLabel.setPadding(2);
        table.addCell(cellLabel);

        PdfPCell cellValue = new PdfPCell(new Phrase(value, font));
        cellValue.setBorder(Rectangle.NO_BORDER);
        cellValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellValue.setPadding(2);
        table.addCell(cellValue);
    }

    public byte[] generateCreditNotePdf(CreditNote creditNote, Sale sale) throws DocumentException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);

        document.open();

        // Encabezado Nota Crédito Electrónica DIAN
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(220, 38, 38));
        Paragraph title = new Paragraph("NOTA CRÉDITO ELECTRÓNICA DIAN", headerFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Paragraph subTitle = new Paragraph("NEXPOS RETAIL & COMMERCE • NIT: 900.785.412-8\nAnulación y Devolución Oficial ante la DIAN (Decreto 358 / Res. 000165)", subHeaderFont);
        subTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subTitle);

        document.add(Chunk.NEWLINE);

        // Datos de la Nota Crédito y Referencia a Factura
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font valFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Font cudeFont = FontFactory.getFont(FontFactory.COURIER, 7, Color.DARK_GRAY);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1, 1});

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("No. Nota Crédito: " + creditNote.getCreditNoteNumber(), labelFont));
        leftCell.addElement(new Paragraph("Factura Afectada: " + creditNote.getInvoiceNumber(), labelFont));
        String formattedDate = creditNote.getCreatedAt() != null ? creditNote.getCreatedAt().format(DATE_FORMATTER) : "N/A";
        leftCell.addElement(new Paragraph("Fecha y Hora Emisión: " + formattedDate, valFont));
        leftCell.addElement(new Paragraph("Cajero / Responsable: " + (creditNote.getCreatedBy() != null ? creditNote.getCreatedBy() : "Caja 1"), valFont));
        leftCell.addElement(new Paragraph("Concepto DIAN: [" + creditNote.getConceptCode() + "] " + creditNote.getConceptDescription(), valFont));
        leftCell.addElement(new Paragraph("Motivo: " + creditNote.getReason(), valFont));
        infoTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.addElement(new Paragraph("Cliente: " + (sale.getCustomerName() != null ? sale.getCustomerName() : "Consumidor Final"), valFont));
        rightCell.addElement(new Paragraph("C.C. / NIT: " + (sale.getCustomerDoc() != null ? sale.getCustomerDoc() : "222222222222"), valFont));
        rightCell.addElement(new Paragraph("Estado DIAN: " + creditNote.getFactusStatus(), labelFont));
        if (creditNote.getOriginalCude() != null) {
            String shortCude = creditNote.getOriginalCude().substring(0, Math.min(24, creditNote.getOriginalCude().length())) + "...";
            rightCell.addElement(new Paragraph("CUDE Factura: " + shortCude, cudeFont));
        }
        infoTable.addCell(rightCell);

        document.add(infoTable);
        document.add(Chunk.NEWLINE);

        // Tabla de Productos Reingresados
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new int[]{4, 1, 2, 1, 2});

        addCreditNoteTableHeader(table);

        BigDecimal totalIva = BigDecimal.ZERO;
        BigDecimal totalBase = BigDecimal.ZERO;

        if (sale.getItems() != null) {
            for (SaleItem item : sale.getItems()) {
                addTableRow(table, item);
                if (item.getIvaAmount() != null) {
                    totalIva = totalIva.add(item.getIvaAmount());
                }
                if (item.getBaseAmount() != null) {
                    totalBase = totalBase.add(item.getBaseAmount());
                }
            }
        }

        document.add(table);
        document.add(Chunk.NEWLINE);

        // Resumen Financiero y Reembolso
        Font summaryFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(220, 38, 38));

        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(60);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        if (totalBase.compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(totalTable, "Base Gravable Revertida:", "$" + totalBase, summaryFont);
            addSummaryRow(totalTable, "IVA Revertido:", "$" + totalIva, summaryFont);
        }
        addSummaryRow(totalTable, "TOTAL ANULADO / DEVUELTO:", "$" + creditNote.getTotalAmount(), totalFont);

        if (creditNote.getRefundCash() != null && creditNote.getRefundCash().compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(totalTable, "Reembolso en Efectivo (Caja):", "$" + creditNote.getRefundCash(), summaryFont);
        }
        if (creditNote.getRefundOther() != null && creditNote.getRefundOther().compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(totalTable, "Reembolso Tarjeta/Transf/Otros:", "$" + creditNote.getRefundOther(), summaryFont);
        }

        document.add(totalTable);
        document.add(Chunk.NEWLINE);

        // Bloque CUDE y Validación DIAN
        if (creditNote.getCude() != null) {
            Paragraph cudeLabel = new Paragraph("CUDE NOTA CRÉDITO DIAN (Código Único de Documento Electrónico):", labelFont);
            Paragraph cudeVal = new Paragraph(creditNote.getCude(), cudeFont);
            document.add(cudeLabel);
            document.add(cudeVal);
            document.add(Chunk.NEWLINE);
        }

        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
        Paragraph footer = new Paragraph(
                "Este documento certifica la anulación contable y tributaria de la factura electrónica indicada.\n" +
                "El inventario asociado ha sido reingresado al Kardex del sistema NexPOS.\n" +
                (creditNote.getDianResponseMessage() != null ? creditNote.getDianResponseMessage() : "Documento validado ante la DIAN."),
                footerFont
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private void addCreditNoteTableHeader(PdfPTable table) {
        Object[][] headers = {
                {"Producto Reingresado", Element.ALIGN_LEFT},
                {"Cant.", Element.ALIGN_CENTER},
                {"Precio Unit.", Element.ALIGN_RIGHT},
                {"IVA", Element.ALIGN_CENTER},
                {"Subtotal Devuelto", Element.ALIGN_RIGHT}
        };

        for (Object[] header : headers) {
            PdfPCell headerCell = new PdfPCell(new Phrase((String) header[0], FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
            headerCell.setHorizontalAlignment((Integer) header[1]);
            headerCell.setBackgroundColor(new Color(220, 38, 38));
            headerCell.setPadding(5);
            table.addCell(headerCell);
        }
    }

    /**
     * Genera el comprobante formal en PDF de un Recibo de Caja / Abono a Cartera de cliente.
     */
    public byte[] generateCreditPaymentReceiptPdf(CustomerCreditMovement movement, Customer customer, CompanyConfig config) throws DocumentException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);

        document.open();

        String businessName = (config != null && config.getBusinessName() != null) ? config.getBusinessName() : "NEXPOS RETAIL & COMMERCE";
        String nit = (config != null && config.getNit() != null) ? config.getNit() : "900.785.412-8";
        String address = (config != null && config.getAddress() != null) ? config.getAddress() : "Cali, Colombia";
        String phone = (config != null && config.getPhone() != null) ? config.getPhone() : "(602) 889-1234";

        // Título y membrete
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(13, 148, 136));
        Paragraph title = new Paragraph(businessName, headerFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Paragraph subTitle = new Paragraph("NIT: " + nit + " • " + address + " • Tel: " + phone + "\nCOMPROBANTE DE RECAUDO Y ABONO A CARTERA", subHeaderFont);
        subTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subTitle);

        document.add(Chunk.NEWLINE);

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font valFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        // Tabla de datos del comprobante y cliente
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1, 1});

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("RECIBO DE CAJA NO: " + (movement.getReceiptNumber() != null ? movement.getReceiptNumber() : "RC-" + movement.getId()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(13, 148, 136))));
        leftCell.addElement(new Paragraph("Fecha y Hora: " + (movement.getCreatedAt() != null ? movement.getCreatedAt().format(DATE_FORMATTER) : "N/A"), valFont));
        leftCell.addElement(new Paragraph("Medio de Pago: " + (movement.getPaymentMethod() != null ? movement.getPaymentMethod() : "EFECTIVO"), valFont));
        leftCell.addElement(new Paragraph("Recibido por: " + movement.getRegisteredBy(), valFont));
        infoTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.addElement(new Paragraph("DATOS DEL CLIENTE", labelFont));
        rightCell.addElement(new Paragraph("Nombre: " + (customer != null ? customer.getName() : "Cliente"), valFont));
        rightCell.addElement(new Paragraph("Documento: " + (customer != null ? customer.getDocType() + " " + customer.getDocNumber() : "N/A"), valFont));
        rightCell.addElement(new Paragraph("Teléfono: " + (customer != null && customer.getPhone() != null ? customer.getPhone() : "N/A"), valFont));
        rightCell.addElement(new Paragraph("Dirección: " + (customer != null && customer.getAddress() != null ? customer.getAddress() : "Cali"), valFont));
        infoTable.addCell(rightCell);

        document.add(infoTable);
        document.add(Chunk.NEWLINE);

        // Tabla de desglose de cartera
        PdfPTable balanceTable = new PdfPTable(2);
        balanceTable.setWidthPercentage(100);
        balanceTable.setWidths(new float[]{2, 1});

        PdfPCell headerB1 = new PdfPCell(new Phrase("CONCEPTO / ESTADO DE CUENTA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        headerB1.setBackgroundColor(new Color(13, 148, 136));
        headerB1.setPadding(6);
        balanceTable.addCell(headerB1);

        PdfPCell headerB2 = new PdfPCell(new Phrase("MONTO COP", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        headerB2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerB2.setBackgroundColor(new Color(13, 148, 136));
        headerB2.setPadding(6);
        balanceTable.addCell(headerB2);

        addBalanceRow(balanceTable, "Saldo Anterior de Deuda:", movement.getPreviousBalance(), false, valFont);
        addBalanceRow(balanceTable, "VALOR ABONADO / RECAUDADO:", movement.getAmount(), true, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(5, 150, 105)));
        addBalanceRow(balanceTable, "Nuevo Saldo Pendiente de Deuda:", movement.getNewBalance(), false, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK));

        if (customer != null && customer.getCreditLimit() != null) {
            BigDecimal avail = customer.getCreditLimit().subtract(movement.getNewBalance()).max(BigDecimal.ZERO);
            addBalanceRow(balanceTable, "Cupo Total Aprobado:", customer.getCreditLimit(), false, valFont);
            addBalanceRow(balanceTable, "Cupo de Crédito Disponible Restante:", avail, false, valFont);
        }

        document.add(balanceTable);
        document.add(Chunk.NEWLINE);

        if (movement.getNotes() != null && !movement.getNotes().isBlank()) {
            Paragraph notesPar = new Paragraph("Observaciones: " + movement.getNotes(), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.DARK_GRAY));
            document.add(notesPar);
            document.add(Chunk.NEWLINE);
        }

        // Firmas
        PdfPTable signaturesTable = new PdfPTable(2);
        signaturesTable.setWidthPercentage(100);
        signaturesTable.setWidths(new float[]{1, 1});

        PdfPCell sigCustomer = new PdfPCell();
        sigCustomer.setBorder(Rectangle.NO_BORDER);
        sigCustomer.addElement(new Paragraph("\n\n________________________________________\nFirma del Cliente\nC.C. / NIT: " + (customer != null ? customer.getDocNumber() : ""), valFont));
        signaturesTable.addCell(sigCustomer);

        PdfPCell sigCashier = new PdfPCell();
        sigCashier.setBorder(Rectangle.NO_BORDER);
        sigCashier.addElement(new Paragraph("\n\n________________________________________\nFirma y Sello Cajero Responsable\nUsuario: " + movement.getRegisteredBy(), valFont));
        signaturesTable.addCell(sigCashier);

        document.add(signaturesTable);
        document.add(Chunk.NEWLINE);

        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
        Paragraph footer = new Paragraph(
                "NexPOS Retail System • Este recibo constituye soporte oficial y contable del recaudo efectuado a cartera.",
                footerFont
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private void addBalanceRow(PdfPTable table, String concept, BigDecimal amount, boolean isHighlight, Font font) {
        PdfPCell c1 = new PdfPCell(new Phrase(concept, font));
        c1.setPadding(6);
        if (isHighlight) {
            c1.setBackgroundColor(new Color(240, 253, 250));
        }
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(String.format("$%,.2f", amount != null ? amount : BigDecimal.ZERO), font));
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(6);
        if (isHighlight) {
            c2.setBackgroundColor(new Color(240, 253, 250));
        }
        table.addCell(c2);
    }
}
