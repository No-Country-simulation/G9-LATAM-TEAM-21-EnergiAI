package com.hackathon.energia.dto;

/** Mapea la respuesta de POST /predict del microservicio Python (ia-service). */
public class PrediccionInterna {
    private String categoria;
    private double probabilidad;

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public double getProbabilidad() { return probabilidad; }
    public void setProbabilidad(double probabilidad) { this.probabilidad = probabilidad; }
}
