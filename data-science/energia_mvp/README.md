# MVP — Eficiencia Energética Residencial
Hackathon ONE · Alura + Oracle · Equipo G9 LATAM

Clasifica el perfil de consumo eléctrico de una vivienda o pequeño
establecimiento (`Eficiente` / `Moderado` / `Ineficiente`), genera
recomendaciones de ahorro y estima el costo mensual, siguiendo el brief del
reto.

## Estructura del proyecto

```
energia_mvp/
├── data/
│   ├── generate_dataset.py       # generador EnergiAI del dataset sintético
│   └── consumo_energetico.csv    # dataset generado (100,000 registros)
├── notebooks/
│   └── analisis_consumo_energetico.ipynb   # EDA + entrenamiento + evaluación
├── models/
│   ├── modelo_eficiencia_energetica.pkl    # pipeline sklearn serializado
│   └── model_metadata.json                 # métricas y metadatos del modelo
├── api/
│   ├── main.py            # FastAPI: endpoints REST
│   ├── schemas.py          # validación Pydantic
│   └── recommendations.py  # motor de recomendaciones + estimación financiera
├── requirements.txt
└── Dockerfile
```

## Cómo correr localmente

```bash
pip install -r requirements.txt

# 1. Generar datos y entrenar (si no existe ya el .pkl en models/)
jupyter nbconvert --to notebook --execute --inplace notebooks/analisis_consumo_energetico.ipynb

# 2. Levantar la API
cd api
uvicorn main:app --reload --port 8000
```

Documentación interactiva (Swagger) disponible en `http://localhost:8000/docs`.

## Endpoints

### `POST /analisis-energetico`
Entrada:
```json
{
  "consumo_kwh": 420,
  "uso_horario_pico": true,
  "cantidad_equipos": 10,
  "tipo_inmueble": "Casa",
  "horas_alto_consumo": 8
}
```
Salida:
```json
{
  "id": "bfa4445e-37af-4d32-bcef-c510f40d75a6",
  "categoria": "Ineficiente",
  "probabilidad": 0.85,
  "recomendaciones": [
    "Reducir el uso de equipos durante los horarios pico",
    "Evaluar equipos con alto consumo energético",
    "Distribuir las actividades de mayor consumo a lo largo del día"
  ],
  "costo_estimado_mensual": 315.00
}
```

### `GET /resultados/{id}`
Consulta un análisis previamente generado.

### `GET /health`
Healthcheck del servicio y del modelo cargado.

## Dataset

El dataset (`data/generate_dataset.py`, generador **EnergiAI**) simula
**100.000 perfiles** de consumo con el esquema exacto pedido por el reto:
`consumo_kwh`, `uso_horario_pico`, `cantidad_equipos`, `tipo_inmueble`
(`Apartamento` / `Casa` / `Casa Grande` / `Local Comercial`),
`horas_alto_consumo` → `categoria`. Los rangos están calibrados con órdenes
de magnitud de datasets reales de consumo residencial (UCI *Tetouan City*,
UCI *Individual Household Electric Power Consumption*) y un estudio de 225
hogares en Maharashtra, India. La categoría se deriva de un índice de
eficiencia (consumo por equipo + uso en pico + horas de alto consumo) con
ruido gaussiano, para que el problema sea realista y no trivialmente
separable.

## Modelo

Se compararon `RandomForest`, `GradientBoosting` y `XGBoost` sobre el
dataset de 100,000 registros. Métricas en test (ver
`models/model_metadata.json` para el resultado exacto de la última
ejecución):

| Modelo | Accuracy | F1-macro |
|---|---|---|
| XGBoost (seleccionado) | ~0.74 | ~0.75 |

El modelo solo usa 4 variables numéricas + tipo de inmueble (one-hot), sin
variables auxiliares como antigüedad de equipos; el detalle de EDA,
entrenamiento, validación cruzada e importancia de variables está en el
notebook.

## Integración con OCI

El servicio usa **OCI Object Storage** (API S3-compatible) para almacenar y
distribuir el modelo entrenado, desacoplando el artefacto de modelo del
contenedor de la API:

1. Sube el `.pkl` y `model_metadata.json` a un bucket de Object Storage.
2. Define las variables de entorno al desplegar la API (en OCI Compute,
   Container Instances o Functions):
   - `OCI_BUCKET_NAME`
   - `OCI_ACCESS_KEY_ID` / `OCI_SECRET_ACCESS_KEY` (Customer Secret Keys)
   - `OCI_ENDPOINT_URL` (`https://<namespace>.compat.objectstorage.<region>.oraclecloud.com`)
3. Al arrancar, `api/main.py` descarga el modelo desde el bucket antes de
   cargarlo; si no hay configuración OCI, usa la copia local en `./models`.

Si no se configuran las variables OCI, la API sigue funcionando con el
modelo empaquetado localmente — útil para desarrollo y demo del hackathon.

## Despliegue con Docker

```bash
docker build -t energia-mvp-api .
docker run -p 8000:8000 energia-mvp-api

# con OCI Object Storage:
docker run -p 8000:8000 \
  -e OCI_BUCKET_NAME=energia-mvp-bucket \
  -e OCI_ACCESS_KEY_ID=... \
  -e OCI_SECRET_ACCESS_KEY=... \
  -e OCI_ENDPOINT_URL=https://<namespace>.compat.objectstorage.<region>.oraclecloud.com \
  energia-mvp-api
```

## Tarifa de referencia

Se usa la tarifa sugerida por el reto: **$0.75 USD/kWh** (`api/recommendations.py`).

## Pendientes / roadmap (recursos opcionales del brief)

- [ ] Dashboard de seguimiento e historial de análisis (persistencia en OCI
      Autonomous DB o SQLite en vez de almacenamiento en memoria)
- [ ] Procesamiento por lotes vía CSV
- [ ] Pruebas automatizadas (`tests/`)
- [ ] Alertas de alto consumo y comparación entre períodos
