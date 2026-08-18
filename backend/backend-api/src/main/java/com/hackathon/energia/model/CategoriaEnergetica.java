package com.hackathon.energia.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CategoriaEnergetica {
    EFICIENTE,
    MODERADO,
    INEFICIENTE;

    @JsonCreator
    public static CategoriaEnergetica fromString(String valor) {
        return CategoriaEnergetica.valueOf(valor.trim().toUpperCase());
    }
}

