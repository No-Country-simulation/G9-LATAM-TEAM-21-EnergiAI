package com.hackathon.energia.dto;

import com.hackathon.energia.model.CategoriaEnergetica;

import java.util.List;

public record DatosRespuestaModeloIA(
        CategoriaEnergetica categoria,
        Double probabilidad,
        List<String> recomendaciones
) {}
