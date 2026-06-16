# Habilidad de Backend: Arquitectura Spring Boot 4.x

Este documento define la guía técnica obligatoria para el desarrollo del backend basado en **Spring Boot 4.x**, **Spring Framework 7.x**, y **Jakarta EE 11** utilizando **Java 21/25**.

---

## 1. Reglas Arquitectónicas Críticas

1.  **Arquitectura Hexagonal (Clean Architecture):**
    *   **Domain:** Sin dependencias de Spring Boot ni Spring AI. Modela entidades y puertos de interfaces.
    *   **Application:** Casos de uso y lógica de negocio.
    *   **Infrastructure:** Adaptadores de REST, persistencia JPA, Redis y clientes de LLMs.

2.  **Lombok Prohibido en el Dominio:**
    *   Utiliza **Java Records** para modelar DTOs y objetos inmutables.
    *   Los constructores de inyección de dependencias deben ser explícitos o autogenerados nativamente, no mediante Lombok en las capas de dominio.

3.  **Virtual Threads Habilitados:**
    *   Habilitado nativamente vía configuración:
        ```yaml
        spring:
          threads:
            virtual:
              enabled: true
        ```

---

## 2. Convenciones de Código y Ejemplos

### A. Controlador REST con OpenAPI 3.1

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "API para gestión de productos")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Crear nuevo producto")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### B. Manejo de Excepciones RFC 7807 (ProblemDetail)

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problemDetail.setTitle("Producto No Encontrado");
        return problemDetail;
    }
}
```

### C. Cliente HTTP Declarativo (@HttpExchange)

```java
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;

@HttpExchange(url = "/api/products")
public interface ProductClient {

    @GetExchange("/{id}")
    ProductResponse getProduct(@PathVariable UUID id);
}
```

### D. Seguridad con Spring Security 7.x (Lambdas)

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll()
            )
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
```

### E. Tests de Integración con @MockitoBean (Spring Boot 4)

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService service;

    @MockitoBean
    private ProductRepository repository;

    @Test
    void shouldFindProductById() {
        UUID id = UUID.randomUUID();
        Product product = new Product("Sample Product");

        when(repository.findById(id)).thenReturn(Optional.of(product));

        ProductResponse response = service.findById(id);
        assertThat(response.name()).isEqualTo("Sample Product");
    }
}
```

---

## 3. Lista de Verificación (Constraints Checklist)

Antes de entregar cualquier código, verifica:
*   ✅ ¿Usas dependencias `jakarta.*` actualizadas a Jakarta EE 11?
*   ✅ ¿Evitas Lombok en capas de negocio usando Records e inyección manual por constructor?
*   ✅ ¿Utilizas la anotación `@MockitoBean` para Mockito en tests de Spring Boot?
*   ✅ ¿La observabilidad e instrumentación OTLP están configuradas?
*   ✅ ¿Se definen los esquemas de API usando anotaciones OpenAPI 3.1?
*   ✅ ¿Los Virtual Threads de Spring Boot 4 están habilitados?
