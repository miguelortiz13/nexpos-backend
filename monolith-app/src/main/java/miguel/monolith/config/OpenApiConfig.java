package miguel.monolith.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("⚡ NexPOS - Retail & Point of Sale API")
                        .description("API RESTful para la plataforma de punto de venta (POS) y gestión comercial multirrubro NexPOS. " +
                                "Provee servicios de autenticación JWT (RBAC), control de inventario con búsqueda por código de barras, " +
                                "terminal de punto de venta (POS) transaccional con descuento atómico de existencias y emisión de facturas en PDF.")
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Miguel Ángel Ortiz Escobar")
                                .url("https://github.com/miguelortiz13")
                                .email("contacto@marketcali.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingresa el token JWT obtenido del endpoint /auth/login para autenticar las peticiones.")));
    }
}
