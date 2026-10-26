package miguel.product.repository;

import jakarta.persistence.LockModeType;
import miguel.product.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Optional<Producto> findByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarras(String codigoBarras);

    /**
     * Consulta con bloqueo pesimista de escritura (SELECT ... FOR UPDATE)
     * para operaciones atómicas de deducción o ajuste de inventario concurrentes en cajas POS.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);

    /**
     * Consulta optimizada a nivel de base de datos para productos con stock menor o igual al mínimo.
     * Reemplaza el filtrado en memoria por una consulta SQL indexada.
     */
    @Query("SELECT p FROM Producto p WHERE p.cantidad <= p.minStock ORDER BY p.cantidad ASC")
    List<Producto> findLowStockProducts();

    /**
     * Búsqueda server-side indexada por nombre, marca, código de barras o categoría.
     */
    @Query("SELECT p FROM Producto p WHERE " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.marca) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "p.codigoBarras LIKE CONCAT('%', :query, '%') OR " +
           "LOWER(p.categoria) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY p.nombre ASC")
    List<Producto> searchProducts(@Param("query") String query);
}