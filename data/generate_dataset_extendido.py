"""
EnergiAI - Generador de dataset sintético EXTENDIDO
=====================================================
Basado en diccionario_datos_energiai2.xlsx.

Incluye:
  - Capa 1 (🟢 obligatorias del brief): consumo_kwh, uso_horario_pico,
    cantidad_equipos, tipo_inmueble, horas_alto_consumo
  - Capa 2 (alto impacto / bajo costo de implementación): superficie_m2,
    habitantes_ocupantes, pct_iluminacion_led, pct_equipos_inteligentes,
    antiguedad_promedio_ponderada, factor_potencia, tiene_paneles_solares,
    capacidad_solar_kwp, pais, region_pais, mes
  - Salidas informativas: consumo_especifico_kwh_m2, costo_referencia_brief
    (tarifa plana $0.75/kWh), costo_estimado_mensual (ponderado pico/valle
    y descuento por autoconsumo solar), co2_evitable_kg_mes

Las tarifas pico/valle y factores de emisión de CO2 por país son valores
ILUSTRATIVOS para fines de demo (no son cifras oficiales de ningún ente
regulador) — ver nota en README / observaciones legales del diccionario.

No requiere conexión a internet: todo se genera localmente con numpy.
"""

import numpy as np
import pandas as pd

RANDOM_SEED = 42
N_MUESTRAS = 100_000
TARIFA_REFERENCIA_BRIEF = 0.75  # USD/kWh, tarifa plana exigida por el brief

TIPOS_INMUEBLE = {
    # tipo: (consumo_base_kwh, dispersión, equipos_tipicos)
    "Apartamento":      (180, 40, (3, 10)),
    "Casa":             (280, 70, (5, 16)),
    "Casa Grande":      (420, 90, (8, 22)),
    "Local Comercial":  (550, 130, (6, 20)),
}

# Valores de referencia ilustrativos por país (tarifa pico/valle en USD/kWh
# y factor de emisión de CO2 en kg/kWh según mezcla eléctrica aproximada).
PAISES = {
    "Peru":      {"prob": 0.35, "tarifa_pico": 0.95, "tarifa_valle": 0.60, "factor_co2_kg_kwh": 0.31},
    "Colombia":  {"prob": 0.30, "tarifa_pico": 0.85, "tarifa_valle": 0.55, "factor_co2_kg_kwh": 0.16},
    "Chile":     {"prob": 0.20, "tarifa_pico": 1.05, "tarifa_valle": 0.70, "factor_co2_kg_kwh": 0.35},
    "México":    {"prob": 0.15, "tarifa_pico": 1.10, "tarifa_valle": 0.75, "factor_co2_kg_kwh": 0.42},
}

REGIONES = {
    "Costa":  1.10,   # más calor/humedad -> más climatización
    "Sierra": 0.90,   # clima más frío -> menos climatización
    "Selva":  1.20,   # calor + humedad alta -> mayor uso de AC/ventiladores
}

MESES = ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio",
         "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"]


def generar_dataset(n: int = N_MUESTRAS, seed: int = RANDOM_SEED, ruido_std: float = 0.35) -> pd.DataFrame:
    """
    ruido_std controla la desviación estándar del ruido gaussiano sumado al
    score de eficiencia antes de derivar `categoria` (ver sección 6 más abajo).
    Valor por defecto (0.35): dataset "realista", con incertidumbre deliberada
    que ninguna de las 13 variables explica. Valores más bajos (ej. 0.15)
    producen un dataset más "limpio" / fácil de separar, útil solo para
    comparar cómo cambia la accuracy — no se recomienda como dataset final.
    """
    rng = np.random.default_rng(seed)

    # -----------------------------------------------------------
    # 1. Variables geográficas y temporales
    # -----------------------------------------------------------
    pais = rng.choice(list(PAISES.keys()), size=n, p=[PAISES[p]["prob"] for p in PAISES])
    region_pais = rng.choice(list(REGIONES.keys()), size=n, p=[0.4, 0.35, 0.25])
    mes_idx = rng.integers(0, 12, size=n)
    mes = np.array(MESES)[mes_idx]
    # Estacionalidad simplificada: meses "calurosos" (índice 0,1,11 ~ verano
    # austral e índice 5,6,7 ~ verano boreal) elevan el consumo un poco.
    factor_estacional = 1 + 0.10 * np.sin((mes_idx / 12) * 2 * np.pi)

    # -----------------------------------------------------------
    # 2. Perfil del inmueble
    # -----------------------------------------------------------
    tipo_inmueble = rng.choice(list(TIPOS_INMUEBLE.keys()), size=n, p=[0.35, 0.35, 0.15, 0.15])
    cantidad_equipos = np.array([
        rng.integers(TIPOS_INMUEBLE[t][2][0], TIPOS_INMUEBLE[t][2][1] + 1)
        for t in tipo_inmueble
    ])
    superficie_m2 = np.clip(
        rng.normal(loc=[{"Apartamento": 70, "Casa": 120, "Casa Grande": 220,
                          "Local Comercial": 150}[t] for t in tipo_inmueble],
                   scale=25, size=n), 25, None
    ).round(0)
    habitantes_ocupantes = np.clip(rng.poisson(lam=3, size=n), 1, 10)

    # -----------------------------------------------------------
    # 3. Comportamiento de consumo (Capa 1, obligatorias del brief)
    # -----------------------------------------------------------
    horas_alto_consumo = np.clip(rng.normal(loc=5, scale=2.5, size=n), 0, 16).round().astype(int)
    uso_horario_pico = rng.choice([True, False], size=n, p=[0.4, 0.6])
    region_factor = np.array([REGIONES[r] for r in region_pais])

    base = np.array([TIPOS_INMUEBLE[t][0] for t in tipo_inmueble])
    dispersion = np.array([TIPOS_INMUEBLE[t][1] for t in tipo_inmueble])

    consumo_kwh = (
        base * region_factor * factor_estacional
        + cantidad_equipos * rng.normal(9, 2, size=n)
        + horas_alto_consumo * rng.normal(14, 3, size=n)
        + uso_horario_pico * rng.normal(45, 15, size=n)
        + habitantes_ocupantes * rng.normal(8, 3, size=n)
        + rng.normal(0, dispersion, size=n)
    )
    consumo_kwh = np.clip(consumo_kwh, 40, None).round(1)

    # -----------------------------------------------------------
    # 4. Señales de eficiencia (Capa 2)
    # -----------------------------------------------------------
    factor_potencia = np.clip(rng.normal(0.87, 0.06, size=n), 0.70, 0.98).round(2)
    pct_iluminacion_led = np.clip(rng.beta(2, 2, size=n), 0, 1).round(2)
    pct_equipos_inteligentes = np.clip(rng.beta(1.5, 4, size=n), 0, 1).round(2)
    antiguedad_promedio_ponderada = np.clip(rng.gamma(shape=2.2, scale=4.0, size=n), 0, 30).round(1)
    tiene_paneles_solares = rng.choice([True, False], size=n, p=[0.12, 0.88])
    capacidad_solar_kwp = np.where(
        tiene_paneles_solares,
        np.clip(rng.normal(4.5, 1.8, size=n), 1.0, 12.0).round(1),
        0.0,
    )

    # -----------------------------------------------------------
    # 5. Variables agregadas / calculadas
    # -----------------------------------------------------------
    consumo_especifico_kwh_m2 = (consumo_kwh / superficie_m2).round(2)

    # -----------------------------------------------------------
    # 6. Etiqueta (categoria) vía score de eficiencia + ruido
    # -----------------------------------------------------------
    def z(x):
        return (x - x.mean()) / x.std()

    score = (
        0.30 * z(consumo_especifico_kwh_m2)
        + 0.15 * uso_horario_pico.astype(float)
        + 0.15 * z(antiguedad_promedio_ponderada)
        + 0.10 * z(horas_alto_consumo)
        - 0.12 * z(pct_iluminacion_led)
        - 0.08 * z(pct_equipos_inteligentes)
        - 0.05 * z(factor_potencia)
        - 0.10 * tiene_paneles_solares.astype(float)
        + rng.normal(0, ruido_std, size=n)  # ruido para que no sea trivial (parametrizable)
    )

    q1, q2 = np.quantile(score, [0.40, 0.75])
    categoria = np.select(
        [score <= q1, score <= q2],
        ["Eficiente", "Moderado"],
        default="Ineficiente",
    )

    # -----------------------------------------------------------
    # 7. Costos y CO2 (salidas informativas, no features del modelo)
    # -----------------------------------------------------------
    tarifa_pico = np.array([PAISES[p]["tarifa_pico"] for p in pais])
    tarifa_valle = np.array([PAISES[p]["tarifa_valle"] for p in pais])
    factor_co2 = np.array([PAISES[p]["factor_co2_kg_kwh"] for p in pais])

    # Fracción del consumo que ocurre en horario punta (aproximación simple)
    frac_pico = np.where(uso_horario_pico, horas_alto_consumo / 24, horas_alto_consumo / 48)
    frac_pico = np.clip(frac_pico, 0, 0.8)

    # Autoconsumo solar: generación mensual aproximada (120 kWh por kWp
    # instalado es un promedio ilustrativo) con 60% de aprovechamiento directo.
    generacion_solar_kwh = capacidad_solar_kwp * 120 * 0.6
    consumo_neto_kwh = np.clip(consumo_kwh - generacion_solar_kwh, 0, None)

    costo_referencia_brief = (consumo_kwh * TARIFA_REFERENCIA_BRIEF).round(2)
    costo_estimado_mensual = (
        consumo_neto_kwh * (frac_pico * tarifa_pico + (1 - frac_pico) * tarifa_valle)
    ).round(2)

    # CO2 evitable estimado: potencial de ahorro si se aplican recomendaciones
    # típicas (~15% del consumo neto), convertido a kg de CO2 con el factor
    # de emisión del país. Cifra referencial, no oficial.
    co2_evitable_kg_mes = (consumo_neto_kwh * 0.15 * factor_co2).round(1)

    # -----------------------------------------------------------
    # 8. Ensamblar
    # -----------------------------------------------------------
    df = pd.DataFrame({
        "pais": pais,
        "region_pais": region_pais,
        "mes": mes,
        "tipo_inmueble": tipo_inmueble,
        "superficie_m2": superficie_m2,
        "habitantes_ocupantes": habitantes_ocupantes,
        "consumo_kwh": consumo_kwh,
        "uso_horario_pico": uso_horario_pico,
        "cantidad_equipos": cantidad_equipos,
        "horas_alto_consumo": horas_alto_consumo,
        "factor_potencia": factor_potencia,
        "pct_iluminacion_led": pct_iluminacion_led,
        "pct_equipos_inteligentes": pct_equipos_inteligentes,
        "antiguedad_promedio_ponderada": antiguedad_promedio_ponderada,
        "tiene_paneles_solares": tiene_paneles_solares,
        "capacidad_solar_kwp": capacidad_solar_kwp,
        "consumo_especifico_kwh_m2": consumo_especifico_kwh_m2,
        "tarifa_kwh_referencia": TARIFA_REFERENCIA_BRIEF,
        "tarifa_kwh_pico": tarifa_pico,
        "tarifa_kwh_valle": tarifa_valle,
        "costo_referencia_brief": costo_referencia_brief,
        "costo_estimado_mensual": costo_estimado_mensual,
        "co2_evitable_kg_mes": co2_evitable_kg_mes,
        "categoria": categoria,
    })
    return df


if __name__ == "__main__":
    df = generar_dataset()
    out_path = "/home/claude/energia_mvp/data/consumo_energetico_extendido.csv"
    df.to_csv(out_path, index=False)
    print(df.head(8).to_string(index=False))
    print("\nDistribución de categorías:")
    print(df["categoria"].value_counts(normalize=True).round(3))
    print(f"\nDataset guardado en: {out_path}  ({len(df)} filas, {len(df.columns)} columnas)")
