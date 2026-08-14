package com.hackathon.energia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DatosVectorFeaturesIA(
        @JsonProperty("consumo_kwh") Double consumoKwh,
        @JsonProperty("cantidad_equipos") Integer cantidadEquipos,
        @JsonProperty("horas_alto_consumo") Double horasAltoConsumo,
        @JsonProperty("superficie_m2") Double superficieM2,
        @JsonProperty("habitantes_ocupantes") Integer habitantesOcupantes,
        @JsonProperty("factor_potencia") Double factorPotencia,
        @JsonProperty("porcentaje_iluminacion_led") Double porcentajeIluminacionLed,
        @JsonProperty("porcentaje_equipos_inteligentes") Double porcentajeEquiposInteligentes,
        @JsonProperty("antiguedad_promedio_ponderada") Double antiguedadPromedioPonderada,
        @JsonProperty("capacidad_solar_kwp") Double capacidadSolarKwp,
        @JsonProperty("consumo_especifico_kwh_m2") Double consumoEspecificoKwhM2,
        @JsonProperty("uso_horario_pico") Integer usoHorarioPico,
        @JsonProperty("tiene_paneles_solares") Integer tienePanelesSolares,
        @JsonProperty("tipo_inmueble_Casa") Integer tipoInmuebleCasa,
        @JsonProperty("tipo_inmueble_Apartamento") Integer tipoInmuebleApartamento,
        @JsonProperty("tipo_inmueble_Local") Integer tipoInmuebleLocal,
        @JsonProperty("region_pais_CO-Bogota") Integer regionPaisCOBogota,
        @JsonProperty("region_pais_CO-Medellin") Integer regionPaisCOMedellin,
        @JsonProperty("region_pais_MX-CDMX") Integer regionPaisMXCDMX,
        @JsonProperty("region_pais_BR-Brasilia") Integer regionPaisBRBrasilia
) {}
