package miguel.sales.repository;

import jakarta.persistence.LockModeType;
import miguel.sales.model.CompanyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyConfigRepository extends JpaRepository<CompanyConfig, Long> {
    Optional<CompanyConfig> findFirstByOrderByIdAsc();

    /**
     * Bloqueo pesimista para asignación atómica e inmutable de números consecutivos fiscales
     * (Facturas DIAN, Notas Crédito y Recibos de Caja) en entornos multi-caja o distribuidos.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CompanyConfig c ORDER BY c.id ASC")
    Optional<CompanyConfig> findFirstForUpdate();
}
