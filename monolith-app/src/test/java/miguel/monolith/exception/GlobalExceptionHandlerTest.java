package miguel.monolith.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound_shouldReturn404() {
        EntityNotFoundException ex = new EntityNotFoundException("Producto no encontrado con ID: 99");
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleNotFound(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Producto no encontrado con ID: 99", response.getBody().get("message"));
    }

    @Test
    void handleDataIntegrity_withDuplicateEntry_shouldReturn409() {
        Throwable cause = new Throwable("Duplicate entry '770123456789' for key 'codigo_barras'");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate key", cause);
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleDataIntegrity(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("código de barras"));
    }

    @Test
    void handleDataIntegrity_withForeignKeyConstraint_shouldReturn409() {
        Throwable cause = new Throwable("Cannot delete or update a parent row: a foreign key constraint fails");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("FK error", cause);
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleDataIntegrity(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("datos vinculados"));
    }

    @Test
    void handleAccessDenied_shouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleAccessDenied(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("No tiene permisos suficientes"));
    }

    @Test
    void handleBadCredentials_shouldReturn401() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleBadCredentials(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("Credenciales incorrectas"));
    }

    @Test
    void handleBadRequest_shouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("Parámetro inválido");
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleBadRequest(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Parámetro inválido", response.getBody().get("message"));
    }

    @Test
    void handleGenericException_shouldReturn500() {
        Exception ex = new Exception("Error inesperado en el servidor");
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("error interno"));
    }
}
