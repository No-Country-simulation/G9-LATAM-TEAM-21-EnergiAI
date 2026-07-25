CREATE TABLE analisis_energetico(
    id                      VARCHAR(36),
    consumo_kwh             DECIMAL(8,2) NOT NULL,
    uso_horario_pico        BOOLEAN DEFAULT FALSE,
    cantidad_equipos        INT NOT NULL,
    tipo_inmueble           TEXT NOT NULL,
    horas_alto_consumo      DECIMAL(8,2) NOT NULL,
    categoria               ENUM('EFICIENTE', 'MODERADO', 'INEFICIENTE') NOT NULL,
    probabilidad            FLOAT NOT NULL,
    recomendaciones         JSON,
    costo_estimado_mensual  DECIMAL(8,2) NOT NULL,
    creacion                TIMESTAMP DEFAULT NOW(),

    PRIMARY KEY (id)
);