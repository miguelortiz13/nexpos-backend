package miguel.product.repository;

import miguel.product.model.BatchStatus;
import miguel.product.model.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

    List<ProductBatch> findByProductIdOrderByExpirationDateAsc(Long productId);

    List<ProductBatch> findByStatusOrderByExpirationDateAsc(BatchStatus status);

    Optional<ProductBatch> findByProductIdAndBatchNumber(Long productId, String batchNumber);

    /**
     * Lotes activos ordenados por FEFO (First Expired, First Out)
     */
    @Query("SELECT b FROM ProductBatch b WHERE b.product.id = :productId AND b.currentQuantity > 0 " +
           "ORDER BY b.expirationDate ASC")
    List<ProductBatch> findActiveBatchesByProductFEFO(@Param("productId") Long productId);

    /**
     * Lotes que vencen en o antes de la fecha dada y aún tienen stock
     */
    @Query("SELECT b FROM ProductBatch b WHERE b.expirationDate <= :thresholdDate AND b.currentQuantity > 0 " +
           "ORDER BY b.expirationDate ASC")
    List<ProductBatch> findExpiringBatches(@Param("thresholdDate") LocalDate thresholdDate);

    /**
     * Lotes ya vencidos con stock disponible
     */
    @Query("SELECT b FROM ProductBatch b WHERE b.expirationDate < :today AND b.currentQuantity > 0 " +
           "ORDER BY b.expirationDate ASC")
    List<ProductBatch> findExpiredBatchesWithStock(@Param("today") LocalDate today);
}
