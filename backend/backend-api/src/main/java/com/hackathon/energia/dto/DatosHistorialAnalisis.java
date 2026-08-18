package com.hackathon.energia.dto;

import com.hackathon.energia.model.CategoriaEnergetica;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DatosHistorialAnalisis(
        UUID idAnalisis,
        LocalDate fecha,
        Double consumoKwh,
        Double superficieM2,
        Double consumoEspecificoKwhM2,
        String tipoInmueble,
        String regionPais,
        CategoriaEnergetica categoria,
        BigDecimal costoEstimadoMensual
) {}
