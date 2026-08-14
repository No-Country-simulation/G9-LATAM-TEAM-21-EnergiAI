package com.hackathon.energia.mapper;

import com.hackathon.energia.dto.DatosRegistroAnalisis;
import com.hackathon.energia.dto.DatosVectorFeaturesIA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeaturesMapperTest {

    private FeaturesMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FeaturesMapper();
    }

    private DatosRegistroAnalisis crearDatosPrueba() {
        return new DatosRegistroAnalisis(
                420.0,
                10,
                8.0,
                80.0,
                4,
                0.92,
                0.60,
                0.30,
                6.5,
                0.0,
                true,
                false,
                "Casa",
                "CO-Bogota",
                4.7110,
                -74.0721
        );
    }

    @Nested
    @DisplayName("One-hot encoding tipo_inmueble")
    class OneHotTipoInmueble {

        @Test
        @DisplayName("Casa → tipoInmuebleCasa=1, Apartamento=0, Local=0")
        void testCasa() {
            var datos = crearDatosPrueba();
            var result = mapper.aVectorFeatures(datos);

            assertEquals(1, result.tipoInmuebleCasa());
            assertEquals(0, result.tipoInmuebleApartamento());
            assertEquals(0, result.tipoInmuebleLocal());
        }

        @Test
        @DisplayName("Apartamento → tipoInmuebleApartamento=1, Casa=0, Local=0")
        void testApartamento() {
            var datos = new DatosRegistroAnalisis(
                    420.0, 10, 8.0, 80.0, 4, 0.92, 0.60, 0.30, 6.5, 0.0,
                    true, false, "Apartamento", "CO-Bogota", 4.7110, -74.0721
            );
            var result = mapper.aVectorFeatures(datos);

            assertEquals(0, result.tipoInmuebleCasa());
            assertEquals(1, result.tipoInmuebleApartamento());
            assertEquals(0, result.tipoInmuebleLocal());
        }

        @Test
        @DisplayName("Local → tipoInmuebleLocal=1, Casa=0, Apartamento=0")
        void testLocal() {
            var datos = new DatosRegistroAnalisis(
                    420.0, 10, 8.0, 80.0, 4, 0.92, 0.60, 0.30, 6.5, 0.0,
                    true, false, "Local", "CO-Bogota", 4.7110, -74.0721
            );
            var result = mapper.aVectorFeatures(datos);

            assertEquals(0, result.tipoInmuebleCasa());
            assertEquals(0, result.tipoInmuebleApartamento());
            assertEquals(1, result.tipoInmuebleLocal());
        }
    }

    @Nested
    @DisplayName("One-hot encoding region_pais")
    class OneHotRegionPais {

        @Test
        @DisplayName("CO-Bogota → regionPaisCOBogota=1, otros=0")
        void testCOBogota() {
            var datos = crearDatosPrueba();
            var result = mapper.aVectorFeatures(datos);

            assertEquals(1, result.regionPaisCOBogota());
            assertEquals(0, result.regionPaisCOMedellin());
            assertEquals(0, result.regionPaisMXCDMX());
            assertEquals(0, result.regionPaisBRBrasilia());
        }

        @Test
        @DisplayName("MX-CDMX → regionPaisMXCDMX=1, otros=0")
        void testMXCDMX() {
            var datos = new DatosRegistroAnalisis(
                    420.0, 10, 8.0, 80.0, 4, 0.92, 0.60, 0.30, 6.5, 0.0,
                    true, false, "Casa", "MX-CDMX", 19.39, -99.30
            );
            var result = mapper.aVectorFeatures(datos);

            assertEquals(0, result.regionPaisCOBogota());
            assertEquals(0, result.regionPaisCOMedellin());
            assertEquals(1, result.regionPaisMXCDMX());
            assertEquals(0, result.regionPaisBRBrasilia());
        }

        @Test
        @DisplayName("BR-Brasilia → regionPaisBRBrasilia=1, otros=0")
        void testBRBrasilia() {
            var datos = new DatosRegistroAnalisis(
                    420.0, 10, 8.0, 80.0, 4, 0.92, 0.60, 0.30, 6.5, 0.0,
                    true, false, "Casa", "BR-Brasilia", -15.77, -49.04
            );
            var result = mapper.aVectorFeatures(datos);

            assertEquals(0, result.regionPaisCOBogota());
            assertEquals(0, result.regionPaisCOMedellin());
            assertEquals(0, result.regionPaisMXCDMX());
            assertEquals(1, result.regionPaisBRBrasilia());
        }
    }

    @Nested
    @DisplayName("consumo_especifico_kwh_m2")
    class ConsumoEspecifico {

        @Test
        @DisplayName("420 kWh / 80 m2 = 5.25")
        void testNormal() {
            var datos = crearDatosPrueba();
            var result = mapper.aVectorFeatures(datos);

            assertEquals(5.25, result.consumoEspecificoKwhM2(), 0.001);
        }

        @Test
        @DisplayName("Superficie 0 → consumoEspecifico = 0.0")
        void testSuperficieCero() {
            var datos = new DatosRegistroAnalisis(
                    420.0, 10, 8.0, 0.0, 4, 0.92, 0.60, 0.30, 6.5, 0.0,
                    true, false, "Casa", "CO-Bogota", 4.7110, -74.0721
            );
            var result = mapper.aVectorFeatures(datos);

            assertEquals(0.0, result.consumoEspecificoKwhM2());
        }
    }

    @Nested
    @DisplayName("Boolean a Integer")
    class BooleanAInt {

        @Test
        @DisplayName("uso_horario_pico=true → 1")
        void testUsoHorarioPicoTrue() {
            var datos = crearDatosPrueba();
            var result = mapper.aVectorFeatures(datos);

            assertEquals(1, result.usoHorarioPico());
        }

        @Test
        @DisplayName("tiene_paneles_solares=false → 0")
        void testTienePanelesFalse() {
            var datos = crearDatosPrueba();
            var result = mapper.aVectorFeatures(datos);

            assertEquals(0, result.tienePanelesSolares());
        }

        @Test
        @DisplayName("tiene_paneles_solares=true → 1")
        void testTienePanelesTrue() {
            var datos = new DatosRegistroAnalisis(
                    420.0, 10, 8.0, 80.0, 4, 0.92, 0.60, 0.30, 6.5, 5.0,
                    true, true, "Casa", "CO-Bogota", 4.7110, -74.0721
            );
            var result = mapper.aVectorFeatures(datos);

            assertEquals(1, result.tienePanelesSolares());
            assertEquals(5.0, result.capacidadSolarKwp());
        }
    }

    @Nested
    @DisplayName("Campos numéricos se preservan")
    class CamposNumericos {

        @Test
        @DisplayName("Todos los campos numéricos se mapean correctamente")
        void testCamposNumericos() {
            var datos = crearDatosPrueba();
            var result = mapper.aVectorFeatures(datos);

            assertEquals(420.0, result.consumoKwh());
            assertEquals(10, result.cantidadEquipos());
            assertEquals(8.0, result.horasAltoConsumo());
            assertEquals(80.0, result.superficieM2());
            assertEquals(4, result.habitantesOcupantes());
            assertEquals(0.92, result.factorPotencia());
            assertEquals(0.60, result.porcentajeIluminacionLed());
            assertEquals(0.30, result.porcentajeEquiposInteligentes());
            assertEquals(6.5, result.antiguedadPromedioPonderada());
            assertEquals(0.0, result.capacidadSolarKwp());
        }
    }
}
