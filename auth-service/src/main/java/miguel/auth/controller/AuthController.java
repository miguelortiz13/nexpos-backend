package miguel.auth.controller;

import miguel.auth.dto.AuthResponse;
import miguel.auth.dto.LoginRequest;
import miguel.auth.model.Usuario;
import miguel.auth.security.JwtProvider;
import miguel.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProvider jwtProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Usuario usuario) {
        try {
            usuario.setRole("USER"); // Registro público siempre crea USER
            Usuario saved = authService.register(usuario);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse response = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestParam String token) {
        if (jwtProvider.validateToken(token)) {
            return ResponseEntity.ok(Map.of("valid", true, "username", jwtProvider.getUsernameFromToken(token)));
        }
        return ResponseEntity.status(401).body(Map.of("valid", false, "message", "Token inválido"));
    }
}
