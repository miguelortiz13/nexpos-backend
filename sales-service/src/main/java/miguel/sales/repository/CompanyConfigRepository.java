package miguel.sales.repository;

import miguel.sales.model.CompanyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyConfigRepository extends JpaRepository<CompanyConfig, Long> {
    Optional<CompanyConfig> findFirstByOrderByIdAsc();
}
