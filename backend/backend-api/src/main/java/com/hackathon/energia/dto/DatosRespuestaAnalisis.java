package com.hackathon.energia.dto;

import java.math.BigDecimal;
import java.util.List;

public record DatosRespuestaAnalisis(
        String categoria,
        Double probabilidad,
        List<String> recomendaciones,
        BigDecimal costoEstimadoMensual
) {}
