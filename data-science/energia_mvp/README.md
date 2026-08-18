# EnergIA — Ciencia de Datos ⚡
### Sector: Sostenibilidad, Energía y Hogares Inteligentes

Servicio de clasificación de eficiencia energética — parte de ciencia de
datos del proyecto EnergIA (Hackathon ONE, equipo G9 LATAM).

## 📖 Qué hace

Recibe datos de consumo eléctrico de una vivienda o local, y:
1. Clasifica el perfil energético: 🟢 **Eficiente** / 🟡 **Moderado** / 🔴 **Ineficiente**
2. Genera recomendaciones personalizadas de ahorro
3. Estima el costo mensual (tarifa de referencia $0.75/kWh)
4. Agrega una alerta preventiva si la temperatura actual (vía OpenWeatherMap)
   supera los 32°C, sugiriendo minimizar el uso de equipos de alto consumo

## 🔌 Contrato de la API

**Importante:** el endpoint de este servicio es `/analisis-energetico`. No
confundir con `/analise-energetica`, la ruta pública de `AnalisisController.java`
en el backend Java — esta API solo la llama el backend internamente vía `URL_MODELO`.

### `POST /analisis-energetico`

```json
{
  "consumo_kwh": 420,
  "cantidad_equipos": 10,
  "horas_alto_consumo": 8,
  "superficie_m2": 80,
  "habitantes_ocupantes": 4,
  "factor_potencia": 0.92,
  "porcentaje_iluminacion_led": 0.60,
  "porcentaje_equipos_inteligentes": 0.30,
  "antiguedad_promedio_ponderada": 6.5,
  "capacidad_solar_kwp": 0,
  "uso_horario_pico": true,
  "tiene_paneles_solares": false,
  "tipo_inmueble": "Casa",
  "region_pais": "CO-Bogota",
  "latitud": 4.7110,
  "longitud": -74.0721
}
```

`tipo_inmueble` acepta: `Casa`, `Apartamento`, `Local`.
`region_pais` acepta: `CO-Bogota`, `CO-Medellin`, `MX-CDMX`, `BR-Brasilia`
(valores exactos usados por el `<select>` del frontend).

**Respuesta:**
```json
{
  "categoria": "Ineficiente",
  "probabilidad": 0.81,
  "recomendaciones": [
    "Reducir el uso de equipos durante los horarios pico",
    "Evaluar equipos con alto consumo energético: menos del 50% de la iluminación es LED",
    "Distribuir las actividades de mayor consumo a lo largo del día"
  ],
  "costo_estimado_mensual": 315.00,
  "id_analisis": "a91f3c2e-88b1-4e0a-9c7d-3f2b1e0a9d1c",
  "fecha": "2026-08-16"
}
```

Validación cruzada: si `tiene_paneles_solares=false`, se fuerza
`capacidad_solar_kwp=0`; si `true`, exige `capacidad_solar_kwp>0`.

### `GET /health`
Healthcheck del servicio y del modelo cargado.

## 🗂 Estructura del proyecto

```
energia_mvp/
├── api/
│   ├── main_v3.py              # FastAPI: endpoint /analisis-energetico
│   ├── schemas_v3.py           # validación Pydantic (15 campos)
│   ├── recommendations_v3.py   # recomendaciones + cálculo de costo
│   └── openweather_client.py   # clima real (OpenWeatherMap) con fallback
├── data/
│   └── generate_dataset_v3.py  # generador del dataset sintético (100k filas)
├── models/
│   ├── modelo_eficiencia_energetica_v3.pkl
│   └── model_metadata_v3.json
├── scripts/
│   └── train_v3.py             # entrena y compara algoritmos, registra en MLflow
├── tests/
│   └── test_api_v3.py          # 9 tests automatizados
└── requirements.txt
```

## 🧠 Modelo

Se compararon `RandomForest`, `GradientBoosting` y `XGBoost` sobre 100,000
registros sintéticos con las 15 variables del contrato. Resultados (ver
`models/model_metadata_v3.json` para la corrida exacta):

| Algoritmo | Accuracy | F1-macro |
|---|---|---|
| RandomForest | 0.593 | 0.591 |
| **GradientBoosting (seleccionado)** | **0.599** | **0.596** |
| XGBoost | 0.597 | 0.592 |

GradientBoosting ganó en 4 de 5 métricas (accuracy, f1_macro, f1_eficiente,
f1_ineficiente); RandomForest fue mejor solo en `f1_moderado`. El
experimento completo, con comparación de algoritmos y matrices de
confusión, está documentado en `notebooks/evaluacion_modelo_v3.ipynb` y
registrado en MLflow (`scripts/train_v3.py`, experimento
`energia-eficiencia-clasificacion`).

`f1_ineficiente` es la métrica más baja de los 3 algoritmos (~0.46-0.48) —
detectar la categoría "Ineficiente" es la tarea más difícil del modelo,
posiblemente por el ruido deliberado del generador sintético.

## 🌡 Alerta climática

Usa la API gratuita de [OpenWeatherMap](https://openweathermap.org/api).
Requiere la variable de entorno `OPENWEATHER_API_KEY`. Si no está
configurada, o si la llamada falla/hace timeout, el análisis se completa
igual — simplemente sin la recomendación de clima (fallback no bloqueante).
Umbral: 32°C. Caché en memoria con TTL de 20 min por coordenada.

## 🚀 Cómo correr localmente

**Requiere Python 3.11.**

```bash
pip install -r requirements.txt

# (Re)entrenar si hace falta
cd scripts
python train_v3.py

# Ver comparación de experimentos en MLflow
mlflow ui --port 5000

# Levantar la API
cd ../api
uvicorn main_v3:app --reload --port 8000
```

Documentación interactiva: `http://localhost:8000/docs`

## ✅ Tests

```bash
pytest tests/test_api_v3.py -v
```

9 tests: healthcheck, las 4 ciudades reales, validación cruzada de paneles
solares, rechazo del formato antiguo de `region_pais`, formato UUID del
`id_analisis`, y que la ausencia de `OPENWEATHER_API_KEY` no rompe el análisis.

## 🐳 Docker

```bash
docker build -t energia-ia-service .
docker run -p 8000:8000 -e OPENWEATHER_API_KEY=tu_key energia-ia-service
```

Integrado en `docker-compose.yml` del proyecto raíz como el servicio `ia-service`.

## 📋 Estado de integración

- [x] Modelo entrenado, evaluado y comparado (3 algoritmos, MLflow)
- [x] Clasificación funcional con probabilidad
- [x] Recomendaciones automáticas (reglas + alerta climática condicional)
- [x] Estimación de costo mensual
- [x] API documentada (Swagger/OpenAPI automático)
- [x] 9 tests automatizados
- [x] Gate de calidad en CI (`f1_macro` mínimo 0.55)
- [ ] **Backend Java pendiente de actualizar** (`DatosRegistroAnalisis.java`
      solo acepta 5 campos; este servicio ya expone 15) — bloqueador
      conocido, coordinado con el equipo de backend