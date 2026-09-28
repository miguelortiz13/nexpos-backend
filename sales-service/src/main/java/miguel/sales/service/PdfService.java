package miguel.sales.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import miguel.sales.model.Sale;
import miguel.sales.model.SaleItem;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(5, 150, 105));
        Paragraph title = new Paragraph("NEXPOS RETAIL & COMMERCE", headerFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        Paragraph subTitle = new Paragraph("NIT: 900.123.456-7 | Sistema de Gestión Comercial\nComprobante de Venta POS", subHeaderFont);
        subTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subTitle);

        document.add(Chunk.NEWLINE);

        // Datos de la Venta y Cliente
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font valFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1, 1});

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("No. Venta: #" + sale.getId(), labelFont));
        String formattedDate = sale.getSaleDate() != null ? sale.getSaleDate().format(DATE_FORMATTER) : "N/A";
        leftCell.addElement(new Paragraph("Fecha: " + formattedDate, valFont));
        leftCell.addElement(new Paragraph("Medio de Pago: " + (sale.getPaymentMethod() != null ? sale.getPaymentMethod() : "EFECTIVO"), valFont));
        infoTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.addElement(new Paragraph("Cliente: " + (sale.getCustomerName() != null ? sale.getCustomerName() : "Consumidor Final"), valFont));
        rightCell.addElement(new Paragraph("C.C. / NIT: " + (sale.getCustomerDoc() != null ? sale.getCustomerDoc() : "222222222222"), valFont));
        infoTable.addCell(rightCell);

        document.add(infoTable);
        document.add(Chunk.NEWLINE);

        // Tabla de Productos
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new int[]{4, 2, 2, 2});

        addTableHeader(table);

        if (sale.getItems() != null) {
            for (SaleItem item : sale.getItems()) {
                addTableRow(table, item);
            }
        }

        document.add(table);
        document.add(Chunk.NEWLINE);

        // Resumen Financiero (Total, Recibido, Cambio)
        Font summaryFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(41, 128, 185));

        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(50);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        addSummaryRow(totalTable, "TOTAL A PAGAR:", "$" + sale.getTotalAmount(), totalFont);

        if (sale.getAmountPaid() != null) {
            addSummaryRow(totalTable, "Monto Recibido:", "$" + sale.getAmountPaid(), summaryFont);
        }
        if (sale.getChangeAmount() != null) {
            addSummaryRow(totalTable, "Cambio / Vuelto:", "$" + sale.getChangeAmount(), summaryFont);
        }

        document.add(totalTable);

        // Pie de página
        document.add(Chunk.NEWLINE);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);
        Paragraph footer = new Paragraph("¡Gracias por su compra! Comprobante emitido por NexPOS Retail.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private void addTableHeader(PdfPTable table) {
        Object[][] headers = {
                {"Producto", Element.ALIGN_LEFT},
                {"Cantidad", Element.ALIGN_CENTER},
                {"Precio Unit.", Element.ALIGN_RIGHT},
                {"Subtotal", Element.ALIGN_RIGHT}
        };

        for (Object[] header : headers) {
            PdfPCell headerCell = new PdfPCell(new Phrase((String) header[0], FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
            headerCell.setHorizontalAlignment((Integer) header[1]);
            headerCell.setBackgroundColor(new Color(41, 128, 185));
            headerCell.setPadding(6);
            table.addCell(headerCell);
        }
    }

    private void addTableRow(PdfPTable table, SaleItem item) {
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

        PdfPCell c1 = new PdfPCell(new Phrase(item.getProductName(), cellFont));
        c1.setPadding(5);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), cellFont));
        c2.setHorizontalAlignment(Element.ALIGN_CENTER);
        c2.setPadding(5);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase("$" + item.getUnitPrice(), cellFont));
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c3.setPadding(5);
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase("$" + item.getSubTotal(), cellFont));
        c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c4.setPadding(5);
        table.addCell(c4);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, font));
        cellLabel.setBorder(Rectangle.NO_BORDER);
        cellLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellLabel.setPadding(3);
        table.addCell(cellLabel);

        PdfPCell cellValue = new PdfPCell(new Phrase(value, font));
        cellValue.setBorder(Rectangle.NO_BORDER);
        cellValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellValue.setPadding(3);
        table.addCell(cellValue);
    }
}
