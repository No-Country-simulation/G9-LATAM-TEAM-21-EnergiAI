package com.hackathon.energia.dto;

import com.hackathon.energia.model.CategoriaEnergetica;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Resultado del análisis de eficiencia energética")
public record DatosRespuestaAnalisis(
        @Schema(description = "Categoría de eficiencia energética")
        CategoriaEnergetica categoria,

        @Schema(description = "Probabilidad asociada a la categoría", example = "0.81")
        Double probabilidad,

        @Schema(description = "Recomendaciones para reducir el consumo")
        List<String> recomendaciones,

        @Schema(description = "Costo estimado mensual en base a la tarifa de referencia", example = "315.00")
        BigDecimal costoEstimadoMensual
) {}