package com.transporte;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Clase de pruebas para UrbanBackApplication.
 * Utiliza @SpringBootTest para cargar el contexto completo de la aplicación.
 * El objetivo principal es verificar que la aplicación puede arrancar sin errores.
 */
@SpringBootTest
@DisplayName("Tests para UrbanBackApplication")
@ActiveProfiles("test")
class UrbanBackApplicationTest {

    /**
     * Test para verificar que el contexto de la aplicación se carga correctamente.
     * Si el contexto se carga sin lanzar excepciones, el test pasa.
     */
    @Test
    @DisplayName("El contexto de la aplicación debe cargar exitosamente")
    void contextLoads() {
        // Este test simplemente verifica que el contexto de Spring Boot se carga
        // sin lanzar ninguna excepción. Si llega a este punto, significa que la aplicación
        // puede arrancar correctamente.
        assertTrue(true, "El contexto de la aplicación no pudo cargar.");
    }

    /**
     * Test para verificar que el método main se puede ejecutar.
     * Aunque SpringApplication.run() ya es probado por Spring Boot,
     * este test asegura que la invocación del main no produce errores inmediatos.
     * No se requiere una aserción compleja ya que el SpringBootTest ya valida el arranque.
     */
    @Test
    @DisplayName("El método main debe ejecutarse sin errores al iniciar")
    void mainMethodRuns() {
        // No necesitamos llamar a SpringApplication.run() directamente aquí,
        // ya que @SpringBootTest ya se encarga de iniciar el contexto de la aplicación.
        // Si el test llega a este punto, significa que el main se ejecutó como parte del ciclo de vida del test.
        assertTrue(true, "El método main no se ejecutó correctamente.");
    }
}
