package com.hackathon.energia.dto;

import jakarta.validation.constraints.NotBlank;

public record DatosLoginUsuario(
        @NotBlank
        String login,

        @NotBlank
        String password
) {
}
