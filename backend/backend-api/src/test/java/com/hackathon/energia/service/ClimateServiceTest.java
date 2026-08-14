package com.hackathon.energia.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClimateServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private ClimateService climateService;

    @BeforeEach
    void setUp() {
        climateService = new ClimateService(restTemplate);
    }

    @Nested
    @DisplayName("Temperatura >= 32°C")
    class TemperaturaAlta {

        @Test
        @DisplayName("35°C → retorna alerta de temperatura")
        void testRetornarAlertaSiTemperaturaAlta() {
            ReflectionTestUtils.setField(climateService, "apiKey", "test-key");

            ClimateService.ClimateResponse mockResponse = new ClimateService.ClimateResponse();
            mockResponse.main = new ClimateService.Main();
            mockResponse.main.temp = 35.0;

            when(restTemplate.getForObject(anyString(), any(Class.class)))
                    .thenReturn(mockResponse);

            Optional<String> result = climateService.obtenerAlertaTemperatura(4.7110, -74.0721);

            assertTrue(result.isPresent());
            assertTrue(result.get().contains("35.0"));
            assertTrue(result.get().contains("32"));
            assertTrue(result.get().contains("10"));
        }

        @Test
        @DisplayName("32°C exactamente → retorna alerta (umbral inclusivo)")
        void testRetornarAlertaSiTemperaturaExacta32() {
            ReflectionTestUtils.setField(climateService, "apiKey", "test-key");

            ClimateService.ClimateResponse mockResponse = new ClimateService.ClimateResponse();
            mockResponse.main = new ClimateService.Main();
            mockResponse.main.temp = 32.0;

            when(restTemplate.getForObject(anyString(), any(Class.class)))
                    .thenReturn(mockResponse);

            Optional<String> result = climateService.obtenerAlertaTemperatura(4.7110, -74.0721);

            assertTrue(result.isPresent());
        }
    }

    @Nested
    @DisplayName("Temperatura < 32°C")
    class TemperaturaBaja {

        @Test
        @DisplayName("25°C → retorna Optional.empty()")
        void testRetornarEmptySiTemperaturaBaja() {
            ReflectionTestUtils.setField(climateService, "apiKey", "test-key");

            ClimateService.ClimateResponse mockResponse = new ClimateService.ClimateResponse();
            mockResponse.main = new ClimateService.Main();
            mockResponse.main.temp = 25.0;

            when(restTemplate.getForObject(anyString(), any(Class.class)))
                    .thenReturn(mockResponse);

            Optional<String> result = climateService.obtenerAlertaTemperatura(4.7110, -74.0721);

            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("Fallback gracioso")
    class FallbackGracioso {

        @Test
        @DisplayName("Si API falla → retorna Optional.empty() sin lanzar excepción")
        void testFallbackSiApiFalla() {
            ReflectionTestUtils.setField(climateService, "apiKey", "test-key");

            when(restTemplate.getForObject(anyString(), any(Class.class)))
                    .thenThrow(new RestClientException("Connection timeout"));

            Optional<String> result = climateService.obtenerAlertaTemperatura(4.7110, -74.0721);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Si API key no configurada → retorna Optional.empty()")
        void testEmptySiApiKeyNoConfigurada() {
            ReflectionTestUtils.setField(climateService, "apiKey", "");

            Optional<String> result = climateService.obtenerAlertaTemperatura(4.7110, -74.0721);

            assertFalse(result.isPresent());
            verify(restTemplate, never()).getForObject(anyString(), any());
        }

        @Test
        @DisplayName("Si lat/lon son null → retorna Optional.empty()")
        void testEmptySiLatLonNull() {
            ReflectionTestUtils.setField(climateService, "apiKey", "test-key");

            Optional<String> result = climateService.obtenerAlertaTemperatura(null, null);

            assertFalse(result.isPresent());
            verify(restTemplate, never()).getForObject(anyString(), any());
        }
    }
}
