package com.hackathon.energia.service;

import com.hackathon.energia.client.IaModelCliente;
import com.hackathon.energia.dto.DatosHistorialAnalisis;
import com.hackathon.energia.dto.DatosRegistroAnalisis;
import com.hackathon.energia.dto.DatosRespuestaAnalisis;
import com.hackathon.energia.model.AnalisisEnergetico;
import com.hackathon.energia.model.Usuario;
import com.hackathon.energia.repository.AnalisisEnergeticoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalisisService {

    private static final BigDecimal TARIFA_REFERENCIA_KWH = new BigDecimal("0.75");

    private final AnalisisEnergeticoRepository analisisEnergeticoRepository;
    private final IaModelCliente iaModelCliente;
    private final ClimateService climateService;

    @Transactional
    public DatosRespuestaAnalisis procesarAnalisis(DatosRegistroAnalisis datos, Usuario usuario) {
       var prediccion = iaModelCliente.obtenerPrediccion(datos);

       var recomendaciones = new ArrayList<>(prediccion.recomendaciones());
       climateService.obtenerAlertaTemperatura(datos.latitud(), datos.longitud())
               .ifPresent(recomendaciones::add);

       var costoEstimado = BigDecimal.valueOf(datos.consumoKwh())
               .multiply(TARIFA_REFERENCIA_KWH)
               .setScale(2, RoundingMode.HALF_UP);

        var analisis = AnalisisEnergetico.builder()
                .consumoKwh(datos.consumoKwh())
                .usoHorarioPico(datos.usoHorarioPico())
                .cantidadEquipos(datos.cantidadEquipos())
                .tipoInmueble(datos.tipoInmueble())
                .horasAltoConsumo(datos.horasAltoConsumo())
                .superficieM2(datos.superficieM2())
                .habitantesOcupantes(datos.habitantesOcupantes())
                .factorPotencia(datos.factorPotencia())
                .porcentajeIluminacionLed(datos.porcentajeIluminacionLed())
                .porcentajeEquiposInteligentes(datos.porcentajeEquiposInteligentes())
                .antiguedadPromedioPonderada(datos.antiguedadPromedioPonderada())
                .capacidadSolarKwp(datos.capacidadSolarKwp())
                .tienePanelesSolares(datos.tienePanelesSolares())
                .regionPais(datos.regionPais())
                .latitud(datos.latitud())
                .longitud(datos.longitud())
                .categoria(prediccion.categoria())
                .probabilidad(prediccion.probabilidad())
                .recomendaciones(recomendaciones)
                .costoEstimadoMensual(costoEstimado)
                .usuarioId(usuario != null ? usuario.getId() : null)
                .build();

       // saveAndFlush (no save): con @UuidGenerator el INSERT se puede diferir al
       // commit de la transaccion, y necesitamos leer fechaCreacion (@CreationTimestamp)
       // ya poblada antes de construir la respuesta.
       var analisisGuardado = analisisEnergeticoRepository.saveAndFlush(analisis);

       return new DatosRespuestaAnalisis(
               prediccion.categoria(),
               prediccion.probabilidad(),
               recomendaciones,
               costoEstimado,
               analisisGuardado.getId(),
               analisisGuardado.getFechaCreacion().toLocalDate()
       );
    }

    public List<DatosHistorialAnalisis> obtenerHistorial(Usuario usuario) {
        return analisisEnergeticoRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuario.getId())
                .stream()
                .map(a -> new DatosHistorialAnalisis(
                        a.getId(),
                        a.getFechaCreacion().toLocalDate(),
                        a.getConsumoKwh(),
                        a.getSuperficieM2(),
                        (a.getSuperficieM2() != null && a.getSuperficieM2() > 0)
                                ? a.getConsumoKwh() / a.getSuperficieM2()
                                : 0.0,
                        a.getTipoInmueble(),
                        a.getRegionPais(),
                        a.getCategoria(),
                        a.getCostoEstimadoMensual()
                ))
                .toList();
    }
}
