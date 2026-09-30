package miguel.sales.repository;

import miguel.sales.model.CashMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {

    List<CashMovement> findByShiftIdOrderByCreatedAtDesc(Long shiftId);
}
