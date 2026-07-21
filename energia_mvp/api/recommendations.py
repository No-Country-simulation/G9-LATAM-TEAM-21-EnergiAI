TARIFA_REFERENCIA_USD_KWH = 0.75

# Umbrales de referencia por tipo de inmueble (equipos "típicos" según el
# generador de datos EnergiAI). Se usan para juzgar si cantidad_equipos o
# consumo_kwh están por encima de lo esperable para ese tipo de inmueble.
RANGOS_EQUIPOS_TIPICOS = {
    "Apartamento": (3, 10),
    "Casa": (5, 16),
    "Casa Grande": (8, 22),
    "Local Comercial": (6, 20),
}


def generar_recomendaciones(registro: dict) -> list:
    recs = []

    consumo_kwh = registro.get("consumo_kwh", 0)
    cantidad_equipos = registro.get("cantidad_equipos", 1) or 1
    consumo_por_equipo = consumo_kwh / cantidad_equipos

    if registro.get("uso_horario_pico"):
        recs.append("Reducir el uso de equipos durante los horarios pico")

    tipo = registro.get("tipo_inmueble")
    rango = RANGOS_EQUIPOS_TIPICOS.get(tipo)
    if rango and cantidad_equipos > rango[1]:
        recs.append("Evaluar equipos con alto consumo energético: la cantidad de equipos supera lo típico para este tipo de inmueble")
    elif cantidad_equipos >= 15:
        recs.append("Evaluar equipos con alto consumo energético")

    if registro.get("horas_alto_consumo", 0) >= 6:
        recs.append("Distribuir las actividades de mayor consumo a lo largo del día")

    if consumo_por_equipo >= 45:
        recs.append("El consumo por equipo es elevado; considerar renovar o hacer mantenimiento a los equipos más antiguos")

    if consumo_kwh >= 600:
        recs.append("El consumo mensual total es alto; realizar una auditoría energética del inmueble")

    if not recs:
        recs.append("El perfil de consumo ya es eficiente; mantener los hábitos actuales")

    return recs


def estimar_costo(consumo_kwh: float, tarifa: float = TARIFA_REFERENCIA_USD_KWH) -> float:
    return round(consumo_kwh * tarifa, 2)
