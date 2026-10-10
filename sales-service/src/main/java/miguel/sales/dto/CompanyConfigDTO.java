package miguel.sales.dto;

import java.time.LocalDate;

public record CompanyConfigDTO(
        Long id,
        String nit,
        String businessName,
        String tradeName,
        String address,
        String city,
        String department,
        String phone,
        String email,
        String taxRegime,
        String dianResolutionNumber,
        String dianPrefix,
        Long dianRangeFrom,
        Long dianRangeTo,
        Long dianCurrentNumber,
        String dianNcPrefix,
        Long dianNcCurrentNumber,
        String creditReceiptPrefix,
        Long creditReceiptCurrentNumber,
        String dianTechnicalKey,
        LocalDate dianStartDate,
        LocalDate dianEndDate,
        String factusApiUrl,
        String factusClientId,
        String factusClientSecret,
        String factusApiToken,
        Boolean facturacionActiva,
        String environment
) {}
