package miguel.product.repository;

import miguel.product.model.PhysicalAuditItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhysicalAuditItemRepository extends JpaRepository<PhysicalAuditItem, Long> {

    List<PhysicalAuditItem> findBySessionIdOrderByCountedAtDesc(Long sessionId);

    Optional<PhysicalAuditItem> findBySessionIdAndProductId(Long sessionId, Long productId);
}
