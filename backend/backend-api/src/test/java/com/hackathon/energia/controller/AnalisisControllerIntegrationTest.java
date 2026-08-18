package com.hackathon.energia.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.energia.dto.DatosRegistroAnalisis;
import com.hackathon.energia.dto.DatosRespuestaAnalisis;
import com.hackathon.energia.exception.InvalidEnumValueException;
import com.hackathon.energia.model.CategoriaEnergetica;
import com.hackathon.energia.service.AnalisisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AnalisisController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {com.hackathon.energia.security.SecurityConfig.class, com.hackathon.energia.security.SecurityFilter.class}
        )
)
@Import(com.hackathon.energia.exception.GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalisisControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalisisService analisisService;

    private DatosRegistroAnalisis crearDatosValidos() {
        return new DatosRegistroAnalisis(
                420.0,
                10,
                8.0,
                80.0,
                4,
                0.92,
                0.60,
                0.30,
                6.5,
                0.0,
                true,
                false,
                "Casa",
                "CO-Bogota",
                4.7110,
                -74.0721
        );
    }

    private DatosRespuestaAnalisis crearRespuestaValida() {
        return new DatosRespuestaAnalisis(
                CategoriaEnergetica.INEFICIENTE,
                0.81,
                List.of("Reducir uso de equipos en horario pico"),
                new BigDecimal("315.00"),
                UUID.randomUUID(),
                LocalDate.now()
        );
    }

    @Nested
    @DisplayName("POST /analise-energetica - Caso de éxito")
    class CasoExito {

        @Test
        @DisplayName("Payload válido → 201 CREATED con respuesta completa")
        void testPayloadValido() throws Exception {
            when(analisisService.procesarAnalisis(any(), any())).thenReturn(crearRespuestaValida());

            var datos = crearDatosValidos();

            mockMvc.perform(post("/analise-energetica")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(datos)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.categoria").value("INEFICIENTE"))
                    .andExpect(jsonPath("$.probabilidad").value(0.81))
                    .andExpect(jsonPath("$.recomendaciones").isArray())
                    .andExpect(jsonPath("$.costo_estimado_mensual").value(315.00))
                    .andExpect(jsonPath("$.id_analisis").isNotEmpty())
                    .andExpect(jsonPath("$.fecha").isNotEmpty());
        }

        @Test
        @DisplayName("Con alerta climática → recomendaciones incluye alerta")
        void testConAlertaClimatica() throws Exception {
            var respuesta = new DatosRespuestaAnalisis(
                    CategoriaEnergetica.MODERADO,
                    0.75,
                    List.of("Recomendación 1", "Alerta de temperatura alta"),
                    new BigDecimal("315.00"),
                    UUID.randomUUID(),
                    LocalDate.now()
            );
            when(analisisService.procesarAnalisis(any(), any())).thenReturn(respuesta);

            var datos = crearDatosValidos();

            mockMvc.perform(post("/analise-energetica")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(datos)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.recomendaciones.length()").value(2))
                    .andExpect(jsonPath("$.recomendaciones[1]").value("Alerta de temperatura alta"));
        }
    }

    @Nested
    @DisplayName("POST /analise-energetica - Errores de validación")
    class ErroresValidacion {

        @Test
        @DisplayName("JSON malformado → 400 BAD_REQUEST")
        void testJsonMalformado() throws Exception {
            String jsonInvalido = "{ \"consumo_kwh\": 420, \"tipo_inmueble\": \"Casa\"";

            mockMvc.perform(post("/analise-energetica")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonInvalido))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Campo obligatorio faltante → 400 BAD_REQUEST")
        void testCampoObligatorioFaltante() throws Exception {
            var datos = new DatosRegistroAnalisis(
                    null,
                    10,
                    8.0,
                    80.0,
                    4,
                    0.92,
                    0.60,
                    0.30,
                    6.5,
                    0.0,
                    true,
                    false,
                    "Casa",
                    "CO-Bogota",
                    4.7110,
                    -74.0721
            );

            mockMvc.perform(post("/analise-energetica")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(datos)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Datos de entrada inválidos"))
                    .andExpect(jsonPath("$.detalles.consumoKwh").exists());
        }

        @Test
        @DisplayName("consumo_kwh negativo → 400 BAD_REQUEST")
        void testConsumoNegativo() throws Exception {
            var datos = new DatosRegistroAnalisis(
                    -100.0,
                    10,
                    8.0,
                    80.0,
                    4,
                    0.92,
                    0.60,
                    0.30,
                    6.5,
                    0.0,
                    true,
                    false,
                    "Casa",
                    "CO-Bogota",
                    4.7110,
                    -74.0721
            );

            mockMvc.perform(post("/analise-energetica")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(datos)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detalles.consumoKwh").exists());
        }
    }

    @Nested
    @DisplayName("POST /analise-energetica - Enum inválido")
    class EnumInvalido {

        @Test
        @DisplayName("tipo_inmueble='Oficina' → 400 BAD_REQUEST")
        void testTipoInmuebleInvalido() throws Exception {
            var datos = new DatosRegistroAnalisis(
                    420.0,
                    10,
                    8.0,
                    80.0,
                    4,
                    0.92,
                    0.60,
                    0.30,
                    6.5,
                    0.0,
                    true,
                    false,
                    "Oficina",
                    "CO-Bogota",
                    4.7110,
                    -74.0721
            );

            mockMvc.perform(post("/analise-energetica")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(datos)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Datos de entrada inválidos"));
        }

        @Test
        @DisplayName("region_pais='AR-BuenosAires' → 400 BAD_REQUEST")
        void testRegionPaisInvalido() throws Exception {
            var datos = new DatosRegistroAnalisis(
                    420.0,
                    10,
                    8.0,
                    80.0,
                    4,
                    0.92,
                    0.60,
                    0.30,
                    6.5,
                    0.0,
                    true,
                    false,
                    "Casa",
                    "AR-BuenosAires",
                    -34.6037,
                    -58.3816
            );

            mockMvc.perform(post("/analise-energetica")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(datos)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Datos de entrada inválidos"));
        }
    }

    @Nested
    @DisplayName("POST /analise-energetica - Error del servicio IA")
    class ErrorServicioIA {

        @Test
        @DisplayName("Cuando IA falla → 503 SERVICE_UNAVAILABLE")
        void testErrorServicioIA() throws Exception {
            when(analisisService.procesarAnalisis(any(), any()))
                    .thenThrow(new IllegalStateException("Error al comunicarse con IA"));

            var datos = crearDatosValidos();

            mockMvc.perform(post("/analise-energetica")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(datos)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value(503))
                    .andExpect(jsonPath("$.error").value("Servicio de inferencia no disponible"));
        }
    }
}
