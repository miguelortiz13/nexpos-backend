package miguel.sales.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import miguel.product.model.Producto;
import miguel.product.repository.ProductoRepository;
import miguel.sales.dto.SaleItemRequest;
import miguel.sales.dto.SaleRequest;
import miguel.sales.model.Invoice;
import miguel.sales.model.Sale;
import miguel.sales.model.SaleItem;
import miguel.sales.repository.InvoiceRepository;
import miguel.sales.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProductoRepository productoRepository;
    private final CashShiftService cashShiftService;

    @Transactional
    public Sale createSale(SaleRequest request) {
        return createSale(request, "cajero_pos");
    }

    @Transactional
    public Sale createSale(SaleRequest request, String cashierUsername) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("La venta debe contener al menos un producto.");
        }

        Sale sale = new Sale();
        sale.setCustomerId(request.getCustomerId() != null ? request.getCustomerId() : 1L);
        sale.setCustomerName(request.getCustomerName() != null && !request.getCustomerName().isBlank()
                ? request.getCustomerName()
                : "Consumidor Final");
        sale.setCustomerDoc(request.getCustomerDoc() != null && !request.getCustomerDoc().isBlank()
                ? request.getCustomerDoc()
                : "222222222222");
        sale.setSaleDate(LocalDateTime.now());

        String paymentMethod = (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank())
                ? request.getPaymentMethod().toUpperCase()
                : "EFECTIVO";
        sale.setPaymentMethod(paymentMethod);

        List<SaleItem> items = new ArrayList<>();
        BigDecimal calculatedTotal = BigDecimal.ZERO;

        for (SaleItemRequest itemReq : request.getItems()) {
            if (itemReq.getProductId() == null) {
                throw new RuntimeException("El ID del producto es obligatorio.");
            }
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new RuntimeException("La cantidad debe ser mayor a 0.");
            }

            Producto producto = productoRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + itemReq.getProductId()));

            // 1. Validación de stock disponible
            if (producto.getCantidad() < itemReq.getQuantity()) {
                throw new RuntimeException("Stock insuficiente para '" + producto.getNombre() +
                        "'. Disponible: " + producto.getCantidad() + ", solicitado: " + itemReq.getQuantity());
            }

            // 2. Descuento atómico de inventario
            producto.setCantidad(producto.getCantidad() - itemReq.getQuantity());
            productoRepository.save(producto);

            // 3. Precio oficial de la base de datos (inmune a alteración de precio en el cliente)
            BigDecimal unitPrice = producto.getPrecio();
            BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            calculatedTotal = calculatedTotal.add(subTotal);

            SaleItem item = SaleItem.builder()
                    .productId(producto.getId())
                    .productName(producto.getNombre())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .subTotal(subTotal)
                    .sale(sale)
                    .build();
            items.add(item);
        }

        sale.setItems(items);
        sale.setTotalAmount(calculatedTotal);

        // 4. Validación de pago y cálculo de cambio/vuelto
        BigDecimal amountPaid = request.getAmountPaid() != null ? request.getAmountPaid() : calculatedTotal;
        if ("EFECTIVO".equalsIgnoreCase(paymentMethod) && amountPaid.compareTo(calculatedTotal) < 0) {
            throw new RuntimeException("El monto recibido ($" + amountPaid + ") es menor que el total a pagar ($" + calculatedTotal + ").");
        }

        BigDecimal change = amountPaid.subtract(calculatedTotal).max(BigDecimal.ZERO);
        sale.setAmountPaid(amountPaid);
        sale.setChangeAmount(change);
        sale.setCashierUsername(cashierUsername != null ? cashierUsername : "cajero_pos");

        Sale savedSale = saleRepository.save(sale);

        // 5. Generación de factura
        Invoice invoice = Invoice.builder()
                .invoiceNumber("FAC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .issuedAt(LocalDateTime.now())
                .sale(savedSale)
                .build();
        invoiceRepository.save(invoice);

        // 6. Vinculación y acumulación en el turno de caja activo
        cashShiftService.addSaleToActiveShift(cashierUsername, savedSale);

        return savedSale;
    }

    public Sale getSaleById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));
    }

    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }
}
