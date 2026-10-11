package miguel.product.repository;

import miguel.product.model.InventoryMovement;
import miguel.product.model.InventoryMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<InventoryMovement> findTop100ByOrderByCreatedAtDesc();

    List<InventoryMovement> findByMovementTypeOrderByCreatedAtDesc(InventoryMovementType movementType);

    long countByProductId(Long productId);

    @Query("SELECT m.product.id, SUM(m.quantity) FROM InventoryMovement m " +
           "WHERE m.movementType = :movementType AND m.createdAt >= :sinceDate " +
           "GROUP BY m.product.id")
    List<Object[]> sumQuantityByProductAndMovementTypeSince(
            @Param("movementType") InventoryMovementType movementType,
            @Param("sinceDate") LocalDateTime sinceDate);
}
