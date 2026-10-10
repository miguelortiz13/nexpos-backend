package miguel.sales.service;

import lombok.RequiredArgsConstructor;
import miguel.sales.dto.CompanyConfigDTO;
import miguel.sales.model.CompanyConfig;
import miguel.sales.repository.CompanyConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CompanyConfigService {

    private final CompanyConfigRepository companyConfigRepository;

    @Transactional(readOnly = true)
    public CompanyConfig getConfig() {
        return companyConfigRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefaultConfig);
    }

    @Transactional
    public CompanyConfig updateConfig(CompanyConfigDTO dto) {
        CompanyConfig config = companyConfigRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefaultConfig);

        if (dto.nit() != null) config.setNit(dto.nit());
        if (dto.businessName() != null) config.setBusinessName(dto.businessName());
        if (dto.tradeName() != null) config.setTradeName(dto.tradeName());
        if (dto.address() != null) config.setAddress(dto.address());
        if (dto.city() != null) config.setCity(dto.city());
        if (dto.department() != null) config.setDepartment(dto.department());
        if (dto.phone() != null) config.setPhone(dto.phone());
        if (dto.email() != null) config.setEmail(dto.email());
        if (dto.taxRegime() != null) config.setTaxRegime(dto.taxRegime());

        if (dto.dianResolutionNumber() != null) config.setDianResolutionNumber(dto.dianResolutionNumber());
        if (dto.dianPrefix() != null) config.setDianPrefix(dto.dianPrefix());
        if (dto.dianRangeFrom() != null) config.setDianRangeFrom(dto.dianRangeFrom());
        if (dto.dianRangeTo() != null) config.setDianRangeTo(dto.dianRangeTo());
        if (dto.dianCurrentNumber() != null) config.setDianCurrentNumber(dto.dianCurrentNumber());
        if (dto.dianNcPrefix() != null) config.setDianNcPrefix(dto.dianNcPrefix());
        if (dto.dianNcCurrentNumber() != null) config.setDianNcCurrentNumber(dto.dianNcCurrentNumber());
        if (dto.creditReceiptPrefix() != null) config.setCreditReceiptPrefix(dto.creditReceiptPrefix());
        if (dto.creditReceiptCurrentNumber() != null) config.setCreditReceiptCurrentNumber(dto.creditReceiptCurrentNumber());
        if (dto.dianTechnicalKey() != null) config.setDianTechnicalKey(dto.dianTechnicalKey());
        if (dto.dianStartDate() != null) config.setDianStartDate(dto.dianStartDate());
        if (dto.dianEndDate() != null) config.setDianEndDate(dto.dianEndDate());

        if (dto.factusApiUrl() != null) config.setFactusApiUrl(dto.factusApiUrl());
        if (dto.factusClientId() != null) config.setFactusClientId(dto.factusClientId());
        if (dto.factusClientSecret() != null) config.setFactusClientSecret(dto.factusClientSecret());
        if (dto.factusApiToken() != null) config.setFactusApiToken(dto.factusApiToken());
        if (dto.facturacionActiva() != null) config.setFacturacionActiva(dto.facturacionActiva());
        if (dto.environment() != null) config.setEnvironment(dto.environment());

        config.setUpdatedAt(LocalDateTime.now());
        return companyConfigRepository.save(config);
    }

    @Transactional
    public synchronized String getAndIncrementInvoiceNumber() {
        CompanyConfig config = getConfig();
        Long current = config.getDianCurrentNumber() != null ? config.getDianCurrentNumber() : 1L;
        String prefix = config.getDianPrefix() != null && !config.getDianPrefix().isBlank() ? config.getDianPrefix() : "POS";

        String invoiceNumber = String.format("%s-%d", prefix, current);

        config.setDianCurrentNumber(current + 1);
        config.setUpdatedAt(LocalDateTime.now());
        companyConfigRepository.save(config);

        return invoiceNumber;
    }

    @Transactional
    public synchronized String getAndIncrementCreditNoteNumber() {
        CompanyConfig config = getConfig();
        Long current = config.getDianNcCurrentNumber() != null ? config.getDianNcCurrentNumber() : 1L;
        String prefix = config.getDianNcPrefix() != null && !config.getDianNcPrefix().isBlank() ? config.getDianNcPrefix() : "NC";

        String creditNoteNumber = String.format("%s-%d", prefix, current);

        config.setDianNcCurrentNumber(current + 1);
        config.setUpdatedAt(LocalDateTime.now());
        companyConfigRepository.save(config);

        return creditNoteNumber;
    }

    @Transactional
    public synchronized String getAndIncrementCreditReceiptNumber() {
        CompanyConfig config = getConfig();
        Long current = config.getCreditReceiptCurrentNumber() != null ? config.getCreditReceiptCurrentNumber() : 1L;
        String prefix = config.getCreditReceiptPrefix() != null && !config.getCreditReceiptPrefix().isBlank() ? config.getCreditReceiptPrefix() : "RC";

        String receiptNumber = String.format("%s-%05d", prefix, current);

        config.setCreditReceiptCurrentNumber(current + 1);
        config.setUpdatedAt(LocalDateTime.now());
        companyConfigRepository.save(config);

        return receiptNumber;
    }

    private CompanyConfig createDefaultConfig() {
        CompanyConfig config = CompanyConfig.builder()
                .nit("900.785.412-8")
                .businessName("NexPOS Retail Colombia S.A.S.")
                .tradeName("NexPOS Supermarket & Market")
                .address("Av. Roosevelt # 34-50")
                .city("Cali")
                .department("Valle del Cauca")
                .phone("(602) 889-1234")
                .email("facturacion@nexpos.co")
                .taxRegime("RESPONSABLE_IVA")
                .dianResolutionNumber("18764000001")
                .dianPrefix("POS")
                .dianRangeFrom(1L)
                .dianRangeTo(50000L)
                .dianCurrentNumber(1L)
                .dianNcPrefix("NC")
                .dianNcCurrentNumber(1L)
                .dianTechnicalKey("fc8eac422eba16e22ffd8c6f94b3f40a6e38162c")
                .dianStartDate(LocalDate.of(2026, 1, 1))
                .dianEndDate(LocalDate.of(2027, 12, 31))
                .factusApiUrl("https://api-sandbox.factus.com.co")
                .facturacionActiva(true)
                .environment("SANDBOX")
                .updatedAt(LocalDateTime.now())
                .build();
        return companyConfigRepository.save(config);
    }
}
