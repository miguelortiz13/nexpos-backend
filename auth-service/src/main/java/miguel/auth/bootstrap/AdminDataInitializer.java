package miguel.auth.bootstrap;

import miguel.auth.model.Usuario;
import miguel.auth.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // En local queda "admin"; en Azure llega desde Key Vault (ADMIN_PASSWORD).
    @Value("${app.admin.password:admin}")
    private String adminPassword;

    public AdminDataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (usuarioRepository.findByUsername("admin").isEmpty()) {
            Usuario admin = Usuario.builder()
                    .username("admin")
                    .password(passwordEncoder.encode(adminPassword))
                    .email("admin@marketcali.com")
                    .role("ADMIN")
                    .build();
            usuarioRepository.save(admin);
            System.out.println("Default admin user created: admin");
        }
    }
}
