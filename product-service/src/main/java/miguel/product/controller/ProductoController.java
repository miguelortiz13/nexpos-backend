package miguel.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import miguel.product.dto.InventoryMovementDTO;
import miguel.product.dto.InventoryMovementRequest;
import miguel.product.dto.ProductoDTO;
import miguel.product.model.Producto;
import miguel.product.service.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    // GET: Listar todos (público)
    @GetMapping
    public ResponseEntity<List<Producto>> listarProductos() {
        return ResponseEntity.ok(productoService.listarTodos());
    }

    // GET: Buscar por término (público)
    @GetMapping("/buscar")
    public ResponseEntity<List<Producto>> buscarProductos(
            @RequestParam(name = "q", required = false, defaultValue = "") String query) {
        return ResponseEntity.ok(productoService.buscarProductos(query));
    }

    // GET: Buscar por ID (público)
    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerProducto(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.buscarPorId(id));
    }

    // POST: Crear nuevo producto (solo ADMIN)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Producto> crearProducto(@Valid @RequestBody ProductoDTO productoDTO) {
        Producto nuevoProducto = productoService.crearProducto(productoDTO);
        return new ResponseEntity<>(nuevoProducto, HttpStatus.CREATED);
    }

    // PUT: Actualizar (solo ADMIN)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Producto> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoDTO productoDTO) {
        Producto productoActualizado = productoService.actualizarProducto(id, productoDTO);
        return ResponseEntity.ok(productoActualizado);
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
    public ResponseEntity<Producto> buscarPorCodigoBarras(@PathVariable String codigoBarras) {
        return productoService.buscarPorCodigoBarras(codigoBarras)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST: Registrar movimiento de inventario / Kardex (ENTRADA, SALIDA, AJUSTE)
    @PostMapping("/{id}/movimientos")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<InventoryMovementDTO> registrarMovimiento(
            @PathVariable Long id,
            @Valid @RequestBody InventoryMovementRequest request,
            Principal principal) {
        String username = principal != null ? principal.getName() : "ADMIN";
        InventoryMovementDTO movimiento = productoService.registrarMovimiento(id, request, username);
        return new ResponseEntity<>(movimiento, HttpStatus.CREATED);
    }

    // GET: Obtener historial Kardex de un producto
    @GetMapping("/{id}/movimientos")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<InventoryMovementDTO>> obtenerMovimientosPorProducto(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerMovimientosPorProducto(id));
    }

    // GET: Obtener movimientos recientes globales de inventario
    @GetMapping("/movimientos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InventoryMovementDTO>> obtenerMovimientosRecientes() {
        return ResponseEntity.ok(productoService.obtenerMovimientosRecientes());
    }

    // GET: Listar productos con stock crítico (bajo stock o agotados)
    @GetMapping("/bajo-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Producto>> obtenerProductosBajoStock() {
        return ResponseEntity.ok(productoService.obtenerProductosBajoStock());
    }
}