package miguel.product.repository;

import miguel.product.model.Purchase;
import miguel.product.model.PurchasePaymentStatus;
import miguel.product.model.PurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    Optional<Purchase> findByPurchaseNumber(String purchaseNumber);

    List<Purchase> findAllByOrderByReceivedAtDesc();

    List<Purchase> findBySupplierIdOrderByReceivedAtDesc(Long supplierId);

    @Query("SELECT p FROM Purchase p WHERE " +
           "(:supplierId IS NULL OR p.supplier.id = :supplierId) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:paymentStatus IS NULL OR p.paymentStatus = :paymentStatus) AND " +
           "(p.receivedAt BETWEEN :startDate AND :endDate) " +
           "ORDER BY p.receivedAt DESC")
    List<Purchase> findWithFilters(
            @Param("supplierId") Long supplierId,
            @Param("status") PurchaseStatus status,
            @Param("paymentStatus") PurchasePaymentStatus paymentStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM Purchase p WHERE p.status = 'RECEIVED' AND p.paymentStatus = 'PENDING'")
    BigDecimal sumTotalPendingPayables();

    @Query("SELECT COUNT(p) FROM Purchase p WHERE p.status = 'RECEIVED' AND p.paymentStatus = 'PENDING'")
    long countPendingPayables();
}
