package miguel.monolith.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Documentación Swagger / OpenAPI 3
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // Monitoreo y Salud Actuator
                        .requestMatchers("/actuator/**").permitAll()
                        // Autenticación pública
                        .requestMatchers("/auth/**").permitAll()
                        // Catálogo de productos y Kardex
                        .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/productos/*/movimientos").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasRole("ADMIN")
                        // Gestión de usuarios: exclusivo ADMIN
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        // Ventas y facturación: usuarios autenticados (cajeros y admin)
                        .requestMatchers("/api/sales/**").authenticated()
                        // Control de caja y turnos: usuarios autenticados (cajeros y admin)
                        .requestMatchers("/api/cash-shifts/**").authenticated()
                        // Clientes (directorio y búsqueda POS): usuarios autenticados (cajeros y admin)
                        .requestMatchers("/api/customers/**").authenticated()
                        // Configuración fiscal de empresa y DIAN / Factus: lectura autenticada, mutación ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/company-config/**").authenticated()
                        .requestMatchers("/api/company-config/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Orígenes permitidos separados por coma. En Azure se agrega la URL de la
    // Static Web App del frontend (variable CORS_ALLOWED_ORIGINS).
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost,http://127.0.0.1:5173}")
    private List<String> allowedOrigins;

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
