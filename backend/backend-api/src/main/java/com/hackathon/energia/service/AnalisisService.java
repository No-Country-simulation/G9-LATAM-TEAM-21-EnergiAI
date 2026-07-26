package com.hackathon.energia.service;

import com.hackathon.energia.dto.DatosRegistroAnalisis;
import com.hackathon.energia.dto.DatosRespuestaAnalisis;
import com.hackathon.energia.repository.AnalisisEnergeticoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalisisService {

    private final AnalisisEnergeticoRepository analisisEnergeticoRepository;

    public DatosRespuestaAnalisis procesarAnalisis(DatosRegistroAnalisis datos) {

    }
}
