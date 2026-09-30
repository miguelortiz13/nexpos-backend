package miguel.sales.repository;

import miguel.sales.model.CashShift;
import miguel.sales.model.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CashShiftRepository extends JpaRepository<CashShift, Long> {

    Optional<CashShift> findFirstByCashierUsernameAndStatusOrderByOpenedAtDesc(String cashierUsername, ShiftStatus status);

    Optional<CashShift> findFirstByStatusOrderByOpenedAtDesc(ShiftStatus status);

    List<CashShift> findAllByOrderByOpenedAtDesc();

    List<CashShift> findByCashierUsernameOrderByOpenedAtDesc(String cashierUsername);
}
