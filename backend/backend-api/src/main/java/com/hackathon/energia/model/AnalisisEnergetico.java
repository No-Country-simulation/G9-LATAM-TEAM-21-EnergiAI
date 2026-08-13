package com.hackathon.energia.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "analisis_energetico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalisisEnergetico {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(updatable = false, nullable = false, length = 36)
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
    private Double horasAltoConsumo;

    @Column(name = "categoria")
    @Enumerated(EnumType.STRING)
    private CategoriaEnergetica categoria;

    @Column(name = "probabilidad")
    private Double probabilidad;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recomendaciones", columnDefinition = "json")
    private List<String> recomendaciones;

    @Column(name = "costo_estimado_mensual", precision = 10, scale = 2)
    private BigDecimal costoEstimadoMensual;

    @CreationTimestamp
    @Column(name = "creacion", updatable = false,  nullable = false)
    private LocalDateTime fechaCreacion;
}