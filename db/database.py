"""
Capa de persistencia SQLite para la API de EnergiAI.

Implementa las mismas 4 tablas de db/schema.sql (tipos_inmueble, modelos,
analisis_energetico, recomendaciones), adaptadas a sintaxis SQLite y con
las columnas del esquema extendido (13 variables + costo dual + CO2).

SQLite se eligió para el demo local por ser un solo archivo sin servidor
que levantar; para producción real, ver las notas de portabilidad a OCI
Autonomous DB al final de db/schema.sql.
"""

import json
import sqlite3
import uuid
from contextlib import contextmanager
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

DB_PATH = Path(__file__).resolve().parent.parent / "energiai.db"

# Mismos valores que TIPOS_INMUEBLE en data/generate_dataset_extendido.py
TIPOS_INMUEBLE_SEED = [
    ("Apartamento", 180, 40, 3, 10, "Vivienda en edificio multifamiliar"),
    ("Casa", 280, 70, 5, 16, "Vivienda unifamiliar estándar"),
    ("Casa Grande", 420, 90, 8, 22, "Vivienda unifamiliar de mayor tamaño"),
    ("Local Comercial", 550, 130, 6, 20, "Pequeño establecimiento comercial u oficina"),
]

SCHEMA_SQLITE = """
CREATE TABLE IF NOT EXISTS tipos_inmueble (
    tipo_inmueble       TEXT PRIMARY KEY,
    consumo_base_kwh    REAL NOT NULL,
    dispersion          REAL NOT NULL,
    equipos_min         INTEGER NOT NULL,
    equipos_max         INTEGER NOT NULL,
    descripcion         TEXT
);

CREATE TABLE IF NOT EXISTS modelos (
    id                          TEXT PRIMARY KEY,
    nombre                      TEXT NOT NULL,
    version                     TEXT NOT NULL,
    fecha_entrenamiento         TEXT NOT NULL,
    n_registros_entrenamiento   INTEGER NOT NULL,
    accuracy                    REAL,
    f1_macro                    REAL,
    ruta_artefacto              TEXT NOT NULL,
    tarifa_referencia_usd_kwh   REAL NOT NULL DEFAULT 0.75,
    activo                      INTEGER NOT NULL DEFAULT 0,
    UNIQUE (nombre, version)
);

CREATE TABLE IF NOT EXISTS analisis_energetico (
    id                              TEXT PRIMARY KEY,
    consumo_kwh                     REAL NOT NULL,
    uso_horario_pico                INTEGER NOT NULL,
    cantidad_equipos                INTEGER NOT NULL,
    tipo_inmueble                   TEXT NOT NULL REFERENCES tipos_inmueble(tipo_inmueble),
    horas_alto_consumo              INTEGER NOT NULL,
    superficie_m2                   REAL,
    habitantes_ocupantes            INTEGER,
    factor_potencia                 REAL,
    pct_iluminacion_led             REAL,
    pct_equipos_inteligentes        REAL,
    antiguedad_promedio_ponderada   REAL,
    tiene_paneles_solares           INTEGER,
    capacidad_solar_kwp             REAL,
    consumo_especifico_kwh_m2       REAL,
    pais                            TEXT,
    region_pais                     TEXT,
    categoria                       TEXT NOT NULL,
    probabilidad                    REAL NOT NULL,
    costo_referencia_brief          REAL NOT NULL,
    costo_estimado_mensual          REAL NOT NULL,
    co2_evitable_kg_mes             REAL NOT NULL,
    excedente_solar_kwh             REAL,
    ingreso_estimado_excedente_mensual REAL,
    modelo_id                       TEXT NOT NULL REFERENCES modelos(id),
    fecha_creacion                  TEXT NOT NULL,
    origen                          TEXT NOT NULL DEFAULT 'api'
);

CREATE INDEX IF NOT EXISTS idx_analisis_fecha ON analisis_energetico (fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_analisis_categoria ON analisis_energetico (categoria);
CREATE INDEX IF NOT EXISTS idx_analisis_pais ON analisis_energetico (pais);

CREATE TABLE IF NOT EXISTS recomendaciones (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    analisis_id   TEXT NOT NULL REFERENCES analisis_energetico(id) ON DELETE CASCADE,
    texto         TEXT NOT NULL,
    orden         INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_recomendaciones_analisis ON recomendaciones (analisis_id);
"""


@contextmanager
def get_conn():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    try:
        yield conn
        conn.commit()
    finally:
        conn.close()


def _migrar_columnas(conn):
    """Agrega columnas nuevas a bases de datos ya creadas con un esquema
    anterior, sin perder los datos existentes (SQLite no soporta
    'ADD COLUMN IF NOT EXISTS', así que se verifica primero)."""
    columnas_necesarias = {
        "excedente_solar_kwh": "REAL",
        "ingreso_estimado_excedente_mensual": "REAL",
    }
    existentes = {row["name"] for row in conn.execute("PRAGMA table_info(analisis_energetico)")}
    for col, tipo in columnas_necesarias.items():
        if col not in existentes:
            conn.execute(f"ALTER TABLE analisis_energetico ADD COLUMN {col} {tipo}")


def init_db(metadata: dict, model_path: str):
    """Crea las tablas si no existen, siembra tipos_inmueble, y registra
    (o reutiliza) la versión de modelo actualmente cargada como 'activa'."""
    with get_conn() as conn:
        conn.executescript(SCHEMA_SQLITE)
        _migrar_columnas(conn)

        conn.executemany(
            """INSERT OR IGNORE INTO tipos_inmueble
               (tipo_inmueble, consumo_base_kwh, dispersion, equipos_min, equipos_max, descripcion)
               VALUES (?, ?, ?, ?, ?, ?)""",
            TIPOS_INMUEBLE_SEED,
        )

        nombre = metadata.get("modelo", "desconocido")
        version = "1.0.0"
        existente = conn.execute(
            "SELECT id FROM modelos WHERE nombre = ? AND version = ?", (nombre, version)
        ).fetchone()

        if existente:
            modelo_id = existente["id"]
        else:
            modelo_id = str(uuid.uuid4())
            conn.execute(
                """INSERT INTO modelos
                   (id, nombre, version, fecha_entrenamiento, n_registros_entrenamiento,
                    accuracy, f1_macro, ruta_artefacto, tarifa_referencia_usd_kwh, activo)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)""",
                (
                    modelo_id, nombre, version, datetime.now(timezone.utc).isoformat(),
                    metadata.get("n_registros_entrenamiento", 0),
                    metadata.get("metricas_test", {}).get("accuracy"),
                    metadata.get("metricas_test", {}).get("f1_macro"),
                    model_path,
                    metadata.get("tarifa_referencia_usd_kwh", 0.75),
                ),
            )

        # Solo el modelo cargado en esta corrida queda marcado como activo
        conn.execute("UPDATE modelos SET activo = 0")
        conn.execute("UPDATE modelos SET activo = 1 WHERE id = ?", (modelo_id,))

    return modelo_id


def insertar_analisis(registro: dict, resultado: dict, modelo_id: str) -> None:
    with get_conn() as conn:
        conn.execute(
            """INSERT INTO analisis_energetico (
                id, consumo_kwh, uso_horario_pico, cantidad_equipos, tipo_inmueble,
                horas_alto_consumo, superficie_m2, habitantes_ocupantes, factor_potencia,
                pct_iluminacion_led, pct_equipos_inteligentes, antiguedad_promedio_ponderada,
                tiene_paneles_solares, capacidad_solar_kwp, consumo_especifico_kwh_m2,
                pais, region_pais, categoria, probabilidad, costo_referencia_brief,
                costo_estimado_mensual, co2_evitable_kg_mes, excedente_solar_kwh,
                ingreso_estimado_excedente_mensual, modelo_id, fecha_creacion, origen
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (
                resultado["id"], registro["consumo_kwh"], int(registro["uso_horario_pico"]),
                registro["cantidad_equipos"], registro["tipo_inmueble"], registro["horas_alto_consumo"],
                registro.get("superficie_m2"), registro.get("habitantes_ocupantes"),
                registro.get("factor_potencia"), registro.get("pct_iluminacion_led"),
                registro.get("pct_equipos_inteligentes"), registro.get("antiguedad_promedio_ponderada"),
                int(registro.get("tiene_paneles_solares", False)), registro.get("capacidad_solar_kwp"),
                resultado["consumo_especifico_kwh_m2"], registro.get("pais"), registro.get("region_pais"),
                resultado["categoria"], resultado["probabilidad"], resultado["costo_referencia_brief"],
                resultado["costo_estimado_mensual"], resultado["co2_evitable_kg_mes"],
                resultado["excedente_solar_kwh"], resultado["ingreso_estimado_excedente_mensual"],
                modelo_id, datetime.now(timezone.utc).isoformat(), "api",
            ),
        )
        conn.executemany(
            "INSERT INTO recomendaciones (analisis_id, texto, orden) VALUES (?, ?, ?)",
            [(resultado["id"], texto, i) for i, texto in enumerate(resultado["recomendaciones"])],
        )


def obtener_analisis(resultado_id: str) -> Optional[dict]:
    with get_conn() as conn:
        row = conn.execute(
            "SELECT * FROM analisis_energetico WHERE id = ?", (resultado_id,)
        ).fetchone()
        if row is None:
            return None
        recs = conn.execute(
            "SELECT texto FROM recomendaciones WHERE analisis_id = ? ORDER BY orden", (resultado_id,)
        ).fetchall()
        return {
            "id": row["id"],
            "categoria": row["categoria"],
            "probabilidad": row["probabilidad"],
            "recomendaciones": [r["texto"] for r in recs],
            "costo_referencia_brief": row["costo_referencia_brief"],
            "costo_estimado_mensual": row["costo_estimado_mensual"],
            "co2_evitable_kg_mes": row["co2_evitable_kg_mes"],
            "consumo_especifico_kwh_m2": row["consumo_especifico_kwh_m2"],
            "excedente_solar_kwh": row["excedente_solar_kwh"] or 0,
            "ingreso_estimado_excedente_mensual": row["ingreso_estimado_excedente_mensual"] or 0,
        }


def obtener_estadisticas(ultimos: int = 15) -> dict:
    with get_conn() as conn:
        total = conn.execute("SELECT COUNT(*) AS n FROM analisis_energetico").fetchone()["n"]

        por_categoria = {r["categoria"]: r["n"] for r in conn.execute(
            "SELECT categoria, COUNT(*) AS n FROM analisis_energetico GROUP BY categoria"
        )}
        por_tipo = {r["tipo_inmueble"]: r["n"] for r in conn.execute(
            "SELECT tipo_inmueble, COUNT(*) AS n FROM analisis_energetico GROUP BY tipo_inmueble"
        )}
        por_pais = {r["pais"]: r["n"] for r in conn.execute(
            "SELECT pais, COUNT(*) AS n FROM analisis_energetico WHERE pais IS NOT NULL GROUP BY pais"
        )}
        por_region = {r["region_pais"]: r["n"] for r in conn.execute(
            "SELECT region_pais, COUNT(*) AS n FROM analisis_energetico WHERE region_pais IS NOT NULL GROUP BY region_pais"
        )}
        energia_disponible_por_pais = {r["pais"]: round(r["kwh"], 1) for r in conn.execute(
            """SELECT pais, COALESCE(SUM(excedente_solar_kwh), 0) AS kwh
               FROM analisis_energetico WHERE pais IS NOT NULL GROUP BY pais"""
        )}

        agregados = conn.execute(
            """SELECT
                   COALESCE(SUM(costo_estimado_mensual), 0) AS costo_total,
                   COALESCE(SUM(co2_evitable_kg_mes), 0) AS co2_total,
                   COALESCE(SUM(ingreso_estimado_excedente_mensual), 0) AS ingreso_solar_total,
                   COALESCE(AVG(consumo_kwh), 0) AS consumo_prom
               FROM analisis_energetico"""
        ).fetchone()

        recientes = conn.execute(
            """SELECT id, categoria, tipo_inmueble, consumo_kwh, costo_estimado_mensual,
                      co2_evitable_kg_mes, pais, region_pais, fecha_creacion
               FROM analisis_energetico ORDER BY fecha_creacion DESC LIMIT ?""",
            (ultimos,),
        ).fetchall()

        return {
            "total_analizados": total,
            "conteo_por_categoria": {
                "Eficiente": por_categoria.get("Eficiente", 0),
                "Moderado": por_categoria.get("Moderado", 0),
                "Ineficiente": por_categoria.get("Ineficiente", 0),
            },
            "conteo_por_tipo_inmueble": por_tipo,
            "conteo_por_pais": por_pais,
            "conteo_por_region": por_region,
            "energia_disponible_por_pais_kwh": energia_disponible_por_pais,
            "costo_estimado_total_mensual": round(agregados["costo_total"], 2),
            "co2_evitable_total_kg_mes": round(agregados["co2_total"], 1),
            "ingreso_solar_total_mensual": round(agregados["ingreso_solar_total"], 2),
            "consumo_promedio_kwh": round(agregados["consumo_prom"], 1),
            "ultimos_analisis": [dict(r) for r in recientes],
        }
