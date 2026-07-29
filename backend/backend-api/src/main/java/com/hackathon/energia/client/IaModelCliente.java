package com.hackathon.energia.client;

import com.hackathon.energia.dto.DatosRegistroAnalisis;
import com.hackathon.energia.dto.DatosRespuestaAnalisis;
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

    public DatosRespuestaAnalisis obtenerPrediccion(DatosRegistroAnalisis datos) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<DatosRegistroAnalisis> request = new HttpEntity<>(datos, headers);

            String url = System.getenv("URL_MODELO");
            log.info("Enviando a IA: {}", url);

            ResponseEntity<DatosRespuestaAnalisis> response = restTemplate.postForEntity(url, request, DatosRespuestaAnalisis.class);
            log.info("Respuesta IA: {}", response.getStatusCode());

            return response.getBody();

        } catch (Exception e) {
            throw new RuntimeException("Error al comunicarse con IA", e);
        }
    }
}
