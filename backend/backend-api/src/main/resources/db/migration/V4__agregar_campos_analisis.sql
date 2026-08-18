ALTER TABLE analisis_energetico
    ADD COLUMN superficie_m2 DOUBLE,
    ADD COLUMN habitantes_ocupantes INT,
    ADD COLUMN factor_potencia DOUBLE,
    ADD COLUMN porcentaje_iluminacion_led DOUBLE,
    ADD COLUMN porcentaje_equipos_inteligentes DOUBLE,
    ADD COLUMN antiguedad_promedio_ponderada DOUBLE,
    ADD COLUMN capacidad_solar_kwp DOUBLE,
    ADD COLUMN tiene_paneles_solares BOOLEAN,
    ADD COLUMN region_pais VARCHAR(50),
    ADD COLUMN latitud DOUBLE,
    ADD COLUMN longitud DOUBLE;
