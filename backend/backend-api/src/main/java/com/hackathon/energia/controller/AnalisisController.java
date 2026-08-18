package com.hackathon.energia.controller;

import com.hackathon.energia.dto.DatosHistorialAnalisis;
import com.hackathon.energia.dto.DatosRegistroAnalisis;
import com.hackathon.energia.dto.DatosRespuestaAnalisis;
import com.hackathon.energia.model.Usuario;
import com.hackathon.energia.service.AnalisisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analise-energetica")
@RequiredArgsConstructor
public class AnalisisController {

    private final AnalisisService analisisService;

    @PostMapping()
    public ResponseEntity<DatosRespuestaAnalisis> analisar(@RequestBody @Valid DatosRegistroAnalisis datosAnalisis,
                                                             @AuthenticationPrincipal Usuario usuario) {
       var resultado = analisisService.procesarAnalisis(datosAnalisis, usuario);
       return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @GetMapping("/historial")
    public ResponseEntity<List<DatosHistorialAnalisis>> historial(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(analisisService.obtenerHistorial(usuario));
    }

}
