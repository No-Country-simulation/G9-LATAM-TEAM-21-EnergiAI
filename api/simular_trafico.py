"""
Simulador de tráfico para la demo en vivo.
Envía análisis sintéticos a la API cada pocos segundos, para que el
dashboard (dashboard.html) se vea "vivo" durante la presentación a
los jurados sin depender de que alguien llene el formulario a mano.

Uso:
    python3 simular_trafico.py --url http://localhost:8000 --intervalo 3
"""
import argparse
import random
import sys
import time

import requests

TIPOS_INMUEBLE = ["Apartamento", "Casa", "Casa Grande", "Local Comercial"]
REGIONES = ["Costa", "Sierra", "Selva"]
PAISES = ["Peru", "Colombia", "Chile", "México"]


def generar_solicitud_aleatoria() -> dict:
    tipo = random.choice(TIPOS_INMUEBLE)
    tiene_solar = random.random() < 0.15
    return {
        "consumo_kwh": round(random.uniform(80, 1100), 1),
        "uso_horario_pico": random.random() < 0.45,
        "cantidad_equipos": random.randint(2, 22),
        "tipo_inmueble": tipo,
        "horas_alto_consumo": random.randint(0, 14),
        "superficie_m2": round(random.uniform(45, 280), 0),
        "habitantes_ocupantes": random.randint(1, 7),
        "factor_potencia": round(random.uniform(0.72, 0.97), 2),
        "pct_iluminacion_led": round(random.uniform(0.05, 0.95), 2),
        "pct_equipos_inteligentes": round(random.uniform(0, 0.6), 2),
        "antiguedad_promedio_ponderada": round(random.uniform(0, 22), 1),
        "tiene_paneles_solares": tiene_solar,
        "capacidad_solar_kwp": round(random.uniform(1, 8), 1) if tiene_solar else 0,
        "region_pais": random.choice(REGIONES),
        "pais": random.choice(PAISES),
    }


def main():
    parser = argparse.ArgumentParser(description="Simulador de tráfico para la demo")
    parser.add_argument("--url", default="http://localhost:8000", help="URL base de la API")
    parser.add_argument("--intervalo", type=float, default=3.0, help="Segundos entre solicitudes")
    parser.add_argument("--n", type=int, default=0, help="Cantidad de solicitudes (0 = infinito)")
    args = parser.parse_args()

    endpoint = f"{args.url}/analisis-energetico"
    enviados = 0

    print(f"Enviando tráfico simulado a {endpoint} cada {args.intervalo}s. Ctrl+C para detener.")
    try:
        while args.n == 0 or enviados < args.n:
            payload = generar_solicitud_aleatoria()
            try:
                resp = requests.post(endpoint, json=payload, timeout=5)
                resp.raise_for_status()
                data = resp.json()
                print(f"[{enviados+1}] {payload['tipo_inmueble']:16s} "
                      f"{payload['consumo_kwh']:7.1f} kWh -> {data['categoria']}")
            except Exception as exc:
                print(f"Error al enviar solicitud: {exc}", file=sys.stderr)

            enviados += 1
            time.sleep(args.intervalo)
    except KeyboardInterrupt:
        print(f"\nDetenido. Se enviaron {enviados} solicitudes.")


if __name__ == "__main__":
    main()
