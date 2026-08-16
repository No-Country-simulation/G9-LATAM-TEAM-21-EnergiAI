TARIFA_REFERENCIA_KWH = 0.75


def generar_recomendaciones_base(registro: dict) -> list:
    recs = []
    if registro.get("uso_horario_pico"):
        recs.append("Reducir el uso de equipos durante los horarios pico")
    if registro.get("porcentaje_iluminacion_led", 1) < 0.5:
        recs.append("Evaluar equipos con alto consumo energético: menos del 50% de la iluminación es LED")
    if registro.get("antiguedad_promedio_ponderada", 0) >= 10:
        recs.append("Considerar renovar equipos con más de 10 años de antigüedad promedio")
    if registro.get("factor_potencia", 1) < 0.85:
        recs.append("Revisar el factor de potencia: valores bajos indican pérdidas por energía reactiva")
    if registro.get("porcentaje_equipos_inteligentes", 1) < 0.2:
        recs.append("Evaluar termostatos o enchufes inteligentes para automatizar el ahorro")
    if not registro.get("tiene_paneles_solares") and registro.get("consumo_kwh", 0) >= 500:
        recs.append("Con este nivel de consumo, evaluar paneles solares podría tener buen retorno")
    if registro.get("horas_alto_consumo", 0) >= 6:
        recs.append("Distribuir las actividades de mayor consumo a lo largo del día")
    if not recs:
        recs.append("El perfil de consumo ya es eficiente; mantener los hábitos actuales")
    return recs


def calcular_consumo_especifico(consumo_kwh: float, superficie_m2: float) -> float:
    if not superficie_m2:
        return 0.0
    return round(consumo_kwh / superficie_m2, 2)


def calcular_costo_estimado_mensual(consumo_kwh: float) -> float:
    return round(consumo_kwh * TARIFA_REFERENCIA_KWH, 2)
