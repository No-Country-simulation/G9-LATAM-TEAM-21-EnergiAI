package com.hackathon.energia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record DatosRegistroAnalisis(

        @JsonProperty("consumo_kwh")
        @NotNull(message = "consumo_kwh es obligatorio")
        @Positive(message = "consumo_kwh debe ser mayor a 0")
        Double consumoKwh,

        @JsonProperty("uso_horario_pico")
        @NotNull(message = "uso_horario_pico es obligatorio")
        Boolean usoHorarioPico,

        @JsonProperty("cantidad_equipos")
        @NotNull(message = "cantidad_equipos es obligatorio")
        @Positive(message = "cantidad_equipos debe ser mayor a 0")
        Integer cantidadEquipos,

        @JsonProperty("tipo_inmueble")
        @NotBlank(message = "tipo_inmueble es obligatorio")
        String tipoInmueble,

        @JsonProperty("horas_alto_consumo")
        @NotNull(message = "horas_alto_consumo es obligatorio")
        @Min(value = 0, message = "horas_alto_consumo debe estar entre 0 y 24")
        @Max(value = 24, message = "horas_alto_consumo debe estar entre 0 y 24")
        Integer horasAltoConsumo
) {}
