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

    @Column(name = "superficie_m2")
    private Double superficieM2;

    @Column(name = "habitantes_ocupantes")
    private Integer habitantesOcupantes;

    @Column(name = "factor_potencia")
    private Double factorPotencia;

    @Column(name = "porcentaje_iluminacion_led")
    private Double porcentajeIluminacionLed;

    @Column(name = "porcentaje_equipos_inteligentes")
    private Double porcentajeEquiposInteligentes;

    @Column(name = "antiguedad_promedio_ponderada")
    private Double antiguedadPromedioPonderada;

    @Column(name = "capacidad_solar_kwp")
    private Double capacidadSolarKwp;

    @Column(name = "tiene_paneles_solares")
    private Boolean tienePanelesSolares;

    @Column(name = "region_pais")
    private String regionPais;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

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

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "usuario_id", updatable = false, length = 36)
    private UUID usuarioId;
}