package com.hackathon.energia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClimateService {

    private static final double UMBRAL_TEMPERATURA_CELSIUS = 32.0;
    private static final int RECARGO_ESTIMADO_PORCENTAJE = 10;

    private final RestTemplate restTemplate;

    @Value("${openweathermap.api.key:}")
    private String apiKey;

    public Optional<String> obtenerAlertaTemperatura(Double latitud, Double longitud) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenWeatherMap API key no configurada, omitiendo alerta climática");
            return Optional.empty();
        }

        if (latitud == null || longitud == null) {
            log.warn("Latitud/longitud no proporcionadas, omitiendo alerta climática");
            return Optional.empty();
        }

        try {
            String url = String.format(
                    "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&units=metric&appid=%s",
                    latitud, longitud, apiKey
            );

            var response = restTemplate.getForObject(url, ClimateResponse.class);

            if (response != null && response.main != null && response.main.temp != null) {
                double temperatura = response.main.temp;
                log.info("Temperatura actual: {}°C", temperatura);

                if (temperatura >= UMBRAL_TEMPERATURA_CELSIUS) {
                    String alerta = String.format(
                            Locale.US,
                            "La temperatura actual es de %.1f°C, por encima del umbral de %.0f°C: minimiza el uso de electrodomésticos de alto consumo en este momento, podrías tener un incremento del %d%% en tu tarifa de consumo.",
                            temperatura, UMBRAL_TEMPERATURA_CELSIUS, RECARGO_ESTIMADO_PORCENTAJE
                    );
                    return Optional.of(alerta);
                }
            }

        } catch (Exception e) {
            log.error("Error al consultar OpenWeatherMap, omitiendo alerta climática: {}", e.getMessage());
        }

        return Optional.empty();
    }

    static class ClimateResponse {
        public Main main;
    }

    static class Main {
        public Double temp;
    }
}
