package miguel.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import miguel.sales.model.CashMovement;
import miguel.sales.model.ShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftSummaryResponse {
    private Long shiftId;
    private String cashierUsername;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private ShiftStatus status;
    private BigDecimal initialAmount;
    private BigDecimal totalSalesCash;
    private BigDecimal totalSalesCard;
    private BigDecimal totalSalesTransfer;
    private BigDecimal totalSalesOther;
    private BigDecimal totalSalesAmount;
    private Integer totalSalesCount;
    private BigDecimal totalEntriesAmount;
    private BigDecimal totalExitsAmount;
    private BigDecimal expectedCashAmount;
    private BigDecimal actualCashAmount;
    private BigDecimal differenceAmount;
    private String notes;
    private String closeNotes;
    private String firstInvoiceNumber;
    private String lastInvoiceNumber;
    private List<CashMovement> movements;
}
