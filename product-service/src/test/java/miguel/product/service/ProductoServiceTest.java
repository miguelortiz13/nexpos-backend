package miguel.product.service;

import jakarta.persistence.EntityNotFoundException;
import miguel.product.dto.ProductoDTO;
import miguel.product.model.Producto;
import miguel.product.repository.InventoryMovementRepository;
import miguel.product.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @InjectMocks
    private ProductoService productoService;

    private Producto testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Producto();
        testProduct.setId(1L);
        testProduct.setCodigoBarras("7702001001001");
        testProduct.setNombre("Arroz Diana 1000g");
        testProduct.setMarca("Diana");
        testProduct.setCategoria("Granos");
        testProduct.setPrecio(new BigDecimal("4500.00"));
        testProduct.setCostPrice(new BigDecimal("3500.00"));
        testProduct.setCantidad(4);
        testProduct.setMinStock(10);
        testProduct.setIvaRate(new BigDecimal("0.00"));
        testProduct.setUnitMeasure("94");
    }

    @Test
    @DisplayName("buscarPorId inexistente debe lanzar EntityNotFoundException")
    void buscarPorId_notFound_shouldThrowEntityNotFoundException() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productoService.buscarPorId(99L));
        verify(productoRepository).findById(99L);
    }

    @Test
    @DisplayName("buscarPorId existente debe retornar el producto")
    void buscarPorId_found_shouldReturnProduct() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        Producto found = productoService.buscarPorId(1L);

        assertNotNull(found);
        assertEquals("Arroz Diana 1000g", found.getNombre());
    }

    @Test
    @DisplayName("obtenerProductosBajoStock debe delegar en findLowStockProducts del repositorio")
    void obtenerProductosBajoStock_shouldCallFindLowStockProducts() {
        when(productoRepository.findLowStockProducts()).thenReturn(Collections.singletonList(testProduct));

        List<Producto> lowStock = productoService.obtenerProductosBajoStock();

        assertNotNull(lowStock);
        assertEquals(1, lowStock.size());
        assertTrue(lowStock.get(0).getCantidad() <= lowStock.get(0).getMinStock());
        verify(productoRepository, times(1)).findLowStockProducts();
        verify(productoRepository, never()).findAll();
    }

    @Test
    @DisplayName("buscarProductos con query debe llamar a searchProducts")
    void buscarProductos_withQuery_shouldCallSearchProducts() {
        when(productoRepository.searchProducts("Diana")).thenReturn(Collections.singletonList(testProduct));

        List<Producto> results = productoService.buscarProductos("Diana");

        assertEquals(1, results.size());
        assertEquals("Diana", results.get(0).getMarca());
        verify(productoRepository).searchProducts("Diana");
        verify(productoRepository, never()).findAll();
    }

    @Test
    @DisplayName("buscarProductos sin query o en blanco debe llamar a findAll")
    void buscarProductos_emptyQuery_shouldReturnAll() {
        when(productoRepository.findAll()).thenReturn(Collections.singletonList(testProduct));

        List<Producto> results = productoService.buscarProductos("   ");

        assertEquals(1, results.size());
        verify(productoRepository).findAll();
        verify(productoRepository, never()).searchProducts(any());
    }

    @Test
    @DisplayName("crearProducto con código de barras duplicado debe lanzar IllegalArgumentException")
    void crearProducto_duplicateBarcode_shouldThrowIllegalArgumentException() {
        ProductoDTO dto = new ProductoDTO(
                "7702001001001", "Arroz Diana", "Diana", new BigDecimal("4500"), 10, "Granos",
                "Arroz blanco", null, BigDecimal.ZERO, "94", 5, new BigDecimal("3500")
        );

        when(productoRepository.existsByCodigoBarras("7702001001001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> productoService.crearProducto(dto));
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("eliminarProducto inexistente debe lanzar EntityNotFoundException")
    void eliminarProducto_notFound_shouldThrowEntityNotFoundException() {
        when(productoRepository.existsById(99L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> productoService.eliminarProducto(99L));
        verify(productoRepository, never()).deleteById(any());
    }
}
