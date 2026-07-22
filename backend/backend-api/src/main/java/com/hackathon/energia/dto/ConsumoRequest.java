package com.hackathon.energia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Datos de consumo energético de una vivienda o establecimiento")
public class ConsumoRequest {

    @Schema(description = "Consumo mensual en kWh", example = "420")
    @NotNull(message = "consumo_kwh es obligatorio")
    @Positive(message = "consumo_kwh debe ser mayor a 0")
    private Double consumo_kwh;

    @Schema(description = "Si hay uso significativo de equipos en horario pico", example = "true")
    @NotNull(message = "uso_horario_pico es obligatorio")
    private Boolean uso_horario_pico;

    @Schema(description = "Cantidad de equipos eléctricos en la vivienda", example = "10")
    @NotNull(message = "cantidad_equipos es obligatorio")
    @Min(value = 0, message = "cantidad_equipos no puede ser negativo")
    private Integer cantidad_equipos;

    @Schema(description = "Tipo de inmueble. Valores válidos: Apartamento, Casa, Casa Grande, Local Comercial (no distingue mayúsculas/minúsculas)", example = "Casa")
    @NotBlank(message = "tipo_inmueble es obligatorio")
    private String tipo_inmueble;

    @Schema(description = "Horas al día con consumo alto (0 a 24)", example = "8")
    @NotNull(message = "horas_alto_consumo es obligatorio")
    @Min(value = 0, message = "horas_alto_consumo debe estar entre 0 y 24")
    @Max(value = 24, message = "horas_alto_consumo debe estar entre 0 y 24")
    private Double horas_alto_consumo;

    public Double getConsumo_kwh() { return consumo_kwh; }
    public void setConsumo_kwh(Double consumo_kwh) { this.consumo_kwh = consumo_kwh; }

    public Boolean getUso_horario_pico() { return uso_horario_pico; }
    public void setUso_horario_pico(Boolean uso_horario_pico) { this.uso_horario_pico = uso_horario_pico; }

    public Integer getCantidad_equipos() { return cantidad_equipos; }
    public void setCantidad_equipos(Integer cantidad_equipos) { this.cantidad_equipos = cantidad_equipos; }

    public String getTipo_inmueble() { return tipo_inmueble; }
    public void setTipo_inmueble(String tipo_inmueble) { this.tipo_inmueble = tipo_inmueble; }

    public Double getHoras_alto_consumo() { return horas_alto_consumo; }
    public void setHoras_alto_consumo(Double horas_alto_consumo) { this.horas_alto_consumo = horas_alto_consumo; }
}
