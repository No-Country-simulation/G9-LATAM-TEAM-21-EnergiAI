package com.hackathon.energia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DatosRegistroUsuario(
        @NotBlank
        @Size(min = 3, max =100)
        String login,

        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {
}
