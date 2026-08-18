import logging
import os
import time

import requests

logger = logging.getLogger("energia-api.weather")

OPENWEATHER_URL = "https://api.openweathermap.org/data/2.5/weather"
TIMEOUT_SEGUNDOS = 3
TTL_CACHE_SEGUNDOS = 20 * 60

UMBRAL_TEMPERATURA_CELSIUS = 32
RECARGO_ESTIMADO_PORCENTAJE = 10

_cache: dict = {}


def _clave_cache(lat: float, lon: float) -> tuple:
    return (round(lat, 2), round(lon, 2))


def obtener_temperatura_actual(lat: float, lon: float):
    clave = _clave_cache(lat, lon)
    ahora = time.time()
    if clave in _cache:
        timestamp, temp = _cache[clave]
        if ahora - timestamp < TTL_CACHE_SEGUNDOS:
            return temp

    api_key = os.getenv("OPENWEATHER_API_KEY")
    if not api_key:
        logger.warning("OPENWEATHER_API_KEY no configurada; se omite la alerta climática.")
        return None

    try:
        resp = requests.get(
            OPENWEATHER_URL,
            params={"lat": lat, "lon": lon, "units": "metric", "appid": api_key},
            timeout=TIMEOUT_SEGUNDOS,
        )
        resp.raise_for_status()
        temp = float(resp.json()["main"]["temp"])
        _cache[clave] = (ahora, temp)
        return temp
    except Exception as exc:
        logger.warning("No se pudo obtener clima (lat=%s, lon=%s): %s", lat, lon, exc)
        return None


def generar_alerta_climatica(lat: float, lon: float):
    temp = obtener_temperatura_actual(lat, lon)
    if temp is None:
        return None
    if temp >= UMBRAL_TEMPERATURA_CELSIUS:
        return (
            f"La temperatura actual es de {temp:.1f}°C, por encima del umbral de "
            f"{UMBRAL_TEMPERATURA_CELSIUS}°C: minimiza el uso de electrodomésticos de alto "
            f"consumo en este momento, podrías tener un incremento del "
            f"{RECARGO_ESTIMADO_PORCENTAJE}% en tu tarifa de consumo."
        )
    return None
