package miguel.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import miguel.product.model.AuditSessionStatus;
import miguel.product.model.PhysicalAuditSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhysicalAuditSessionDTO {
    private Long id;
    private String sessionCode;
    private AuditSessionStatus status;
    private String auditorUsername;
    private String notes;
    private Integer totalItemsCounted;
    private Integer totalDifferenceUnits;
    private BigDecimal totalCostImpact;
    private LocalDateTime startedAt;
    private LocalDateTime closedAt;
    private List<PhysicalAuditItemDTO> items;

    public static PhysicalAuditSessionDTO fromEntity(PhysicalAuditSession session, boolean includeItems) {
        List<PhysicalAuditItemDTO> itemsDto = Collections.emptyList();
        if (includeItems && session.getItems() != null) {
            itemsDto = session.getItems().stream()
                    .map(PhysicalAuditItemDTO::fromEntity)
                    .collect(Collectors.toList());
        }

        return PhysicalAuditSessionDTO.builder()
                .id(session.getId())
                .sessionCode(session.getSessionCode())
                .status(session.getStatus())
                .auditorUsername(session.getAuditorUsername())
                .notes(session.getNotes())
                .totalItemsCounted(session.getTotalItemsCounted())
                .totalDifferenceUnits(session.getTotalDifferenceUnits())
                .totalCostImpact(session.getTotalCostImpact())
                .startedAt(session.getStartedAt())
                .closedAt(session.getClosedAt())
                .items(itemsDto)
                .build();
    }
}
