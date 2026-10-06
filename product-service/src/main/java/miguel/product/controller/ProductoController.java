package miguel.product.controller;

import miguel.product.dto.ProductoDTO;
import miguel.product.model.Producto;
import miguel.product.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    // GET: Listar todos (público)
    @GetMapping
    public ResponseEntity<List<Producto>> listarProductos() {
        return ResponseEntity.ok(productoService.listarTodos());
    }

    // GET: Buscar por ID (público)
    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerProducto(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.buscarPorId(id));
    }

    // POST: Crear nuevo producto (solo ADMIN)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crearProducto(@Valid @RequestBody ProductoDTO productoDTO) {
        try {
            Producto nuevoProducto = productoService.crearProducto(productoDTO);
            return new ResponseEntity<>(nuevoProducto, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // PUT: Actualizar (solo ADMIN)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoDTO productoDTO) {
        try {
            Producto productoActualizado = productoService.actualizarProducto(id, productoDTO);
            return ResponseEntity.ok(productoActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // DELETE: Eliminar (solo ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }

    // GET: Buscar por código de barras (público)
    @GetMapping("/codigo/{codigoBarras}")
    public ResponseEntity<?> buscarPorCodigoBarras(@PathVariable String codigoBarras) {
        return productoService.buscarPorCodigoBarras(codigoBarras)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST: Registrar movimiento de inventario / Kardex (ENTRADA, SALIDA, AJUSTE)
    @PostMapping("/{id}/movimientos")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> registrarMovimiento(
            @PathVariable Long id,
            @Valid @RequestBody miguel.product.dto.InventoryMovementRequest request,
            java.security.Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "ADMIN";
            miguel.product.dto.InventoryMovementDTO movimiento = productoService.registrarMovimiento(id, request, username);
            return new ResponseEntity<>(movimiento, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    // GET: Obtener historial Kardex de un producto
    @GetMapping("/{id}/movimientos")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<miguel.product.dto.InventoryMovementDTO>> obtenerMovimientosPorProducto(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerMovimientosPorProducto(id));
    }

    // GET: Obtener movimientos recientes globales de inventario
    @GetMapping("/movimientos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<miguel.product.dto.InventoryMovementDTO>> obtenerMovimientosRecientes() {
        return ResponseEntity.ok(productoService.obtenerMovimientosRecientes());
    }

    // GET: Listar productos con stock crítico (bajo stock o agotados)
    @GetMapping("/bajo-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Producto>> obtenerProductosBajoStock() {
        return ResponseEntity.ok(productoService.obtenerProductosBajoStock());
    }
}