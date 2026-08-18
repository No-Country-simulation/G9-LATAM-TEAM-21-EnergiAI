"""
API REST — EnergIA, alineada al contrato REAL confirmado en el código del
frontend (rama qa/frontend-mvp-version-2.0).

Ubicación esperada: data-science/energia_mvp/api/main_v3.py
Carga el modelo desde ../models/ (carpeta hermana de api/)

IMPORTANTE: el endpoint de este servicio es /analisis-energetico. No
confundir con /analise-energetica, que es la ruta pública del backend
Java (AnalisisController.java) consumida por el frontend — esta API solo
la llama el backend internamente vía URL_MODELO.
"""
import json
import logging
import uuid
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from pathlib import Path

import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from openweather_client import generar_alerta_climatica
from recommendations_v3 import (
    generar_recomendaciones_base,
    calcular_consumo_especifico,
    calcular_costo_estimado_mensual,
)
from schemas_v3 import ConsumoEnergeticoRequest, ConsumoEnergeticoResponse, ErrorResponse

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("energia-api")

# CORREGIDO: main_v3.py está en api/, y models/ es carpeta HERMANA de api/
# (ambas dentro de energia_mvp/), por eso .parent.parent y no .parent
BASE_DIR = Path(__file__).resolve().parent.parent
MODEL_PATH = BASE_DIR / "models" / "modelo_eficiencia_energetica_v3.pkl"
LABEL_ENCODER_PATH = BASE_DIR / "models" / "label_encoder_v3.pkl"
METADATA_PATH = BASE_DIR / "models" / "model_metadata_v3.json"

FEATURES_NUM = [
    "consumo_kwh", "cantidad_equipos", "horas_alto_consumo", "superficie_m2",
    "habitantes_ocupantes", "factor_potencia", "porcentaje_iluminacion_led",
    "porcentaje_equipos_inteligentes", "antiguedad_promedio_ponderada",
    "capacidad_solar_kwp", "consumo_especifico_kwh_m2",
]
FEATURES_BOOL = ["uso_horario_pico", "tiene_paneles_solares"]
FEATURES_CAT = ["tipo_inmueble", "region_pais"]

_model = None
_label_encoder = None
_metadata = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _model, _label_encoder, _metadata
    if not MODEL_PATH.exists():
        raise RuntimeError(f"No se encontró el modelo en {MODEL_PATH}. Ejecuta scripts/train_v3.py primero.")
    _model = joblib.load(MODEL_PATH)
    _label_encoder = joblib.load(LABEL_ENCODER_PATH) if LABEL_ENCODER_PATH.exists() else None
    _metadata = json.loads(METADATA_PATH.read_text()) if METADATA_PATH.exists() else {}
    logger.info("Modelo cargado: %s (esquema %s)", _metadata.get("modelo"), _metadata.get("esquema"))
    yield
    logger.info("Apagando la API...")


app = FastAPI(
    title="EnergIA API",
    description="Clasificación de eficiencia energética — contrato real del frontend",
    version="3.1.0",
    lifespan=lifespan,
)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["GET", "POST"], allow_headers=["*"])


@app.get("/health", tags=["Sistema"])
def health():
    return {"status": "ok", "modelo_cargado": _model is not None}


@app.post(
    "/analisis-energetico",
    response_model=ConsumoEnergeticoResponse,
    responses={400: {"model": ErrorResponse}, 422: {"model": ErrorResponse}},
    tags=["Análisis"],
)
def analise_energetica(payload: ConsumoEnergeticoRequest):
    if _model is None:
        raise HTTPException(status_code=503, detail="El modelo aún no está disponible.")

    try:
        registro = payload.model_dump()
        tipo_inmueble = registro["tipo_inmueble"]
        tipo_inmueble = tipo_inmueble.value if hasattr(tipo_inmueble, "value") else tipo_inmueble
        region_pais = registro["region_pais"]
        region_pais = region_pais.value if hasattr(region_pais, "value") else region_pais

        consumo_especifico = calcular_consumo_especifico(registro["consumo_kwh"], registro["superficie_m2"])

        X_input = pd.DataFrame([{
            "consumo_kwh": registro["consumo_kwh"],
            "cantidad_equipos": registro["cantidad_equipos"],
            "horas_alto_consumo": registro["horas_alto_consumo"],
            "superficie_m2": registro["superficie_m2"],
            "habitantes_ocupantes": registro["habitantes_ocupantes"],
            "factor_potencia": registro["factor_potencia"],
            "porcentaje_iluminacion_led": registro["porcentaje_iluminacion_led"],
            "porcentaje_equipos_inteligentes": registro["porcentaje_equipos_inteligentes"],
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

    recomendaciones = generar_recomendaciones_base(registro)
    alerta = generar_alerta_climatica(registro["latitud"], registro["longitud"])
    if alerta:
        recomendaciones.append(alerta)

    return {
        "categoria": str(categoria),
        "probabilidad": round(probabilidad, 2),
        "recomendaciones": recomendaciones,
        "costo_estimado_mensual": calcular_costo_estimado_mensual(registro["consumo_kwh"]),
        "id_analisis": str(uuid.uuid4()),
        "fecha": datetime.now(timezone.utc).date().isoformat(),
    }


@app.exception_handler(Exception)
def manejador_errores_generico(request, exc):
    logger.exception("Error no controlado")
    return JSONResponse(status_code=500, content={"detail": "Error interno del servidor."})
