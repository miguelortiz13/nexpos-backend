package miguel.product.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductoDTO(
        @NotBlank(message = "El código de barras es obligatorio")
        String codigoBarras,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
        String nombre,

        String marca,

        @NotNull(message = "El precio es obligatorio")
        @PositiveOrZero(message = "El precio debe ser mayor o igual a cero")
        BigDecimal precio,

        @NotNull(message = "La cantidad es obligatoria")
        @PositiveOrZero(message = "La cantidad debe ser mayor o igual a cero")
        Integer cantidad,

        String categoria,

        String descripcion,

        String imagen,

        BigDecimal ivaRate, // 0.19, 0.05, 0.00 (opcional, default 0.19)

        String unitMeasure  // 94 = Unidad, KGM = Kilo (opcional, default '94')
) {}