package com.hackathon.energia.dto;

public record DatosTokenJWT (
        String accessToken,
        String refreshToken,
        String type
){
    public DatosTokenJWT(String access, String refresh){
        this(access, refresh, "Bearer");
    }
}
