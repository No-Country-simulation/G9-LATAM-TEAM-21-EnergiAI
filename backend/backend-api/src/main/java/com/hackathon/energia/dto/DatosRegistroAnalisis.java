package com.hackathon.energia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Datos de consumo energético enviados para el análisis")
public record DatosRegistroAnalisis(

        @JsonProperty("consumo_kwh")
        @NotNull(message = "consumo_kwh es obligatorio")
        @Positive(message = "consumo_kwh debe ser mayor a 0")
        @Schema(description = "Consumo mensual en kWh", example = "420")
        Double consumoKwh,

        @JsonProperty("uso_horario_pico")
        @NotNull(message = "uso_horario_pico es obligatorio")
        @Schema(description = "Indica si hay uso significativo en horario pico", example = "true")
        Boolean usoHorarioPico,

        @JsonProperty("cantidad_equipos")
        @NotNull(message = "cantidad_equipos es obligatorio")
        @Positive(message = "cantidad_equipos debe ser mayor a 0")
        @Schema(description = "Cantidad de equipos eléctricos en el inmueble", example = "10")
        Integer cantidadEquipos,

        @JsonProperty("tipo_inmueble")
        @NotBlank(message = "tipo_inmueble es obligatorio")
        @Schema(description = "Tipo de inmueble", example = "Casa")
        String tipoInmueble,

        @JsonProperty("horas_alto_consumo")
        @NotNull(message = "horas_alto_consumo es obligatorio")
        @Min(value = 0, message = "horas_alto_consumo debe estar entre 0 y 24")
        @Max(value = 24, message = "horas_alto_consumo debe estar entre 0 y 24")
        @Schema(description = "Horas diarias de alto consumo", example = "8")
        Integer horasAltoConsumo
) {}