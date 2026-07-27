"""
Motor de recomendaciones y cálculos financieros/ambientales — esquema
extendido (13 variables). Reemplaza la versión de 5 variables; la lógica
está portada 1:1 desde notebooks/analisis_consumo_energetico.ipynb
(secciones 9 y 10) para que API y notebook no diverjan.
"""

TARIFA_REFERENCIA_BRIEF_USD_KWH = 0.75

# Mismos valores ilustrativos usados en data/generate_dataset_extendido.py
# (no son cifras oficiales de ningún ente regulador). tarifa_venta_kwh es
# la tarifa a la que se acredita/paga el excedente exportado a la red
# (net metering / net billing) — en la práctica suele ser menor que la
# tarifa de compra; aquí se estima como 60% de la tarifa valle.
PAISES = {
    "Peru":     {"tarifa_pico": 0.95, "tarifa_valle": 0.60, "factor_co2_kg_kwh": 0.31},
    "Colombia": {"tarifa_pico": 0.85, "tarifa_valle": 0.55, "factor_co2_kg_kwh": 0.16},
    "Chile":    {"tarifa_pico": 1.05, "tarifa_valle": 0.70, "factor_co2_kg_kwh": 0.35},
    "México":   {"tarifa_pico": 1.10, "tarifa_valle": 0.75, "factor_co2_kg_kwh": 0.42},
}
FACTOR_TARIFA_VENTA = 0.6  # excedente se acredita al 60% de la tarifa valle


def _generacion_y_excedente_solar(registro: dict) -> tuple:
    """Devuelve (generacion_solar_kwh, excedente_solar_kwh) — reutilizado
    por generar_recomendaciones y estimar_costos_y_co2 para no duplicar
    la fórmula en dos lugares con distinto resultado."""
    consumo_kwh = registro.get("consumo_kwh", 0)
    capacidad_solar_kwp = registro.get("capacidad_solar_kwp", 0) or 0
    generacion_solar_kwh = capacidad_solar_kwp * 120 * 0.6  # promedio ilustrativo
    excedente_solar_kwh = max(generacion_solar_kwh - consumo_kwh, 0)
    return generacion_solar_kwh, excedente_solar_kwh


def generar_recomendaciones(registro: dict) -> list:
    """Recomendaciones basadas en las señales del esquema extendido."""
    recs = []

    if registro.get("uso_horario_pico"):
        recs.append("Reducir el uso de equipos durante los horarios pico")

    if registro.get("pct_iluminacion_led", 1) < 0.5:
        recs.append("Migrar a iluminación LED: menos del 50% del inmueble la usa actualmente")

    if registro.get("antiguedad_promedio_ponderada", 0) >= 10:
        recs.append("Considerar renovar equipos con más de 10 años de antigüedad promedio")

    if registro.get("factor_potencia", 1) < 0.85:
        recs.append("Revisar el factor de potencia: valores bajos indican pérdidas por energía reactiva")

    if registro.get("pct_equipos_inteligentes", 1) < 0.2:
        recs.append("Evaluar termostatos o enchufes inteligentes para automatizar el ahorro")

    if not registro.get("tiene_paneles_solares") and registro.get("consumo_kwh", 0) >= 500:
        recs.append("Con este nivel de consumo, evaluar paneles solares podría tener buen retorno")

    if registro.get("tiene_paneles_solares"):
        _, excedente = _generacion_y_excedente_solar(registro)
        if excedente > 0:
            recs.append(
                f"Tu sistema solar genera más de lo que consumes (~{excedente:.0f} kWh/mes de excedente): "
                "revisa con tu distribuidora la opción de venta/acreditación a la red (net metering)"
            )

    if registro.get("horas_alto_consumo", 0) >= 6:
        recs.append("Distribuir las actividades de mayor consumo a lo largo del día")

    if not recs:
        recs.append("El perfil de consumo ya es eficiente; mantener los hábitos actuales")

    return recs


def calcular_consumo_especifico(consumo_kwh: float, superficie_m2: float) -> float:
    if not superficie_m2:
        return 0.0
    return round(consumo_kwh / superficie_m2, 2)


def estimar_costos_y_co2(registro: dict) -> dict:
    """
    Devuelve costo_referencia_brief (tarifa plana $0.75/kWh, exigida por el
    brief), costo_estimado_mensual (ponderado pico/valle según el país, con
    descuento por autoconsumo solar) y co2_evitable_kg_mes — misma lógica
    que data/generate_dataset_extendido.py, para que las cifras que ve el
    usuario sean consistentes con las que se usaron al entrenar el modelo.
    """
    consumo_kwh = registro["consumo_kwh"]
    pais = registro.get("pais", "Peru")
    datos_pais = PAISES.get(pais, PAISES["Peru"])

    horas_alto_consumo = registro.get("horas_alto_consumo", 0)
    uso_horario_pico = registro.get("uso_horario_pico", False)
    frac_pico = (horas_alto_consumo / 24) if uso_horario_pico else (horas_alto_consumo / 48)
    frac_pico = max(0.0, min(frac_pico, 0.8))

    generacion_solar_kwh, excedente_solar_kwh = _generacion_y_excedente_solar(registro)
    consumo_neto_kwh = max(consumo_kwh - generacion_solar_kwh, 0)

    tarifa_venta = datos_pais["tarifa_valle"] * FACTOR_TARIFA_VENTA
    ingreso_estimado_excedente = round(excedente_solar_kwh * tarifa_venta, 2)

    costo_referencia_brief = round(consumo_kwh * TARIFA_REFERENCIA_BRIEF_USD_KWH, 2)
    costo_energia_neta = consumo_neto_kwh * (frac_pico * datos_pais["tarifa_pico"] + (1 - frac_pico) * datos_pais["tarifa_valle"])
    # El excedente exportado se descuenta del costo — si supera el consumo
    # facturable, el resultado puede quedar negativo (crédito neto a favor).
    costo_estimado_mensual = round(costo_energia_neta - ingreso_estimado_excedente, 2)

    co2_evitable_kg_mes = round(consumo_neto_kwh * 0.15 * datos_pais["factor_co2_kg_kwh"], 1)

    return {
        "costo_referencia_brief": costo_referencia_brief,
        "costo_estimado_mensual": costo_estimado_mensual,
        "co2_evitable_kg_mes": co2_evitable_kg_mes,
        "excedente_solar_kwh": round(excedente_solar_kwh, 1),
        "ingreso_estimado_excedente_mensual": ingreso_estimado_excedente,
    }
