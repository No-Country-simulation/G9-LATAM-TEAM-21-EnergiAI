package com.hackathon.energia.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("400 Bad Request")
    class BadRequest {

        @Test
        @DisplayName("MethodArgumentNotValidException → 400 con detalles de campos")
        void testHandleValidation() {
            var ex = mock(MethodArgumentNotValidException.class);
            var bindingResult = mock(org.springframework.validation.BindingResult.class);
            var fieldError = new org.springframework.validation.FieldError(
                    "objectName", "consumoKwh", "consumo_kwh es obligatorio"
            );
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));

            var response = handler.handleValidation(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            var body = response.getBody();
            assertNotNull(body);
            assertEquals(400, body.get("status"));
            assertEquals("Datos de entrada inválidos", body.get("error"));
        }

        @Test
        @DisplayName("HttpMessageNotReadableException → 400 JSON malformado")
        void testHandleMensajeNoLegible() {
            var ex = mock(HttpMessageNotReadableException.class);

            var response = handler.handleMensajeNoLegible(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            var body = response.getBody();
            assertNotNull(body);
            assertEquals(400, body.get("status"));
            assertEquals("JSON malformado", body.get("error"));
        }

        @Test
        @DisplayName("MissingServletRequestParameterException → 400 parámetro faltante")
        void testHandleParametroFaltante() {
            var ex = new MissingServletRequestParameterException("campo", "String");

            var response = handler.handleParametroFaltante(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            var body = response.getBody();
            assertNotNull(body);
            assertEquals(400, body.get("status"));
            assertEquals("Parámetro faltante", body.get("error"));
        }
    }

    @Nested
    @DisplayName("405 Method Not Allowed")
    class MethodNotAllowed {

        @Test
        @DisplayName("HttpRequestMethodNotSupportedException → 405")
        void testHandleMetodoNoSoportado() {
            var ex = new HttpRequestMethodNotSupportedException("DELETE", java.util.List.of("POST", "GET"));

            var response = handler.handleMetodoNoSoportado(ex);

            assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
            var body = response.getBody();
            assertNotNull(body);
            assertEquals(405, body.get("status"));
            assertEquals("Método no permitido", body.get("error"));
        }
    }

    @Nested
    @DisplayName("422 Unprocessable Entity")
    class UnprocessableEntity {

        @Test
        @DisplayName("InvalidEnumValueException → 422")
        void testHandleEnumInvalido() {
            var ex = new InvalidEnumValueException("Valor 'Oficina' no está en la lista de valores permitidos: Casa, Apartamento, Local");

            var response = handler.handleEnumInvalido(ex);

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
            var body = response.getBody();
            assertNotNull(body);
            assertEquals(422, body.get("status"));
            assertEquals("Valor de enum inválido", body.get("error"));
        }
    }

    @Nested
    @DisplayName("503 Service Unavailable")
    class ServiceUnavailable {

        @Test
        @DisplayName("IllegalStateException → 503")
        void testHandleServicioInferencia() {
            var ex = new IllegalStateException("Error al comunicarse con IA");

            var response = handler.handleServicioInferencia(ex);

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            var body = response.getBody();
            assertNotNull(body);
            assertEquals(503, body.get("status"));
            assertEquals("Servicio de inferencia no disponible", body.get("error"));
        }
    }

    @Nested
    @DisplayName("500 Internal Server Error")
    class InternalServerError {

        @Test
        @DisplayName("Exception genérica → 500")
        void testHandleGenerico() {
            var ex = new RuntimeException("Error inesperado");

            var response = handler.handleGenerico(ex);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            var body = response.getBody();
            assertNotNull(body);
            assertEquals(500, body.get("status"));
            assertEquals("Error interno", body.get("error"));
        }
    }
}
