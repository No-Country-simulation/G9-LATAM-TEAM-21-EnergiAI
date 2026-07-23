package com.hackathon.energia.service;

import com.hackathon.energia.dto.ConsumoRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class PrediccionClient {

    private final WebClient webClient;

    public PrediccionClient(@Value("${inferencia.service.url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public PrediccionInterna predecir(ConsumoRequest req) {
        try {
            return webClient.post()
                    .uri("/predict")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(PrediccionInterna.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "El servicio de inferencia respondió con error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo contactar al servicio de inferencia (ia-service). ¿Está corriendo?", e);
        }
    }
}
