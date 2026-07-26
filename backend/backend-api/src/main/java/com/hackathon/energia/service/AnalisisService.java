package com.hackathon.energia.service;

import com.hackathon.energia.client.IaModelCliente;
import com.hackathon.energia.dto.DatosRegistroAnalisis;
import com.hackathon.energia.dto.DatosRespuestaAnalisis;
import com.hackathon.energia.model.AnalisisEnergetico;
import com.hackathon.energia.repository.AnalisisEnergeticoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalisisService {

    private final AnalisisEnergeticoRepository analisisEnergeticoRepository;
    private final IaModelCliente iaModelCliente;

    public DatosRespuestaAnalisis procesarAnalisis(DatosRegistroAnalisis datos) {
       DatosRespuestaAnalisis prediccion = iaModelCliente.obtenerPrediccion(datos);

       AnalisisEnergetico analisis = AnalisisEnergetico.builder()
               .consumoKwh(datos.consumoKwh())
               .usoHorarioPico(datos.usoHorarioPico())
               .cantidadEquipos(datos.cantidadEquipos())
               .tipoInmueble(datos.tipoInmueble())
               .horasAltoConsumo(datos.horasAltoConsumo())
               .categoria(prediccion.categoria())
               .probabilidad(prediccion.probabilidad())
               .recomendaciones(prediccion.recomendaciones())
               .costoEstimadoMensual(prediccion.costoEstimadoMensual())
               .build();

       analisisEnergeticoRepository.save(analisis);
       return prediccion;
    }
}
