package com.hackathon.energia.service;

import com.hackathon.energia.dto.AnalisisResponse;
import com.hackathon.energia.dto.ConsumoRequest;
import com.hackathon.energia.dto.PrediccionInterna;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalisisEnergeticoService {

    private final PrediccionClient prediccionClient;
    private final RecomendacionService recomendacionService;
    private final double tarifaReferencia;

    // Historial en memoria para GET /analisis-energetico/{id}.
    // Para persistencia real, reemplazar por un repositorio con DB.
    private final Map<String, AnalisisResponse> historial = new ConcurrentHashMap<>();

    public AnalisisEnergeticoService(PrediccionClient prediccionClient,
                                      RecomendacionService recomendacionService,
                                      @Value("${tarifa.referencia.usd_kwh}") double tarifaReferencia) {
        this.prediccionClient = prediccionClient;
        this.recomendacionService = recomendacionService;
        this.tarifaReferencia = tarifaReferencia;
    }

    public AnalisisResponse analizar(ConsumoRequest req) {
        PrediccionInterna prediccion = prediccionClient.predecir(req);

        var recomendaciones = recomendacionService.generar(req, prediccion.getCategoria());
        double costoEstimado = Math.round(req.getConsumo_kwh() * tarifaReferencia * 100.0) / 100.0;

        String id = UUID.randomUUID().toString();
        AnalisisResponse response = new AnalisisResponse(
                id, prediccion.getCategoria(), prediccion.getProbabilidad(),
                recomendaciones, costoEstimado
        );

        historial.put(id, response);
        return response;
    }

    public AnalisisResponse obtenerPorId(String id) {
        return historial.get(id);
    }
}
