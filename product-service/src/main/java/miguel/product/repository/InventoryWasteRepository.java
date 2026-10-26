package miguel.product.repository;

import miguel.product.model.InventoryWaste;
import miguel.product.model.WasteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryWasteRepository extends JpaRepository<InventoryWaste, Long> {

    List<InventoryWaste> findAllByOrderByCreatedAtDesc();

    List<InventoryWaste> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<InventoryWaste> findByWasteTypeOrderByCreatedAtDesc(WasteType wasteType);

    List<InventoryWaste> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT w.wasteType, SUM(w.quantity), SUM(w.totalCost) FROM InventoryWaste w GROUP BY w.wasteType")
    List<Object[]> sumQuantityAndCostByWasteType();
}
