package com.hackathon.energia.dto;

import jakarta.validation.constraints.NotBlank;

public record DatosRefreshToken(
        @NotBlank
        String refreshToken
) {
}
