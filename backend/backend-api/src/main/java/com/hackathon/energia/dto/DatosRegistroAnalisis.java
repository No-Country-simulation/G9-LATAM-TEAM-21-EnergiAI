package com.hackathon.energia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hackathon.energia.validation.EnumValid;
import jakarta.validation.constraints.*;

public record DatosRegistroAnalisis(

        @JsonProperty("consumo_kwh")
        @NotNull(message = "consumo_kwh es obligatorio")
        @Positive(message = "consumo_kwh debe ser mayor a 0")
        @DecimalMax(value = "100000", message = "consumo_kwh supera el máximo razonable")
        Double consumoKwh,

        @JsonProperty("cantidad_equipos")
        @NotNull(message = "cantidad_equipos es obligatorio")
        @PositiveOrZero(message = "cantidad_equipos no puede ser negativo")
        Integer cantidadEquipos,

        @JsonProperty("horas_alto_consumo")
        @NotNull(message = "horas_alto_consumo es obligatorio")
        @DecimalMin(value = "0", message = "horas_alto_consumo debe estar entre 0 y 24")
        @DecimalMax(value = "24", message = "horas_alto_consumo debe estar entre 0 y 24")
        Double horasAltoConsumo,

        @JsonProperty("superficie_m2")
        @NotNull(message = "superficie_m2 es obligatorio")
        @Positive(message = "superficie_m2 debe ser mayor a 0")
        Double superficieM2,

        @JsonProperty("habitantes_ocupantes")
        @NotNull(message = "habitantes_ocupantes es obligatorio")
        @Min(value = 1, message = "habitantes_ocupantes debe ser al menos 1")
        Integer habitantesOcupantes,

        @JsonProperty("factor_potencia")
        @NotNull(message = "factor_potencia es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "factor_potencia debe ser mayor a 0")
        @DecimalMax(value = "1", message = "factor_potencia no puede superar 1")
        Double factorPotencia,

        @JsonProperty("porcentaje_iluminacion_led")
        @NotNull(message = "porcentaje_iluminacion_led es obligatorio")
        @DecimalMin(value = "0", message = "porcentaje_iluminacion_led debe estar entre 0 y 1")
        @DecimalMax(value = "1", message = "porcentaje_iluminacion_led debe estar entre 0 y 1")
        Double porcentajeIluminacionLed,

        @JsonProperty("porcentaje_equipos_inteligentes")
        @NotNull(message = "porcentaje_equipos_inteligentes es obligatorio")
        @DecimalMin(value = "0", message = "porcentaje_equipos_inteligentes debe estar entre 0 y 1")
        @DecimalMax(value = "1", message = "porcentaje_equipos_inteligentes debe estar entre 0 y 1")
        Double porcentajeEquiposInteligentes,

        @JsonProperty("antiguedad_promedio_ponderada")
        @NotNull(message = "antiguedad_promedio_ponderada es obligatorio")
        @PositiveOrZero(message = "antiguedad_promedio_ponderada no puede ser negativo")
        @DecimalMax(value = "50", message = "antiguedad_promedio_ponderada supera el máximo razonable")
        Double antiguedadPromedioPonderada,

        @JsonProperty("capacidad_solar_kwp")
        @NotNull(message = "capacidad_solar_kwp es obligatorio")
        @PositiveOrZero(message = "capacidad_solar_kwp no puede ser negativo")
        Double capacidadSolarKwp,

        @JsonProperty("uso_horario_pico")
        @NotNull(message = "uso_horario_pico es obligatorio")
        Boolean usoHorarioPico,

        @JsonProperty("tiene_paneles_solares")
        @NotNull(message = "tiene_paneles_solares es obligatorio")
        Boolean tienePanelesSolares,

        @JsonProperty("tipo_inmueble")
        @NotBlank(message = "tipo_inmueble es obligatorio")
        @EnumValid(values = {"Casa", "Apartamento", "Local"}, message = "tipo_inmueble debe ser Casa, Apartamento o Local")
        String tipoInmueble,

        @JsonProperty("region_pais")
        @NotBlank(message = "region_pais es obligatorio")
        @EnumValid(values = {"CO-Bogota", "CO-Medellin", "MX-CDMX", "BR-Brasilia"}, message = "region_pais debe ser CO-Bogota, CO-Medellin, MX-CDMX o BR-Brasilia")
        String regionPais,

        @JsonProperty("latitud")
        @NotNull(message = "latitud es obligatorio")
        @DecimalMin(value = "-90", message = "latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "latitud debe estar entre -90 y 90")
        Double latitud,

        @JsonProperty("longitud")
        @NotNull(message = "longitud es obligatorio")
        @DecimalMin(value = "-180", message = "longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "longitud debe estar entre -180 y 180")
        Double longitud

) {
    @AssertTrue(message = "si tiene_paneles_solares es true, capacidad_solar_kwp debe ser mayor a 0; si es false, debe ser 0")
    public boolean isCapacidadSolarConsistente() {
        if (tienePanelesSolares == null || capacidadSolarKwp == null) {
            return true;
        }
        if (tienePanelesSolares) {
            return capacidadSolarKwp > 0;
        }
        return capacidadSolarKwp == 0;
    }
}