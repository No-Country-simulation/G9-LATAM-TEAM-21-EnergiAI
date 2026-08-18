package com.hackathon.energia.client;

import com.hackathon.energia.dto.DatosRegistroAnalisis;
import com.hackathon.energia.dto.DatosRespuestaModeloIA;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class IaModelCliente {
    private final RestTemplate restTemplate;

    public DatosRespuestaModeloIA obtenerPrediccion(DatosRegistroAnalisis datos) {
        try {
            // El modelo v3 (main_v3.py) espera los 15 campos crudos del payload,
            // sin transformar a vector one-hot: hace su propio feature engineering.
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<DatosRegistroAnalisis> request = new HttpEntity<>(datos, headers);

            String url = System.getenv("URL_MODELO");
            log.info("Enviando a IA: {}", url);

            ResponseEntity<DatosRespuestaModeloIA> response = restTemplate.postForEntity(url, request, DatosRespuestaModeloIA.class);
            log.info("Respuesta IA: {}", response.getStatusCode());

            return response.getBody();

        } catch (Exception e) {
            throw new IllegalStateException("Error al comunicarse con IA: " + e.getMessage(), e);
        }
    }
}
