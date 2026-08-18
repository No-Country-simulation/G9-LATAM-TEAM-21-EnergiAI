package com.hackathon.energia.repository;

import com.hackathon.energia.model.AnalisisEnergetico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnalisisEnergeticoRepository extends JpaRepository<AnalisisEnergetico, UUID> {
    List<AnalisisEnergetico> findByUsuarioIdOrderByFechaCreacionDesc(UUID usuarioId);
}