package miguel.product.repository;

import miguel.product.model.AuditSessionStatus;
import miguel.product.model.PhysicalAuditSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhysicalAuditSessionRepository extends JpaRepository<PhysicalAuditSession, Long> {

    Optional<PhysicalAuditSession> findBySessionCode(String sessionCode);

    List<PhysicalAuditSession> findAllByOrderByStartedAtDesc();

    List<PhysicalAuditSession> findByStatusOrderByStartedAtDesc(AuditSessionStatus status);

    long count();
}
