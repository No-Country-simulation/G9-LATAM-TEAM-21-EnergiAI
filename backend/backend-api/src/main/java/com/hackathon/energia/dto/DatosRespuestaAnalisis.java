package com.hackathon.energia.dto;

import com.hackathon.energia.model.CategoriaEnergetica;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DatosRespuestaAnalisis(
        CategoriaEnergetica categoria,
        Double probabilidad,
        List<String> recomendaciones,
        @JsonProperty("costo_estimado_mensual") BigDecimal costoEstimadoMensual,
        @JsonProperty("id_analisis") UUID idAnalisis,
        LocalDate fecha
) {}
