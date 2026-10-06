package miguel.sales.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import miguel.sales.model.Invoice;
import miguel.sales.model.Sale;
import miguel.sales.model.SaleItem;
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
        leftCell.addElement(new Paragraph("Medio de Pago: " + (sale.getPaymentMethod() != null ? sale.getPaymentMethod() : "EFECTIVO"), valFont));
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

        if (sale.getAmountPaid() != null) {
            addSummaryRow(totalTable, "Monto Recibido:", "$" + sale.getAmountPaid(), summaryFont);
        }
        if (sale.getChangeAmount() != null) {
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
}
