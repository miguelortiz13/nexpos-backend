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

    @Autowired
    private miguel.product.repository.InventoryMovementRepository inventoryMovementRepository;

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

        Producto guardado = productoRepository.save(producto);

        // Si se crea con inventario inicial > 0, registrar movimiento de entrada inicial en Kardex
        if (guardado.getCantidad() > 0) {
            miguel.product.model.InventoryMovement inicial = miguel.product.model.InventoryMovement.builder()
                    .product(guardado)
                    .movementType(miguel.product.model.InventoryMovementType.ENTRADA)
                    .quantity(guardado.getCantidad())
                    .previousStock(0)
                    .newStock(guardado.getCantidad())
                    .unitCost(guardado.getCostPrice())
                    .reason("Inventario Inicial al crear producto")
                    .registeredBy("SISTEMA")
                    .build();
            inventoryMovementRepository.save(inicial);
        }

        return guardado;
    }

    @Transactional
    public Producto actualizarProducto(Long id, ProductoDTO productoDTO) {
        Producto producto = buscarPorId(id);

        // Verificar si el nuevo código de barras ya existe (si ha cambiado)
        if (!producto.getCodigoBarras().equals(productoDTO.codigoBarras()) &&
                productoRepository.existsByCodigoBarras(productoDTO.codigoBarras())) {
            throw new RuntimeException("El nuevo código de barras ya está registrado");
        }

        int stockAnterior = producto.getCantidad();
        mapearDTOaProducto(productoDTO, producto);
        producto.setCodigoBarras(productoDTO.codigoBarras());

        Producto actualizado = productoRepository.save(producto);

        // Si el stock fue cambiado directamente mediante el formulario, auditar en Kardex como AJUSTE
        if (stockAnterior != actualizado.getCantidad()) {
            miguel.product.model.InventoryMovement ajuste = miguel.product.model.InventoryMovement.builder()
                    .product(actualizado)
                    .movementType(miguel.product.model.InventoryMovementType.AJUSTE)
                    .quantity(actualizado.getCantidad())
                    .previousStock(stockAnterior)
                    .newStock(actualizado.getCantidad())
                    .unitCost(actualizado.getCostPrice())
                    .reason("Ajuste manual desde edición de producto")
                    .registeredBy("ADMIN")
                    .build();
            inventoryMovementRepository.save(ajuste);
        }

        return actualizado;
    }

    @Transactional
    public void eliminarProducto(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado con ID: " + id);
        }
        productoRepository.deleteById(id);
    }

    @Transactional
    public miguel.product.dto.InventoryMovementDTO registrarMovimiento(
            Long productId,
            miguel.product.dto.InventoryMovementRequest request,
            String username) {
        Producto producto = buscarPorId(productId);
        int previousStock = producto.getCantidad();
        int newStock;

        switch (request.getMovementType()) {
            case ENTRADA -> {
                newStock = previousStock + request.getQuantity();
                if (request.getUnitCost() != null && request.getUnitCost().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    producto.setCostPrice(request.getUnitCost());
                }
            }
            case SALIDA -> {
                if (previousStock < request.getQuantity()) {
                    throw new RuntimeException("Stock insuficiente para dar salida. Disponible: " +
                            previousStock + ", Solicitado: " + request.getQuantity());
                }
                newStock = previousStock - request.getQuantity();
            }
            case AJUSTE -> {
                newStock = request.getQuantity();
            }
            case VENTA -> {
                if (previousStock < request.getQuantity()) {
                    throw new RuntimeException("Stock insuficiente para venta. Disponible: " +
                            previousStock + ", Solicitado: " + request.getQuantity());
                }
                newStock = previousStock - request.getQuantity();
            }
            default -> throw new IllegalArgumentException("Tipo de movimiento desconocido: " + request.getMovementType());
        }

        producto.setCantidad(newStock);
        productoRepository.save(producto);

        miguel.product.model.InventoryMovement movement = miguel.product.model.InventoryMovement.builder()
                .product(producto)
                .movementType(request.getMovementType())
                .quantity(request.getQuantity())
                .previousStock(previousStock)
                .newStock(newStock)
                .unitCost(request.getUnitCost() != null ? request.getUnitCost() : producto.getCostPrice())
                .reason(request.getReason())
                .referenceId(request.getReferenceId())
                .registeredBy(username != null ? username : "SISTEMA")
                .build();

        miguel.product.model.InventoryMovement guardado = inventoryMovementRepository.save(movement);
        return miguel.product.dto.InventoryMovementDTO.fromEntity(guardado);
    }

    @Transactional(readOnly = true)
    public List<miguel.product.dto.InventoryMovementDTO> obtenerMovimientosPorProducto(Long productId) {
        buscarPorId(productId); // asegura que existe
        return inventoryMovementRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(miguel.product.dto.InventoryMovementDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<miguel.product.dto.InventoryMovementDTO> obtenerMovimientosRecientes() {
        return inventoryMovementRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(miguel.product.dto.InventoryMovementDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerProductosBajoStock() {
        return productoRepository.findAll()
                .stream()
                .filter(p -> p.getCantidad() <= p.getMinStock())
                .toList();
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
        if (dto.minStock() != null) {
            producto.setMinStock(dto.minStock());
        }
        if (dto.costPrice() != null) {
            producto.setCostPrice(dto.costPrice());
        }
    }
}