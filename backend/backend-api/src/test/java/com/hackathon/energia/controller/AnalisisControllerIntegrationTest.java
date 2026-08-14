package com.hackathon.energia.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.energia.client.IaModelCliente;
import com.hackathon.energia.dto.DatosRegistroAnalisis;
import com.hackathon.energia.dto.DatosRespuestaAnalisis;
import com.hackathon.energia.dto.DatosRespuestaModeloIA;
import com.hackathon.energia.model.CategoriaEnergetica;
import com.hackathon.energia.service.AnalisisService;
import com.hackathon.energia.service.ClimateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AnalisisControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IaModelCliente iaModelCliente;

    @MockBean
    private ClimateService climateService;

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

    @Nested
    @DisplayName("POST /analise-energetica - Caso de éxito")
    class CasoExito {

        @Test
        @DisplayName("Payload válido → 201 CREATED con respuesta completa")
        void testPayloadValido() throws Exception {
            var prediccion = new DatosRespuestaModeloIA(
                    CategoriaEnergetica.INEFICIENTE,
                    0.81,
                    List.of("Reducir uso de equipos en horario pico")
            );
            when(iaModelCliente.obtenerPrediccion(any())).thenReturn(prediccion);
            when(climateService.obtenerAlertaTemperatura(any(), any())).thenReturn(Optional.empty());

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
            var prediccion = new DatosRespuestaModeloIA(
                    CategoriaEnergetica.MODERADO,
                    0.75,
                    List.of("Recomendación 1")
            );
            when(iaModelCliente.obtenerPrediccion(any())).thenReturn(prediccion);
            when(climateService.obtenerAlertaTemperatura(4.7110, -74.0721))
                    .thenReturn(Optional.of("Alerta de temperatura alta"));

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
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("JSON malformado"));
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
        @DisplayName("tipo_inmueble='Oficina' → 422 UNPROCESSABLE_ENTITY")
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
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.error").value("Valor de enum inválido"));
        }

        @Test
        @DisplayName("region_pais='AR-BuenosAires' → 422 UNPROCESSABLE_ENTITY")
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
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.error").value("Valor de enum inválido"));
        }
    }

    @Nested
    @DisplayName("POST /analise-energetica - Error del servicio IA")
    class ErrorServicioIA {

        @Test
        @DisplayName("Cuando IA falla → 503 SERVICE_UNAVAILABLE")
        void testErrorServicioIA() throws Exception {
            when(iaModelCliente.obtenerPrediccion(any()))
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
