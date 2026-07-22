package com.hackathon.energia.service;

import com.hackathon.energia.dto.ConsumoRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecomendacionService {

    public List<String> generar(ConsumoRequest req, String categoria) {
        List<String> recomendaciones = new ArrayList<>();

        if (Boolean.TRUE.equals(req.getUso_horario_pico())) {
            recomendaciones.add("Reducir el uso de equipos durante los horarios pico");
        }
        if (req.getHoras_alto_consumo() != null && req.getHoras_alto_consumo() >= 6) {
            recomendaciones.add("Distribuir las actividades de mayor consumo a lo largo del día");
        }
        if (req.getCantidad_equipos() != null && req.getCantidad_equipos() >= 8) {
            recomendaciones.add("Evaluar equipos con alto consumo energético y considerar reemplazo por modelos eficientes");
        }
        if ("Ineficiente".equalsIgnoreCase(categoria)) {
            recomendaciones.add("Realizar una auditoría energética para identificar los principales focos de desperdicio");
        } else if ("Moderado".equalsIgnoreCase(categoria)) {
            recomendaciones.add("Monitorear el consumo mensualmente para evitar pasar a un perfil ineficiente");
        }

        if (recomendaciones.isEmpty()) {
            recomendaciones.add("Tu perfil de consumo ya es eficiente, sigue así");
        }
        return recomendaciones;
    }
}
