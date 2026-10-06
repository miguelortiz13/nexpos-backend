package miguel.product.repository;

import miguel.product.model.InventoryMovement;
import miguel.product.model.InventoryMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<InventoryMovement> findTop100ByOrderByCreatedAtDesc();

    List<InventoryMovement> findByMovementTypeOrderByCreatedAtDesc(InventoryMovementType movementType);

    long countByProductId(Long productId);
}
