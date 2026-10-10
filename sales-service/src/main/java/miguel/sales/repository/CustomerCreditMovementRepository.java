package miguel.sales.repository;

import miguel.sales.model.CustomerCreditMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerCreditMovementRepository extends JpaRepository<CustomerCreditMovement, Long> {

    List<CustomerCreditMovement> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<CustomerCreditMovement> findByReceiptNumber(String receiptNumber);

    List<CustomerCreditMovement> findAllByOrderByCreatedAtDesc();
}
