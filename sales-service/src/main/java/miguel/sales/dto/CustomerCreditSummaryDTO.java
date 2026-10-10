package miguel.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCreditSummaryDTO {
    private Long customerId;
    private String docType;
    private String docNumber;
    private String name;
    private String email;
    private String phone;
    private Boolean creditAllowed;
    private BigDecimal creditLimit;
    private BigDecimal currentDebt;
    private BigDecimal availableCredit;
    private Integer totalMovementsCount;
}
