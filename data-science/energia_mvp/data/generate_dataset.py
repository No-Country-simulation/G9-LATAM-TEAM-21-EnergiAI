"""
EnergIA - Generador de dataset alineado al contrato REAL del frontend
========================================================================
Corregido a partir de inspeccionar directamente el código del frontend
en la rama qa/frontend-mvp-version-2.0 (función recolectarDatosFormulario()
en frontend/index.html) — NO de los documentos .md, que tenían un formato
de region_pais distinto (guion bajo, 3 ciudades) al que el frontend
realmente usa (guion, 4 ciudades, incluye Medellín).

Fuente de verdad: frontend/index.html, líneas ~1370 (opciones del <select
id="region_pais">) y ~1840 (recolectarDatosFormulario()).
"""

import numpy as np
import pandas as pd

RANDOM_SEED = 42
N_MUESTRAS = 100_000

TIPOS_INMUEBLE = {
    # tipo: (consumo_base_kwh, dispersión, equipos_tipicos, superficie_tipica)
    "Apartamento": (180, 40, (3, 10), 70),
    "Casa":        (280, 70, (5, 16), 130),
    "Local":       (500, 120, (6, 20), 150),
}

# Formato y valores EXACTOS del <select id="region_pais"> del frontend real.
REGIONES_PAIS = {
    "CO-Bogota":   {"prob": 0.30, "factor_consumo": 0.95, "lat": 4.7110,   "lon": -74.0721},
    "CO-Medellin": {"prob": 0.25, "factor_consumo": 0.90, "lat": 6.2442,   "lon": -75.5812},
    "MX-CDMX":     {"prob": 0.25, "factor_consumo": 1.05, "lat": 19.4326,  "lon": -99.1332},
    "BR-Brasilia": {"prob": 0.20, "factor_consumo": 1.10, "lat": -15.7939, "lon": -47.8828},
}


def generar_dataset(n: int = N_MUESTRAS, seed: int = RANDOM_SEED, ruido_std: float = 0.35) -> pd.DataFrame:
    rng = np.random.default_rng(seed)

    tipo_inmueble = rng.choice(list(TIPOS_INMUEBLE.keys()), size=n, p=[0.40, 0.40, 0.20])
    region_pais = rng.choice(list(REGIONES_PAIS.keys()), size=n,
                              p=[REGIONES_PAIS[r]["prob"] for r in REGIONES_PAIS])

    cantidad_equipos = np.array([
        rng.integers(TIPOS_INMUEBLE[t][2][0], TIPOS_INMUEBLE[t][2][1] + 1) for t in tipo_inmueble
    ])
    superficie_m2 = np.clip(
        rng.normal(loc=[TIPOS_INMUEBLE[t][3] for t in tipo_inmueble], scale=25, size=n), 20, None
    ).round(0)
    habitantes_ocupantes = np.clip(rng.poisson(lam=3, size=n), 1, 10)
    horas_alto_consumo = np.clip(rng.normal(loc=5, scale=2.5, size=n), 0, 24).round(1)
    uso_horario_pico = rng.choice([True, False], size=n, p=[0.4, 0.6])

    factor_potencia = np.clip(rng.normal(0.87, 0.06, size=n), 0.55, 1.0).round(2)
    porcentaje_iluminacion_led = np.clip(rng.beta(2, 2, size=n), 0, 1).round(2)
    porcentaje_equipos_inteligentes = np.clip(rng.beta(1.5, 4, size=n), 0, 1).round(2)
    antiguedad_promedio_ponderada = np.clip(rng.gamma(shape=2.2, scale=4.0, size=n), 0, 45).round(1)

    tiene_paneles_solares = rng.choice([True, False], size=n, p=[0.12, 0.88])
    capacidad_solar_kwp = np.where(
        tiene_paneles_solares, np.clip(rng.normal(4.5, 1.8, size=n), 0.5, 12.0).round(1), 0.0
    )

    base = np.array([TIPOS_INMUEBLE[t][0] for t in tipo_inmueble])
    dispersion = np.array([TIPOS_INMUEBLE[t][1] for t in tipo_inmueble])
    factor_region = np.array([REGIONES_PAIS[r]["factor_consumo"] for r in region_pais])

    consumo_kwh = (
        base * factor_region
        + cantidad_equipos * rng.normal(9, 2, size=n)
        + horas_alto_consumo * rng.normal(14, 3, size=n)
        + uso_horario_pico * rng.normal(45, 15, size=n)
        + habitantes_ocupantes * rng.normal(8, 3, size=n)
        + rng.normal(0, dispersion, size=n)
    )
    consumo_kwh = np.clip(consumo_kwh, 40, None).round(1)
    consumo_especifico_kwh_m2 = (consumo_kwh / superficie_m2).round(2)

    def z(x):
        return (x - x.mean()) / x.std()

    score = (
        0.30 * z(consumo_especifico_kwh_m2)
        + 0.15 * uso_horario_pico.astype(float)
        + 0.15 * z(antiguedad_promedio_ponderada)
        + 0.08 * z(horas_alto_consumo)
        - 0.12 * z(porcentaje_iluminacion_led)
        - 0.08 * z(porcentaje_equipos_inteligentes)
        - 0.08 * z(factor_potencia)
        - 0.10 * tiene_paneles_solares.astype(float)
        + rng.normal(0, ruido_std, size=n)
    )
    q1, q2 = np.quantile(score, [0.40, 0.75])
    categoria = np.select([score <= q1, score <= q2], ["Eficiente", "Moderado"], default="Ineficiente")

    df = pd.DataFrame({
        "consumo_kwh": consumo_kwh,
        "cantidad_equipos": cantidad_equipos,
        "horas_alto_consumo": horas_alto_consumo,
        "superficie_m2": superficie_m2,
        "habitantes_ocupantes": habitantes_ocupantes,
        "factor_potencia": factor_potencia,
        "porcentaje_iluminacion_led": porcentaje_iluminacion_led,
        "porcentaje_equipos_inteligentes": porcentaje_equipos_inteligentes,
        "antiguedad_promedio_ponderada": antiguedad_promedio_ponderada,
        "capacidad_solar_kwp": capacidad_solar_kwp,
        "consumo_especifico_kwh_m2": consumo_especifico_kwh_m2,
        "uso_horario_pico": uso_horario_pico,
        "tiene_paneles_solares": tiene_paneles_solares,
        "tipo_inmueble": tipo_inmueble,
        "region_pais": region_pais,
        "categoria": categoria,
    })
    return df


if __name__ == "__main__":
    df = generar_dataset()
    df.to_csv("consumo_energetico_v3.csv", index=False)
    print(df.head(5).to_string(index=False))
    print("\nDistribución de categorías:\n", df["categoria"].value_counts(normalize=True).round(3))
    print("\nDistribución de ciudades:\n", df["region_pais"].value_counts(normalize=True).round(3))
    print(f"\n{len(df)} filas guardadas en consumo_energetico_v3.csv")
