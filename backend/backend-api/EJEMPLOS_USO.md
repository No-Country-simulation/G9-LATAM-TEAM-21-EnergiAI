# Ejemplos de uso — POST /analisis-energetico

## Ejemplo 1 — Eficiente

```json
{
  "consumo_kwh": 90,
  "uso_horario_pico": false,
  "cantidad_equipos": 3,
  "tipo_inmueble": "Apartamento",
  "horas_alto_consumo": 1
}
```

## Ejemplo 2 — Moderado

```json
{
  "consumo_kwh": 250,
  "uso_horario_pico": true,
  "cantidad_equipos": 6,
  "tipo_inmueble": "Casa",
  "horas_alto_consumo": 4
}
```

## Ejemplo 3 — Ineficiente

```json
{
  "consumo_kwh": 620,
  "uso_horario_pico": true,
  "cantidad_equipos": 14,
  "tipo_inmueble": "Casa Grande",
  "horas_alto_consumo": 10
}
```

## Error de validación

```json
{
  "consumo_kwh": 200,
  "uso_horario_pico": false,
  "cantidad_equipos": 5,
  "tipo_inmueble": "Oficina",
  "horas_alto_consumo": 3
}
```

Devuelve 503 con un mensaje indicando los valores permitidos para
`tipo_inmueble`: Apartamento, Casa, Casa Grande, Local Comercial.
