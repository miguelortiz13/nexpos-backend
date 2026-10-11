package miguel.product.repository;

import miguel.product.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByNit(String nit);

    boolean existsByNit(String nit);

    List<Supplier> findByActiveTrueOrderByCompanyNameAsc();

    @Query("SELECT s FROM Supplier s WHERE " +
           "LOWER(s.companyName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.nit) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.contactName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY s.companyName ASC")
    List<Supplier> searchSuppliers(@Param("query") String query);
}
