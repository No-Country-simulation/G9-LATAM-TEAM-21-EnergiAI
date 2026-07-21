"""
EnergiAI - Generador de dataset sintético calibrado
=====================================================
Genera un dataset con el esquema exacto pedido por el hackatón:
    consumo_kwh, uso_horario_pico, cantidad_equipos,
    tipo_inmueble, horas_alto_consumo  ->  categoria

Los rangos y relaciones entre variables están calibrados con
órdenes de magnitud de datasets reales de consumo residencial
(UCI "Power consumption of Tetouan city", UCI "Individual Household
Electric Power Consumption") y con los hallazgos del estudio de
225 hogares de Maharashtra, India (encuesta + consumo mensual),
que confirma que cantidad de electrodomésticos y tipo de vivienda
son predictores fuertes del consumo mensual.

No requiere conexión a internet: todo se genera localmente con
numpy siguiendo distribuciones y reglas calibradas manualmente.

Autoría original: equipo EnergiAI (compañero de equipo). Adaptado aquí
como función reutilizable `generar_dataset(n, seed)` para que el notebook
y las pruebas puedan invocarlo con distintos tamaños de muestra sin
duplicar el script.
"""

import numpy as np
import pandas as pd

RANDOM_SEED = 42
N_MUESTRAS = 100_000
TARIFA_REFERENCIA = 0.75  # USD/kWh, según el enunciado del hackatón

TIPOS_INMUEBLE = {
    # tipo: (consumo_base_kwh, dispersión, equipos_tipicos)
    "Apartamento":      (180, 40, (3, 10)),
    "Casa":             (280, 70, (5, 16)),
    "Casa Grande":      (420, 90, (8, 22)),
    "Local Comercial":  (550, 130, (6, 20)),
}


def generar_dataset(n: int = N_MUESTRAS, seed: int = RANDOM_SEED) -> pd.DataFrame:
    rng = np.random.default_rng(seed)

    # -----------------------------------------------------------
    # 1. Variables base (independientes)
    # -----------------------------------------------------------
    tipo_inmueble = rng.choice(list(TIPOS_INMUEBLE.keys()), size=n,
                                p=[0.35, 0.35, 0.15, 0.15])

    cantidad_equipos = np.array([
        rng.integers(TIPOS_INMUEBLE[t][2][0], TIPOS_INMUEBLE[t][2][1] + 1)
        for t in tipo_inmueble
    ])

    horas_alto_consumo = np.clip(
        rng.normal(loc=5, scale=2.5, size=n), 0, 16
    ).round().astype(int)

    uso_horario_pico = rng.choice([True, False], size=n, p=[0.4, 0.6])

    # -----------------------------------------------------------
    # 2. Consumo mensual (variable dependiente de las anteriores)
    # -----------------------------------------------------------
    base = np.array([TIPOS_INMUEBLE[t][0] for t in tipo_inmueble])
    dispersion = np.array([TIPOS_INMUEBLE[t][1] for t in tipo_inmueble])

    consumo_kwh = (
        base
        + cantidad_equipos * rng.normal(9, 2, size=n)
        + horas_alto_consumo * rng.normal(14, 3, size=n)
        + uso_horario_pico * rng.normal(45, 15, size=n)
        + rng.normal(0, dispersion, size=n)
    )
    consumo_kwh = np.clip(consumo_kwh, 40, None).round(1)

    # -----------------------------------------------------------
    # 3. Etiqueta (categoria) vía score de eficiencia + ruido
    # -----------------------------------------------------------
    consumo_por_equipo = consumo_kwh / cantidad_equipos
    score = (
        0.6 * (consumo_por_equipo - consumo_por_equipo.mean()) / consumo_por_equipo.std()
        + 0.25 * uso_horario_pico.astype(float)
        + 0.25 * (horas_alto_consumo - horas_alto_consumo.mean()) / horas_alto_consumo.std()
        + rng.normal(0, 0.35, size=n)
    )

    q1, q2 = np.quantile(score, [0.4, 0.75])
    categoria = np.select(
        [score <= q1, score <= q2],
        ["Eficiente", "Moderado"],
        default="Ineficiente",
    )

    # -----------------------------------------------------------
    # 4. Costo estimado mensual (determinístico, no es feature del modelo)
    # -----------------------------------------------------------
    costo_estimado_mensual = (consumo_kwh * TARIFA_REFERENCIA).round(2)

    # -----------------------------------------------------------
    # 5. Ensamblar
    # -----------------------------------------------------------
    df = pd.DataFrame({
        "consumo_kwh": consumo_kwh,
        "uso_horario_pico": uso_horario_pico,
        "cantidad_equipos": cantidad_equipos,
        "tipo_inmueble": tipo_inmueble,
        "horas_alto_consumo": horas_alto_consumo,
        "costo_estimado_mensual": costo_estimado_mensual,
        "categoria": categoria,
    })
    return df


if __name__ == "__main__":
    df = generar_dataset()
    out_path = "/home/claude/energia_mvp/data/consumo_energetico.csv"
    df.to_csv(out_path, index=False)
    print(df.head(10).to_string(index=False))
    print("\nDistribución de categorías:")
    print(df["categoria"].value_counts(normalize=True).round(3))
    print(f"\nDataset guardado en: {out_path}  ({len(df)} filas)")
