package com.hackathon.energia.service;

import com.hackathon.energia.client.IaModelCliente;
import com.hackathon.energia.dto.DatosRegistroAnalisis;
import com.hackathon.energia.dto.DatosRespuestaModeloIA;
import com.hackathon.energia.model.AnalisisEnergetico;
import com.hackathon.energia.model.CategoriaEnergetica;
import com.hackathon.energia.repository.AnalisisEnergeticoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalisisServiceTest {

    @Mock
    private AnalisisEnergeticoRepository analisisEnergeticoRepository;

    @Mock
    private IaModelCliente iaModelCliente;

    @Mock
    private ClimateService climateService;

    private AnalisisService analisisService;

    private DatosRegistroAnalisis datosPrueba;

    @BeforeEach
    void setUp() {
        analisisService = new AnalisisService(analisisEnergeticoRepository, iaModelCliente, climateService);

        datosPrueba = new DatosRegistroAnalisis(
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
    @DisplayName("Cálculo de costo_estimado_mensual")
    class CalculoCosto {

        @Test
        @DisplayName("420 kWh * 0.75 = 315.00")
        void testCalcularCosto() {
            var prediccion = new DatosRespuestaModeloIA(
                    CategoriaEnergetica.INEFICIENTE, 0.81,
                    List.of("Recomendación 1")
            );
            when(iaModelCliente.obtenerPrediccion(datosPrueba)).thenReturn(prediccion);
            when(climateService.obtenerAlertaTemperatura(4.7110, -74.0721)).thenReturn(Optional.empty());

            var analisisGuardado = AnalisisEnergetico.builder()
                    .id(UUID.randomUUID())
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            when(analisisEnergeticoRepository.save(any())).thenReturn(analisisGuardado);

            var resultado = analisisService.procesarAnalisis(datosPrueba);

            assertEquals(new BigDecimal("315.00"), resultado.costoEstimadoMensual());
        }

        @Test
        @DisplayName("100 kWh * 0.75 = 75.00")
        void testCalcularCostoBajo() {
            var datos = new DatosRegistroAnalisis(
                    100.0, 5, 4.0, 60.0, 2, 0.85, 0.80, 0.50, 3.0, 0.0,
                    false, false, "Apartamento", "MX-CDMX", 19.39, -99.30
            );
            var prediccion = new DatosRespuestaModeloIA(
                    CategoriaEnergetica.EFICIENTE, 0.90,
                    List.of("Recomendación 1")
            );
            when(iaModelCliente.obtenerPrediccion(datos)).thenReturn(prediccion);
            when(climateService.obtenerAlertaTemperatura(19.39, -99.30)).thenReturn(Optional.empty());

            var analisisGuardado = AnalisisEnergetico.builder()
                    .id(UUID.randomUUID())
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            when(analisisEnergeticoRepository.save(any())).thenReturn(analisisGuardado);

            var resultado = analisisService.procesarAnalisis(datos);

            assertEquals(new BigDecimal("75.00"), resultado.costoEstimadoMensual());
        }
    }

    @Nested
    @DisplayName("Mapeo de campos DTO → Entity")
    class MapeoCampos {

        @Test
        @DisplayName("Los 16 campos del DTO se mapean al Entity")
        void testMapearTodosLosCampos() {
            var prediccion = new DatosRespuestaModeloIA(
                    CategoriaEnergetica.MODERADO, 0.75,
                    List.of("Recomendación 1")
            );
            when(iaModelCliente.obtenerPrediccion(datosPrueba)).thenReturn(prediccion);
            when(climateService.obtenerAlertaTemperatura(4.7110, -74.0721)).thenReturn(Optional.empty());

            var analisisGuardado = AnalisisEnergetico.builder()
                    .id(UUID.randomUUID())
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            when(analisisEnergeticoRepository.save(any())).thenReturn(analisisGuardado);

            analisisService.procesarAnalisis(datosPrueba);

            ArgumentCaptor<AnalisisEnergetico> captor = ArgumentCaptor.forClass(AnalisisEnergetico.class);
            verify(analisisEnergeticoRepository).save(captor.capture());
            var entity = captor.getValue();

            assertEquals(420.0, entity.getConsumoKwh());
            assertEquals(true, entity.getUsoHorarioPico());
            assertEquals(10, entity.getCantidadEquipos());
            assertEquals("Casa", entity.getTipoInmueble());
            assertEquals(8.0, entity.getHorasAltoConsumo());
            assertEquals(80.0, entity.getSuperficieM2());
            assertEquals(4, entity.getHabitantesOcupantes());
            assertEquals(0.92, entity.getFactorPotencia());
            assertEquals(0.60, entity.getPorcentajeIluminacionLed());
            assertEquals(0.30, entity.getPorcentajeEquiposInteligentes());
            assertEquals(6.5, entity.getAntiguedadPromedioPonderada());
            assertEquals(0.0, entity.getCapacidadSolarKwp());
            assertEquals(false, entity.getTienePanelesSolares());
            assertEquals("CO-Bogota", entity.getRegionPais());
            assertEquals(4.7110, entity.getLatitud());
            assertEquals(-74.0721, entity.getLongitud());
        }
    }

    @Nested
    @DisplayName("Alerta climática en recomendaciones")
    class AlertaClima {

        @Test
        @DisplayName("Si climateService retorna alerta, se agrega a recomendaciones")
        void testAgregarAlertaSiPresente() {
            var prediccion = new DatosRespuestaModeloIA(
                    CategoriaEnergetica.INEFICIENTE, 0.81,
                    List.of("Recomendación 1", "Recomendación 2")
            );
            when(iaModelCliente.obtenerPrediccion(datosPrueba)).thenReturn(prediccion);
            when(climateService.obtenerAlertaTemperatura(4.7110, -74.0721))
                    .thenReturn(Optional.of("Alerta de temperatura alta"));

            var analisisGuardado = AnalisisEnergetico.builder()
                    .id(UUID.randomUUID())
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            when(analisisEnergeticoRepository.save(any())).thenReturn(analisisGuardado);

            var resultado = analisisService.procesarAnalisis(datosPrueba);

            assertEquals(3, resultado.recomendaciones().size());
            assertTrue(resultado.recomendaciones().contains("Alerta de temperatura alta"));
        }

        @Test
        @DisplayName("Si climateService retorna empty, no se agrega alerta")
        void testNoAgregarAlertaSiAusente() {
            var prediccion = new DatosRespuestaModeloIA(
                    CategoriaEnergetica.EFICIENTE, 0.90,
                    List.of("Recomendación 1")
            );
            when(iaModelCliente.obtenerPrediccion(datosPrueba)).thenReturn(prediccion);
            when(climateService.obtenerAlertaTemperatura(4.7110, -74.0721)).thenReturn(Optional.empty());

            var analisisGuardado = AnalisisEnergetico.builder()
                    .id(UUID.randomUUID())
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            when(analisisEnergeticoRepository.save(any())).thenReturn(analisisGuardado);

            var resultado = analisisService.procesarAnalisis(datosPrueba);

            assertEquals(1, resultado.recomendaciones().size());
            assertEquals("Recomendación 1", resultado.recomendaciones().get(0));
        }
    }

    @Nested
    @DisplayName("Respuesta contiene id_analisis y fecha")
    class RespuestaIdFecha {

        @Test
        @DisplayName("La respuesta incluye id y fecha del análisis guardado")
        void testIdYFechaEnRespuesta() {
            UUID expectedId = UUID.randomUUID();
            LocalDateTime expectedFecha = LocalDateTime.of(2026, 8, 14, 10, 0, 0);

            var prediccion = new DatosRespuestaModeloIA(
                    CategoriaEnergetica.MODERADO, 0.75,
                    List.of("Recomendación 1")
            );
            when(iaModelCliente.obtenerPrediccion(datosPrueba)).thenReturn(prediccion);
            when(climateService.obtenerAlertaTemperatura(4.7110, -74.0721)).thenReturn(Optional.empty());

            var analisisGuardado = AnalisisEnergetico.builder()
                    .id(expectedId)
                    .fechaCreacion(expectedFecha)
                    .build();
            when(analisisEnergeticoRepository.save(any())).thenReturn(analisisGuardado);

            var resultado = analisisService.procesarAnalisis(datosPrueba);

            assertEquals(expectedId, resultado.idAnalisis());
            assertEquals(expectedFecha.toLocalDate(), resultado.fecha());
        }
    }
}
