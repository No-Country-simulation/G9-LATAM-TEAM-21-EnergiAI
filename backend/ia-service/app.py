"""
Microservicio de inferencia interno. Carga model.pkl + label_encoder.pkl
y expone POST /predict, consumido por backend-api.
"""
import json
import os

import joblib
import pandas as pd
import requests
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

MODEL_PATH = os.getenv("MODEL_PATH", "model.pkl")
ENCODER_PATH = os.getenv("ENCODER_PATH", "label_encoder.pkl")
METADATA_PATH = os.getenv("METADATA_PATH", "model_metadata.json")

MODEL_URL = os.getenv("MODEL_URL", "")
ENCODER_URL = os.getenv("ENCODER_URL", "")


def descargar_si_hay_url(url: str, destino: str):
    if not url:
        return
    resp = requests.get(url, timeout=60)
    resp.raise_for_status()
    with open(destino, "wb") as f:
        f.write(resp.content)


descargar_si_hay_url(MODEL_URL, MODEL_PATH)
descargar_si_hay_url(ENCODER_URL, ENCODER_PATH)

app = FastAPI(title="Servicio de Inferencia - Eficiencia Energetica")

model = None
label_encoder = None
metadata = {}

with open(METADATA_PATH, "r", encoding="utf-8") as f:
    metadata = json.load(f)

FEATURES_NUM = metadata["features_num"]
FEATURES_CAT = metadata["features_cat"]

try:
    label_encoder = joblib.load(ENCODER_PATH)
except FileNotFoundError:
    label_encoder = None

try:
    model = joblib.load(MODEL_PATH)
except FileNotFoundError:
    model = None

CATEGORIAS_TIPO_INMUEBLE = ["Apartamento", "Casa", "Casa Grande", "Local Comercial"]
_MAPA_TIPO_INMUEBLE = {c.strip().lower(): c for c in CATEGORIAS_TIPO_INMUEBLE}


def normalizar_tipo_inmueble(valor: str) -> str:
    clave = valor.strip().lower()
    if clave not in _MAPA_TIPO_INMUEBLE:
        raise HTTPException(
            status_code=422,
            detail=(
                f"tipo_inmueble inválido: '{valor}'. "
                f"Valores permitidos: {CATEGORIAS_TIPO_INMUEBLE}"
            ),
        )
    return _MAPA_TIPO_INMUEBLE[clave]


class PrediccionRequest(BaseModel):
    consumo_kwh: float = Field(..., gt=0)
    uso_horario_pico: bool
    cantidad_equipos: int = Field(..., ge=0)
    tipo_inmueble: str
    horas_alto_consumo: float = Field(..., ge=0, le=24)


class PrediccionResponse(BaseModel):
    categoria: str
    probabilidad: float


@app.get("/health")
def health():
    return {
        "status": "ok",
        "modelo_cargado": model is not None,
        "encoder_cargado": label_encoder is not None,
    }


@app.post("/predict", response_model=PrediccionResponse)
def predict(payload: PrediccionRequest):
    if model is None:
        raise HTTPException(
            status_code=503,
            detail="El modelo no está cargado en el servicio todavía.",
        )

    fila = {
        "consumo_kwh": payload.consumo_kwh,
        "uso_horario_pico": int(payload.uso_horario_pico),
        "cantidad_equipos": payload.cantidad_equipos,
        "horas_alto_consumo": payload.horas_alto_consumo,
        "tipo_inmueble": normalizar_tipo_inmueble(payload.tipo_inmueble),
    }
    df = pd.DataFrame([fila])

    pred_idx = model.predict(df)[0]
    proba = model.predict_proba(df)[0]
    probabilidad = float(max(proba))

    if label_encoder is not None:
        categoria = label_encoder.inverse_transform([pred_idx])[0]
    else:
        categoria = str(pred_idx)

    return PrediccionResponse(categoria=categoria, probabilidad=round(probabilidad, 4))


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
