package com.hackathon.energia.controller;

import com.hackathon.energia.dto.AnalisisResponse;
import com.hackathon.energia.dto.ConsumoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Análisis Energético", description = "Clasificación de eficiencia energética, recomendaciones y estimación de costo")
@RestController
@RequestMapping("/analisis-energetico")
public class AnalisisEnergeticoController {

    private final AnalisisEnergeticoService service;

    public AnalisisEnergeticoController(AnalisisEnergeticoService service) {
        this.service = service;
    }

    @Operation(
            summary = "Analiza el consumo energético de una vivienda o establecimiento",
            description = "Recibe los datos de consumo, clasifica el perfil (Eficiente/Moderado/Ineficiente) " +
                    "usando el modelo XGBoost, genera recomendaciones y estima el costo mensual."
    )
    @PostMapping
    public ResponseEntity<AnalisisResponse> analizar(@Valid @RequestBody ConsumoRequest request) {
        AnalisisResponse resultado = service.analizar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @Operation(
            summary = "Consulta un análisis previamente calculado por su id",
            description = "Devuelve 404 si el id no existe (por ejemplo, si el servicio se reinició, ya que el historial es en memoria)."
    )
    @GetMapping("/{id}")
    public ResponseEntity<AnalisisResponse> consultar(@PathVariable String id) {
        AnalisisResponse resultado = service.obtenerPorId(id);
        if (resultado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resultado);
    }
}
