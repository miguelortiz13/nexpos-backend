package miguel.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseResponse {

    private Long id;
    private String purchaseNumber;
    private Long supplierId;
    private String supplierName;
    private String supplierNit;
    private String supplierInvoiceNumber;
    private String status;
    private String paymentStatus;
    private String paymentMethod;
    private LocalDate paymentDueDate;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String notes;
    private String registeredBy;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private String cancellationReason;
    private List<PurchaseItemResponse> items;
}
