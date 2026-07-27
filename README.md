# MVP — Eficiencia Energética Residencial (EnergiAI)
Hackathon ONE · Alura + Oracle · Equipo G9 LATAM

Clasifica el perfil de consumo eléctrico de una vivienda o pequeño
establecimiento (`Eficiente` / `Moderado` / `Ineficiente`), genera
recomendaciones de ahorro y estima el impacto financiero y ambiental
mensual, siguiendo el brief del reto + el diccionario de datos del equipo.

## Estructura del proyecto

```
energia_mvp/
├── data/
│   ├── generate_dataset.py             # generador original (5 variables, archivado)
│   ├── generate_dataset_extendido.py   # generador EnergiAI extendido (13 variables, EN USO)
│   └── consumo_energetico*.csv         # datasets generados
├── notebooks/
│   └── analisis_consumo_energetico.ipynb   # EDA + comparación de esquemas + entrenamiento
├── models/
│   ├── modelo_eficiencia_energetica.pkl              # modelo anterior (5 vars, archivado)
│   ├── modelo_eficiencia_energetica_extendido.pkl    # modelo EN PRODUCCIÓN (13 vars)
│   ├── label_encoder_extendido.pkl
│   └── model_metadata_extendido.json
├── db/
│   ├── schema.sql        # DDL (PostgreSQL/SQLite, notas de portabilidad a OCI Autonomous DB)
│   └── schema.mermaid    # diagrama entidad-relación
├── api/
│   ├── main.py                # FastAPI: endpoints REST
│   ├── schemas.py              # validación Pydantic (esquema extendido)
│   ├── recommendations.py      # motor de recomendaciones + costo dual + CO2
│   ├── dashboard.html          # dashboard en vivo (HTML/JS, standalone)
│   └── simular_trafico.py      # generador de tráfico sintético para la demo
├── requirements.txt
└── Dockerfile
```

## Cómo correr localmente

**Requiere Python 3.11** (las versiones pineadas de xgboost/scikit-learn no
son compatibles con 3.12+ en todos los casos, ni con 3.14). Verifica tu
venv antes de instalar:
```powershell
.\venv\Scripts\python.exe --version   # debe decir 3.11.x
```

```bash
pip install -r requirements.txt

# 1. (Re)entrenar si hace falta — genera y sobreescribe models/*_extendido.*
jupyter nbconvert --to notebook --execute --inplace notebooks/analisis_consumo_energetico.ipynb

# 2. Levantar la API
cd api
uvicorn main:app --reload --port 8000
```

Documentación interactiva (Swagger) disponible en `http://localhost:8000/docs`.

## Endpoints

### `POST /analisis-energetico`
Entrada (5 obligatorias del brief + 9 de la Capa 2 del diccionario de
datos; estas últimas tienen valores por defecto razonables si se omiten):
```json
{
  "consumo_kwh": 650,
  "uso_horario_pico": true,
  "cantidad_equipos": 14,
  "tipo_inmueble": "Casa Grande",
  "horas_alto_consumo": 9,
  "superficie_m2": 200,
  "habitantes_ocupantes": 5,
  "factor_potencia": 0.78,
  "pct_iluminacion_led": 0.25,
  "pct_equipos_inteligentes": 0.05,
  "antiguedad_promedio_ponderada": 13,
  "tiene_paneles_solares": false,
  "capacidad_solar_kwp": 0,
  "region_pais": "Selva",
  "pais": "Colombia"
}
```
Salida:
```json
{
  "id": "b90a7fef-9f65-4173-94fa-c451b8e8a59e",
  "categoria": "Ineficiente",
  "probabilidad": 0.66,
  "recomendaciones": [
    "Reducir el uso de equipos durante los horarios pico",
    "Migrar a iluminación LED: menos del 50% del inmueble la usa actualmente"
  ],
  "costo_referencia_brief": 487.50,
  "costo_estimado_mensual": 430.62,
  "co2_evitable_kg_mes": 15.6,
  "consumo_especifico_kwh_m2": 3.25
}
```

`tipo_inmueble` acepta: `Apartamento`, `Casa`, `Casa Grande`, `Local Comercial`.
`region_pais` acepta: `Costa`, `Sierra`, `Selva`.
`pais` acepta: `Peru`, `Colombia`, `Chile`, `México` (define tarifas pico/valle y factor de CO2).

### `GET /health`
Healthcheck del servicio y del modelo cargado.

### `GET /estadisticas`
Agregados en tiempo real (conteo por categoría, por tipo de inmueble, costo
y CO2 acumulados, últimos N análisis) para alimentar el dashboard en vivo.
No toca el flujo transaccional de `/analisis-energetico`.

### `GET /resultados/{id}`
Consulta un análisis previamente generado.

## Dashboard en vivo (demo para jurados)

`api/dashboard.html` es un dashboard standalone (HTML/JS, sin dependencias)
que hace polling a `/estadisticas` cada 3 segundos: KPIs, distribución por
categoría y un feed de los últimos análisis. Pensado para no pisarle el
trabajo a los compañeros de backend/frontend — es una vista adicional de
ciencia de datos, no la app oficial.

```bash
# 1. Levantar la API (ver arriba)
# 2. Abrir api/dashboard.html en el navegador y pulsar "Conectar"
# 3. Generar tráfico simulado para que se vea "vivo" en la demo:
cd api
python3 simular_trafico.py --url http://localhost:8000 --intervalo 3
```

## Dataset

`data/generate_dataset_extendido.py` (generador **EnergiAI**, EN USO)
simula **100.000 perfiles** con 24 columnas: las 5 obligatorias del brief +
8 variables de "Capa 2" del diccionario de datos del equipo (superficie,
habitantes, % LED, % equipos inteligentes, antigüedad, paneles solares,
factor de potencia) + geografía/temporalidad (país, región, mes) + salidas
informativas de costo/CO2 (no usadas como features).

`ruido_std` (parámetro de `generar_dataset`, default `0.35`) controla la
incertidumbre deliberada en la etiqueta — ver sección 12 del notebook para
el experimento de sensibilidad y la justificación de por qué se dejó así.

El generador original de 5 variables (`data/generate_dataset.py`) se
conserva archivado, sin uso en producción.

## Modelo

Se compararon `RandomForest`, `GradientBoosting` y `XGBoost` sobre el
dataset extendido de 100,000 registros. Métricas en test (ver
`models/model_metadata_extendido.json` para el resultado exacto de la
última ejecución):

| Esquema | Accuracy | F1-macro |
|---|---|---|
| 5 variables (referencia histórica, archivado) | ~0.49 | — |
| **13 variables — XGBoost (EN PRODUCCIÓN)** | **~0.61** | **~0.61** |

El notebook documenta explícitamente por qué el esquema de 5 variables
rinde peor en este dataset (la etiqueta depende de variables que ese
esquema no incluye) y compara además una variante con ruido reducido
(`ruido_std=0.15`, accuracy ~0.78) que **no se usa en producción** — se
mantiene el ruido alto por ser más representativo de un problema real.

## Motor de recomendaciones

`api/recommendations.py` reutiliza la misma lógica de
`notebooks/analisis_consumo_energetico.ipynb` (secciones 9-10): reglas
basadas en % LED, antigüedad, factor de potencia, % equipos inteligentes,
paneles solares y horario pico, más el cálculo dual de costo (tarifa plana
del brief vs. ponderado pico/valle por país, con descuento por
autoconsumo solar) y CO2 evitable estimado.

## Integración con OCI

El servicio usa **OCI Object Storage** (API S3-compatible) para almacenar y
distribuir el modelo entrenado, desacoplando el artefacto de modelo del
contenedor de la API:

1. Sube `modelo_eficiencia_energetica_extendido.pkl`,
   `label_encoder_extendido.pkl` y `model_metadata_extendido.json` a un
   bucket de Object Storage (carpeta `models/`).
2. Define las variables de entorno al desplegar la API (en OCI Compute,
   Container Instances o Functions):
   - `OCI_BUCKET_NAME`
   - `OCI_ACCESS_KEY_ID` / `OCI_SECRET_ACCESS_KEY` (Customer Secret Keys)
   - `OCI_ENDPOINT_URL` (`https://<namespace>.compat.objectstorage.<region>.oraclecloud.com`)
3. Al arrancar, `api/main.py` descarga el modelo desde el bucket antes de
   cargarlo; si no hay configuración OCI, usa la copia local en `./models`.

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

`costo_referencia_brief` usa la tarifa plana sugerida por el reto: **$0.75
USD/kWh**. `costo_estimado_mensual` usa tarifas pico/valle ilustrativas por
país (`api/recommendations.py`), no cifras oficiales de ningún regulador.

## Pendientes / roadmap (recursos opcionales del brief)

- [ ] Persistencia real (SQLite/OCI Autonomous DB) usando `db/schema.sql`
      en vez de almacenamiento en memoria — sobrevivir un reinicio de la API
- [ ] Procesamiento por lotes vía CSV
- [ ] Alertas de alto consumo y comparación entre períodos
- [ ] Conectar el formulario del frontend oficial a los 14 campos del
      esquema extendido (o decidir cuáles se infieren por defecto)
