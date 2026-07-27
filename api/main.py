"""
API REST — MVP de Eficiencia Energética
Hackathon ONE — Alura + Oracle | Equipo G9 LATAM

Endpoints:
  POST /analisis-energetico   -> clasifica, recomienda y estima costo
  GET  /resultados/{id}       -> consulta un análisis previamente realizado
  GET  /health                -> healthcheck

El modelo se carga localmente desde ./models o, si las variables de entorno
de OCI están presentes, desde un bucket de OCI Object Storage (API S3-compatible).
"""

import json
import logging
import os
import sys
import uuid
from collections import Counter
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List

import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

sys.path.append(str(Path(__file__).resolve().parent.parent / "db"))
import database

from recommendations import generar_recomendaciones, estimar_costos_y_co2, calcular_consumo_especifico
from schemas import ConsumoEnergeticoRequest, ConsumoEnergeticoResponse, ErrorResponse

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("energia-api")

BASE_DIR = Path(__file__).resolve().parent.parent
MODEL_PATH = BASE_DIR / "models" / "modelo_eficiencia_energetica_extendido.pkl"
LABEL_ENCODER_PATH = BASE_DIR / "models" / "label_encoder_extendido.pkl"
METADATA_PATH = BASE_DIR / "models" / "model_metadata_extendido.json"

_model = None
_label_encoder = None
_metadata = None
_modelo_id = None


def _download_model_from_oci_if_configured():
    """
    Si están configuradas las variables de entorno de OCI Object Storage,
    descarga el modelo desde el bucket antes de cargarlo localmente.
    Usa el endpoint S3-compatible de OCI Object Storage.
    """
    bucket = os.getenv("OCI_BUCKET_NAME")
    if not bucket:
        return  # sin configuración OCI -> se usa el modelo local en ./models

    import boto3

    logger.info("Descargando modelo desde OCI Object Storage (bucket=%s)...", bucket)
    client = boto3.client(
        "s3",
        aws_access_key_id=os.environ["OCI_ACCESS_KEY_ID"],
        aws_secret_access_key=os.environ["OCI_SECRET_ACCESS_KEY"],
        endpoint_url=os.environ["OCI_ENDPOINT_URL"],
    )
    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    client.download_file(bucket, "models/modelo_eficiencia_energetica_extendido.pkl", str(MODEL_PATH))
    if os.getenv("OCI_HAS_LABEL_ENCODER", "true").lower() == "true":
        client.download_file(bucket, "models/label_encoder_extendido.pkl", str(LABEL_ENCODER_PATH))
    client.download_file(bucket, "models/model_metadata_extendido.json", str(METADATA_PATH))
    logger.info("Modelo descargado correctamente desde OCI Object Storage.")


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _model, _label_encoder, _metadata, _modelo_id

    try:
        _download_model_from_oci_if_configured()
    except Exception as exc:  # no bloquear el arranque si falla OCI; usar modelo local
        logger.warning("No se pudo descargar el modelo desde OCI (%s). Usando copia local.", exc)

    if not MODEL_PATH.exists():
        raise RuntimeError(
            f"No se encontró el modelo en {MODEL_PATH}. "
            "Ejecuta el notebook de entrenamiento o configura OCI Object Storage."
        )

    _model = joblib.load(MODEL_PATH)
    _label_encoder = joblib.load(LABEL_ENCODER_PATH) if LABEL_ENCODER_PATH.exists() else None
    _metadata = json.loads(METADATA_PATH.read_text()) if METADATA_PATH.exists() else {}
    logger.info("Modelo cargado: %s", _metadata.get("modelo", "desconocido"))

    _modelo_id = database.init_db(_metadata, str(MODEL_PATH))
    logger.info("Base de datos SQLite lista en %s (modelo_id=%s)", database.DB_PATH, _modelo_id)

    yield  # la app queda corriendo aquí

    logger.info("Apagando la API...")


app = FastAPI(
    title="API de Eficiencia Energética",
    description=(
        "Clasifica perfiles de consumo eléctrico residencial/comercial, "
        "genera recomendaciones de ahorro y estima el impacto financiero mensual."
    ),
    version="1.0.0",
    lifespan=lifespan,
)

# Permite que el dashboard (servido desde otro origen, ej. abierto como
# archivo local o desde el frontend del equipo) consuma esta API.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)


@app.get("/health", tags=["Sistema"])
def health():
    return {"status": "ok", "modelo_cargado": _model is not None}


@app.post(
    "/analisis-energetico",
    response_model=ConsumoEnergeticoResponse,
    responses={400: {"model": ErrorResponse}, 422: {"model": ErrorResponse}},
    tags=["Análisis"],
    summary="Analiza un perfil de consumo energético",
)
def analisis_energetico(payload: ConsumoEnergeticoRequest):
    if _model is None:
        raise HTTPException(status_code=503, detail="El modelo aún no está disponible.")

    try:
        registro = payload.model_dump()
        tipo_inmueble = registro["tipo_inmueble"]
        tipo_inmueble = tipo_inmueble.value if hasattr(tipo_inmueble, "value") else tipo_inmueble
        region_pais = registro["region_pais"]
        region_pais = region_pais.value if hasattr(region_pais, "value") else region_pais
        pais = registro["pais"]
        pais = pais.value if hasattr(pais, "value") else pais
        registro["tipo_inmueble"] = tipo_inmueble
        registro["region_pais"] = region_pais
        registro["pais"] = pais

        consumo_especifico = calcular_consumo_especifico(registro["consumo_kwh"], registro["superficie_m2"])

        X_input = pd.DataFrame([{
            "consumo_kwh": registro["consumo_kwh"],
            "cantidad_equipos": registro["cantidad_equipos"],
            "horas_alto_consumo": registro["horas_alto_consumo"],
            "superficie_m2": registro["superficie_m2"],
            "habitantes_ocupantes": registro["habitantes_ocupantes"],
            "factor_potencia": registro["factor_potencia"],
            "pct_iluminacion_led": registro["pct_iluminacion_led"],
            "pct_equipos_inteligentes": registro["pct_equipos_inteligentes"],
            "antiguedad_promedio_ponderada": registro["antiguedad_promedio_ponderada"],
            "capacidad_solar_kwp": registro["capacidad_solar_kwp"],
            "consumo_especifico_kwh_m2": consumo_especifico,
            "uso_horario_pico": int(registro["uso_horario_pico"]),
            "tiene_paneles_solares": int(registro["tiene_paneles_solares"]),
            "tipo_inmueble": tipo_inmueble,
            "region_pais": region_pais,
        }])

        if _label_encoder is not None:
            pred_idx = _model.predict(X_input)[0]
            categoria = _label_encoder.inverse_transform([pred_idx])[0]
        else:
            categoria = _model.predict(X_input)[0]
        probabilidad = float(_model.predict_proba(X_input)[0].max())

    except Exception as exc:
        logger.exception("Error al ejecutar la inferencia")
        raise HTTPException(status_code=400, detail=f"Error al procesar la solicitud: {exc}")

    costos = estimar_costos_y_co2(registro)

    resultado_id = str(uuid.uuid4())
    resultado = {
        "id": resultado_id,
        "categoria": str(categoria),
        "probabilidad": round(probabilidad, 2),
        "recomendaciones": generar_recomendaciones(registro),
        "consumo_especifico_kwh_m2": consumo_especifico,
        **costos,
    }
    database.insertar_analisis(registro, resultado, _modelo_id)
    return resultado


@app.get(
    "/estadisticas",
    tags=["Dashboard"],
    summary="Agregados en tiempo real para el dashboard en vivo",
)
def estadisticas(ultimos: int = 15):
    """
    Devuelve agregados sobre todos los análisis persistidos en SQLite:
    conteo por categoría, tipo de inmueble, país y región, costo y CO2
    acumulados, y los últimos N análisis para un feed en vivo.
    Sobrevive reinicios de la API (a diferencia del diccionario en memoria
    que se usaba antes).
    """
    return database.obtener_estadisticas(ultimos)


@app.get(
    "/resultados/{resultado_id}",
    response_model=ConsumoEnergeticoResponse,
    responses={404: {"model": ErrorResponse}},
    tags=["Análisis"],
    summary="Consulta un análisis previamente generado",
)
def obtener_resultado(resultado_id: str):
    resultado = database.obtener_analisis(resultado_id)
    if resultado is None:
        raise HTTPException(status_code=404, detail="No se encontró un análisis con ese id.")
    return resultado


@app.exception_handler(Exception)
def manejador_errores_generico(request, exc):
    logger.exception("Error no controlado")
    return JSONResponse(status_code=500, content={"detail": "Error interno del servidor."})
