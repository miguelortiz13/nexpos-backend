package miguel.sales.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {
    private Long id;

    private String docType; // CC, NIT, CE, PASAPORTE, TI

    @NotBlank(message = "El número de documento es obligatorio")
    private String docNumber;

    @NotBlank(message = "El nombre o razón social es obligatorio")
    private String name;

    private String email;
    private String phone;
    private String address;
    private String city;
    private String department;
    private String notes;
    private Boolean creditAllowed;
    private java.math.BigDecimal creditLimit;
    private java.math.BigDecimal currentDebt;
    private java.math.BigDecimal availableCredit;
}
