package com.hackathon.energia.controller;

import com.hackathon.energia.dto.DatosRegistroAnalisis;
import com.hackathon.energia.dto.DatosRespuestaAnalisis;
import com.hackathon.energia.service.AnalisisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analise-energetica")
@RequiredArgsConstructor
@Tag(name = "Análisis Energético", description = "Analiza el consumo y clasifica el perfil de eficiencia energética")
public class AnalisisController {

    private final AnalisisService analisisService;

    @PostMapping()
    @Operation(
            summary = "Analiza el consumo energético de una vivienda o local",
            description = "Recibe los datos de consumo y devuelve la categoría (EFICIENTE/MODERADO/INEFICIENTE), " +
                    "la probabilidad, recomendaciones de ahorro y el costo estimado mensual"

    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Análisis generado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    public ResponseEntity<DatosRespuestaAnalisis> analisar(@RequestBody @Valid DatosRegistroAnalisis datosAnalisis){
       var resultado = analisisService.procesarAnalisis(datosAnalisis);
       return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

}
