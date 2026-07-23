package com.hackathon.energia.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "analisis_energetico")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalisisEnergetico {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "consumo_kwh")
    private Double consumoKwh;

    @Column(name = "uso_horario_pico")
    private Boolean usoHorarioPico;

    @Column(name = "cantidad_equipos")
    private Integer cantidadEquipos;

    @Column(name = "tipo_inmueble")
    private String tipoInmueble;

    @Column(name = "horas_alto_consumo")
    private Integer horasAltoConsumo;

    @Column(name = "categoria")
    private String categoria;

    @Column(name = "probabilidad")
    private Double probabilidad;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recomendaciones", columnDefinition = "json")
    private List<String> recomendaciones;

    @Column(name = "costo_estimado_mensual", precision = 10, scale = 2)
    private BigDecimal costoEstimadoMensual;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;
}