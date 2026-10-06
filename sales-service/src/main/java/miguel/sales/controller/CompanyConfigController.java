package miguel.sales.controller;

import lombok.RequiredArgsConstructor;
import miguel.sales.dto.CompanyConfigDTO;
import miguel.sales.model.CompanyConfig;
import miguel.sales.service.CompanyConfigService;
import miguel.sales.service.FactusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/company-config")
@RequiredArgsConstructor
public class CompanyConfigController {

    private final CompanyConfigService companyConfigService;
    private final FactusService factusService;

    @GetMapping
    public ResponseEntity<CompanyConfig> getConfig() {
        return ResponseEntity.ok(companyConfigService.getConfig());
    }

    @PutMapping
    public ResponseEntity<CompanyConfig> updateConfig(@RequestBody CompanyConfigDTO dto) {
        return ResponseEntity.ok(companyConfigService.updateConfig(dto));
    }

    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        return ResponseEntity.ok(factusService.testConnection());
    }
}
