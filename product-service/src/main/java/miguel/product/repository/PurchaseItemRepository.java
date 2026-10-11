package miguel.product.repository;

import miguel.product.model.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

    List<PurchaseItem> findByPurchaseId(Long purchaseId);

    List<PurchaseItem> findByProductId(Long productId);

    @Query("SELECT pi FROM PurchaseItem pi JOIN FETCH pi.purchase p JOIN FETCH p.supplier s " +
           "ORDER BY p.createdAt DESC")
    List<PurchaseItem> findAllWithPurchaseAndSupplier();
}
