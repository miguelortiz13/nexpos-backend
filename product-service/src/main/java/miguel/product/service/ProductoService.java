package miguel.product.service;

import miguel.product.dto.ProductoDTO;
import miguel.product.model.Producto;
import miguel.product.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Producto> buscarPorCodigoBarras(String codigoBarras) {
        return productoRepository.findByCodigoBarras(codigoBarras);
    }

    @Transactional
    public Producto crearProducto(ProductoDTO productoDTO) {
        // Verificar si el código de barras ya existe
        if (productoRepository.existsByCodigoBarras(productoDTO.codigoBarras())) {
            throw new RuntimeException("El código de barras ya está registrado");
        }

        Producto producto = new Producto();
        mapearDTOaProducto(productoDTO, producto);
        producto.setCodigoBarras(productoDTO.codigoBarras());

        return productoRepository.save(producto);
    }

    @Transactional
    public Producto actualizarProducto(Long id, ProductoDTO productoDTO) {
        Producto producto = buscarPorId(id);

        // Verificar si el nuevo código de barras ya existe (si ha cambiado)
        if (!producto.getCodigoBarras().equals(productoDTO.codigoBarras()) &&
                productoRepository.existsByCodigoBarras(productoDTO.codigoBarras())) {
            throw new RuntimeException("El nuevo código de barras ya está registrado");
        }

        mapearDTOaProducto(productoDTO, producto);
        producto.setCodigoBarras(productoDTO.codigoBarras());

        return productoRepository.save(producto);
    }

    @Transactional
    public void eliminarProducto(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado con ID: " + id);
        }
        productoRepository.deleteById(id);
    }

    private void mapearDTOaProducto(ProductoDTO dto, Producto producto) {
        producto.setNombre(dto.nombre());
        producto.setMarca(dto.marca());
        producto.setPrecio(dto.precio());
        producto.setCantidad(dto.cantidad());
        producto.setCategoria(dto.categoria());
        producto.setDescripcion(dto.descripcion());
        producto.setImagen(dto.imagen());
        if (dto.ivaRate() != null) {
            producto.setIvaRate(dto.ivaRate());
        }
        if (dto.unitMeasure() != null && !dto.unitMeasure().isBlank()) {
            producto.setUnitMeasure(dto.unitMeasure());
        }
    }
}