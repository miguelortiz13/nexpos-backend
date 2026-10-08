package miguel.sales.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import miguel.product.model.InventoryMovement;
import miguel.product.model.InventoryMovementType;
import miguel.product.model.Producto;
import miguel.product.repository.InventoryMovementRepository;
import miguel.product.repository.ProductoRepository;
import miguel.sales.dto.SaleItemRequest;
import miguel.sales.dto.SaleRequest;
import miguel.sales.model.Customer;
import miguel.sales.model.Invoice;
import miguel.sales.model.Sale;
import miguel.sales.model.SaleItem;
import miguel.sales.repository.InvoiceRepository;
import miguel.sales.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProductoRepository productoRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final CashShiftService cashShiftService;
    private final CompanyConfigService companyConfigService;
    private final FactusService factusService;
    private final CustomerService customerService;

    @Transactional
    public Sale createSale(SaleRequest request) {
        return createSale(request, "cajero_pos");
    }

    @Transactional
    public Sale createSale(SaleRequest request, String cashierUsername) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("La venta debe contener al menos un producto.");
        }

        String doc = (request.getCustomerDoc() != null && !request.getCustomerDoc().isBlank())
                ? request.getCustomerDoc().trim()
                : "222222222222";
        String name = (request.getCustomerName() != null && !request.getCustomerName().isBlank())
                ? request.getCustomerName().trim()
                : "Consumidor Final";
        String email = request.getCustomerEmail() != null ? request.getCustomerEmail().trim() : null;

        Customer customer = customerService.getOrCreateCustomer(doc, name, email, null);

        Sale sale = new Sale();
        sale.setCustomerId(customer.getId());
        sale.setCustomerDoc(customer.getDocNumber());
        sale.setCustomerName(customer.getName());
        sale.setCustomerEmail(customer.getEmail());
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

            // 3. Precio oficial y desglose de IVA (inmune a alteración en el cliente)
            BigDecimal unitPrice = producto.getPrecio();
            BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            calculatedTotal = calculatedTotal.add(subTotal);

            BigDecimal ivaRate = producto.getIvaRate() != null ? producto.getIvaRate() : new BigDecimal("0.19");
            BigDecimal baseAmount = subTotal.divide(BigDecimal.ONE.add(ivaRate), 2, RoundingMode.HALF_UP);
            BigDecimal ivaAmount = subTotal.subtract(baseAmount);

            SaleItem item = SaleItem.builder()
                    .productId(producto.getId())
                    .productName(producto.getNombre())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .subTotal(subTotal)
                    .ivaRate(ivaRate)
                    .ivaAmount(ivaAmount)
                    .baseAmount(baseAmount)
                    .sale(sale)
                    .build();
            items.add(item);
        }

        sale.setItems(items);
        sale.setTotalAmount(calculatedTotal);

        // 4. Validación de pago y cálculo de cambio/vuelto por medio de pago
        if ("MIXTO".equalsIgnoreCase(paymentMethod)) {
            BigDecimal reqCash = request.getCashAmount() != null ? request.getCashAmount() : BigDecimal.ZERO;
            BigDecimal reqCard = request.getCardAmount() != null ? request.getCardAmount() : BigDecimal.ZERO;
            BigDecimal reqTransfer = request.getTransferAmount() != null ? request.getTransferAmount() : BigDecimal.ZERO;
            BigDecimal reqOther = request.getOtherAmount() != null ? request.getOtherAmount() : BigDecimal.ZERO;

            BigDecimal nonCashTotal = reqCard.add(reqTransfer).add(reqOther);
            BigDecimal totalTendered = nonCashTotal.add(reqCash);

            if (totalTendered.compareTo(calculatedTotal) < 0) {
                throw new RuntimeException("El monto combinado proporcionado ($" + totalTendered +
                        ") es menor que el total a pagar ($" + calculatedTotal + ").");
            }

            BigDecimal effectiveCashPortion;
            BigDecimal change;

            if (nonCashTotal.compareTo(calculatedTotal) >= 0) {
                effectiveCashPortion = BigDecimal.ZERO;
                change = totalTendered.subtract(calculatedTotal);
            } else {
                BigDecimal requiredCash = calculatedTotal.subtract(nonCashTotal);
                if (reqCash.compareTo(requiredCash) >= 0) {
                    effectiveCashPortion = requiredCash;
                    change = reqCash.subtract(requiredCash);
                } else {
                    effectiveCashPortion = reqCash;
                    change = BigDecimal.ZERO;
                }
            }

            sale.setCashAmount(effectiveCashPortion);
            sale.setCardAmount(reqCard);
            sale.setTransferAmount(reqTransfer);
            sale.setOtherAmount(reqOther);
            sale.setAmountPaid(request.getAmountPaid() != null ? request.getAmountPaid() : totalTendered);
            sale.setChangeAmount(change);
        } else if ("EFECTIVO".equalsIgnoreCase(paymentMethod)) {
            BigDecimal paid = request.getAmountPaid() != null ? request.getAmountPaid() : calculatedTotal;
            if (paid.compareTo(calculatedTotal) < 0) {
                throw new RuntimeException("El monto recibido ($" + paid + ") es menor que el total a pagar ($" + calculatedTotal + ").");
            }
            BigDecimal change = paid.subtract(calculatedTotal).max(BigDecimal.ZERO);
            sale.setCashAmount(calculatedTotal);
            sale.setCardAmount(BigDecimal.ZERO);
            sale.setTransferAmount(BigDecimal.ZERO);
            sale.setOtherAmount(BigDecimal.ZERO);
            sale.setAmountPaid(paid);
            sale.setChangeAmount(change);
        } else if ("TARJETA".equalsIgnoreCase(paymentMethod)) {
            sale.setCashAmount(BigDecimal.ZERO);
            sale.setCardAmount(calculatedTotal);
            sale.setTransferAmount(BigDecimal.ZERO);
            sale.setOtherAmount(BigDecimal.ZERO);
            sale.setAmountPaid(calculatedTotal);
            sale.setChangeAmount(BigDecimal.ZERO);
        } else if ("TRANSFERENCIA".equalsIgnoreCase(paymentMethod)) {
            sale.setCashAmount(BigDecimal.ZERO);
            sale.setCardAmount(BigDecimal.ZERO);
            sale.setTransferAmount(calculatedTotal);
            sale.setOtherAmount(BigDecimal.ZERO);
            sale.setAmountPaid(calculatedTotal);
            sale.setChangeAmount(BigDecimal.ZERO);
        } else {
            sale.setCashAmount(BigDecimal.ZERO);
            sale.setCardAmount(BigDecimal.ZERO);
            sale.setTransferAmount(BigDecimal.ZERO);
            sale.setOtherAmount(calculatedTotal);
            sale.setAmountPaid(calculatedTotal);
            sale.setChangeAmount(BigDecimal.ZERO);
        }
        sale.setCashierUsername(cashierUsername != null ? cashierUsername : "cajero_pos");

        Sale savedSale = saleRepository.save(sale);

        // 5. Registrar movimientos de auditoría en Kardex para cada producto vendido
        for (SaleItem item : savedSale.getItems()) {
            Producto prod = productoRepository.findById(item.getProductId()).orElse(null);
            if (prod != null) {
                InventoryMovement movement = InventoryMovement.builder()
                        .product(prod)
                        .movementType(InventoryMovementType.VENTA)
                        .quantity(item.getQuantity())
                        .previousStock(prod.getCantidad() + item.getQuantity())
                        .newStock(prod.getCantidad())
                        .unitCost(prod.getCostPrice())
                        .reason("Venta POS #" + savedSale.getId())
                        .referenceId(String.valueOf(savedSale.getId()))
                        .registeredBy(cashierUsername != null ? cashierUsername : "cajero_pos")
                        .build();
                inventoryMovementRepository.save(movement);
            }
        }

        // 6. Emisión de Factura / Documento Equivalente Electrónico POS ante Factus y DIAN
        String invoiceNumber = companyConfigService.getAndIncrementInvoiceNumber();
        Invoice invoice = factusService.emitElectronicInvoice(savedSale, invoiceNumber);
        Invoice savedInvoice = invoiceRepository.save(invoice);
        savedSale.setInvoice(savedInvoice);

        // 7. Vinculación y acumulación en el turno de caja activo
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
