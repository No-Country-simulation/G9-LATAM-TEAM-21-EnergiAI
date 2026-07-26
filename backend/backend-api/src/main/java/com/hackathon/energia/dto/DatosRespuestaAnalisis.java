package com.hackathon.energia.dto;

import com.hackathon.energia.model.CategoriaEnergetica;

import java.math.BigDecimal;
import java.util.List;

public record DatosRespuestaAnalisis(
        CategoriaEnergetica categoria,
        Double probabilidad,
        List<String> recomendaciones,
        BigDecimal costoEstimadoMensual
) {}
