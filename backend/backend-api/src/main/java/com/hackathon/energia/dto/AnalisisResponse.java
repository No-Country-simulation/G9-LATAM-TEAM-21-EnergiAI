package com.hackathon.energia.dto;

import java.util.List;

public class AnalisisResponse {
    private String id;
    private String categoria;
    private double probabilidad;
    private List<String> recomendaciones;
    private double costo_estimado_mensual;

    public AnalisisResponse() {}

    public AnalisisResponse(String id, String categoria, double probabilidad,
                             List<String> recomendaciones, double costoEstimadoMensual) {
        this.id = id;
        this.categoria = categoria;
        this.probabilidad = probabilidad;
        this.recomendaciones = recomendaciones;
        this.costo_estimado_mensual = costoEstimadoMensual;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public double getProbabilidad() { return probabilidad; }
    public void setProbabilidad(double probabilidad) { this.probabilidad = probabilidad; }

    public List<String> getRecomendaciones() { return recomendaciones; }
    public void setRecomendaciones(List<String> recomendaciones) { this.recomendaciones = recomendaciones; }

    public double getCosto_estimado_mensual() { return costo_estimado_mensual; }
    public void setCosto_estimado_mensual(double costoEstimadoMensual) { this.costo_estimado_mensual = costoEstimadoMensual; }
}
