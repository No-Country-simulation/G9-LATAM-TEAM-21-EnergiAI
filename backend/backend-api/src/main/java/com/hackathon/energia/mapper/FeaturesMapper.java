package com.hackathon.energia.mapper;

import com.hackathon.energia.dto.DatosRegistroAnalisis;
import com.hackathon.energia.dto.DatosVectorFeaturesIA;
import org.springframework.stereotype.Component;

@Component
public class FeaturesMapper {

    public DatosVectorFeaturesIA aVectorFeatures(DatosRegistroAnalisis datos) {
        var consumoEspecifico = calcularConsumoEspecifico(datos.consumoKwh(), datos.superficieM2());

        return new DatosVectorFeaturesIA(
                datos.consumoKwh(),
                datos.cantidadEquipos(),
                datos.horasAltoConsumo(),
                datos.superficieM2(),
                datos.habitantesOcupantes(),
                datos.factorPotencia(),
                datos.porcentajeIluminacionLed(),
                datos.porcentajeEquiposInteligentes(),
                datos.antiguedadPromedioPonderada(),
                datos.capacidadSolarKwp(),
                consumoEspecifico,
                booleanAInt(datos.usoHorarioPico()),
                booleanAInt(datos.tienePanelesSolares()),
                oneHotTipoInmueble(datos.tipoInmueble(), "Casa"),
                oneHotTipoInmueble(datos.tipoInmueble(), "Apartamento"),
                oneHotTipoInmueble(datos.tipoInmueble(), "Local"),
                oneHotRegionPais(datos.regionPais(), "CO-Bogota"),
                oneHotRegionPais(datos.regionPais(), "CO-Medellin"),
                oneHotRegionPais(datos.regionPais(), "MX-CDMX"),
                oneHotRegionPais(datos.regionPais(), "BR-Brasilia")
        );
    }

    private Double calcularConsumoEspecifico(Double consumoKwh, Double superficieM2) {
        if (superficieM2 == null || superficieM2 <= 0) {
            return 0.0;
        }
        return consumoKwh / superficieM2;
    }

    private Integer booleanAInt(Boolean valor) {
        return valor != null && valor ? 1 : 0;
    }

    private Integer oneHotTipoInmueble(String tipoInmueble, String categoria) {
        if (tipoInmueble == null) return 0;
        return tipoInmueble.equals(categoria) ? 1 : 0;
    }

    private Integer oneHotRegionPais(String regionPais, String region) {
        if (regionPais == null) return 0;
        return regionPais.equals(region) ? 1 : 0;
    }
}
