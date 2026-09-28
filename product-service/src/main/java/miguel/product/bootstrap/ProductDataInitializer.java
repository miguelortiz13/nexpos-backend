package miguel.product.bootstrap;

import miguel.product.model.Producto;
import miguel.product.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ProductDataInitializer implements CommandLineRunner {

    private final ProductoRepository productoRepository;

    public ProductDataInitializer(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (productoRepository.count() == 0) {
            List<Producto> initialProducts = List.of(
                new Producto("Arroz Diana 1kg", "Diana", new BigDecimal("4500.00"), 50, "Granos y Cereales"),
                new Producto("Leche Alquería Entera 1L", "Alquería", new BigDecimal("4200.00"), 35, "Lácteos"),
                new Producto("Aceite Premier 900ml", "Premier", new BigDecimal("12500.00"), 20, "Aceites"),
                new Producto("Café Sello Rojo 500g", "Sello Rojo", new BigDecimal("16800.00"), 25, "Bebidas"),
                new Producto("Pan Tajado Bimbo 450g", "Bimbo", new BigDecimal("6800.00"), 18, "Panadería"),
                new Producto("Huevos AA x30", "Santa Anita", new BigDecimal("18500.00"), 15, "Huevos y Lácteos")
            );

            initialProducts.get(0).setCodigoBarras("7702001000011");
            initialProducts.get(0).setDescripcion("Arroz blanco fortificado primera calidad.");

            initialProducts.get(1).setCodigoBarras("7702001000028");
            initialProducts.get(1).setDescripcion("Leche entera ultrapasteurizada en bolsa.");

            initialProducts.get(2).setCodigoBarras("7702001000035");
            initialProducts.get(2).setDescripcion("Aceite vegetal comestible 100% puro.");

            initialProducts.get(3).setCodigoBarras("7702001000042");
            initialProducts.get(3).setDescripcion("Café tostado y molido sabor tradicional.");

            initialProducts.get(4).setCodigoBarras("7702001000059");
            initialProducts.get(4).setDescripcion("Pan blanco de molde enriquecido.");

            initialProducts.get(5).setCodigoBarras("7702001000066");
            initialProducts.get(5).setDescripcion("Cubeta de 30 huevos frescos seleccionados.");

            productoRepository.saveAll(initialProducts);
            System.out.println("Productos iniciales de prueba creados en MarketCali");
        }
    }
}
