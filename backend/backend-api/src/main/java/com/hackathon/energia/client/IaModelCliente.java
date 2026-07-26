package com.hackathon.energia.client;

import com.hackathon.energia.dto.DatosRegistroAnalisis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
@RequiredArgsConstructor
public class IaModelCliente {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    public String obtenerPrediccion(DatosRegistroAnalisis datos) {
        try {
            String jsonBody = objectMapper.writeValueAsString(datos);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(System.getenv("URL_MODELO")))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.body();
        } catch (Exception e){
            throw new RuntimeException("Error al preparar la peticion hacia la IA)",e);
        }
    }
}
