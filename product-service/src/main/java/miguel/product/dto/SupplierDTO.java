package miguel.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierDTO {

    private Long id;

    @NotBlank(message = "El NIT o identificación fiscal del proveedor es obligatorio")
    @Size(max = 50, message = "El NIT no puede superar los 50 caracteres")
    private String nit;

    @NotBlank(message = "La razón social o nombre del proveedor es obligatorio")
    @Size(max = 150, message = "La razón social no puede superar los 150 caracteres")
    private String companyName;

    private String contactName;
    private String phone;
    private String email;
    private String address;
    private String city;

    @Builder.Default
    private Integer paymentTermsDays = 0; // 0 = Contado, 15, 30, 60 días

    @Builder.Default
    private Boolean active = true;

    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
