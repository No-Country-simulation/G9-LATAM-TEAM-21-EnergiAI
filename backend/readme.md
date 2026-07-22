# Eficiencia Energética — Hackathon ONE G9

## Arquitectura

```
Cliente (front / Postman)
        │  POST /analisis-energetico
        ▼
┌───────────────────────┐        POST /predict        ┌────────────────────────┐
│   backend-api          │ ───────────────────────────▶ │   ia-service            │
│   Spring Boot (8080)   │ ◀─────────────────────────── │   FastAPI (8000)        │
│   - valida entrada     │      {categoria, probabilidad}│   - carga model.pkl     │
│   - pide predicción    │                               │   - carga label_encoder │
│   - genera             │                               │     .pkl                │
│     recomendaciones    │                               │                         │
│   - calcula costo      │                               │                         │
│   - guarda historial   │                               │                         │
└───────────────────────┘                               └────────────────────────┘
```

`backend-api` es la API pública (`POST /analisis-energetico`,
`GET /analisis-energetico/{id}`). `ia-service` es un microservicio
interno que hace la inferencia con el modelo XGBoost.

## Requisitos

- `ia-service/model.pkl`: modelo entrenado. Colocar en esa carpeta, o configurar `MODEL_URL`/`ENCODER_URL`
  para descargarlo desde OCI Object Storage al arrancar.
- `tipo_inmueble` acepta: Apartamento, Casa, Casa Grande, Local Comercial
  (no distingue mayúsculas/minúsculas).

## Cómo correr

**ia-service:**
```bash
cd ia-service
python -m venv venv && source venv/bin/activate  # Windows: venv\Scripts\Activate.ps1
pip install -r requirements.txt
python app.py            # localhost:8000
```

**backend-api:**
```bash
cd backend-api
mvn spring-boot:run      # localhost:8080
```

## Documentación de los endpoints

Con `backend-api` corriendo: `http://localhost:8080/swagger-ui.html`

## Ejemplos de uso

Ver `backend-api/EJEMPLOS_USO.md`.

## Prueba rápida

```bash
curl -X POST http://localhost:8080/analisis-energetico \
  -H "Content-Type: application/json" \
  -d '{
    "consumo_kwh": 420,
    "uso_horario_pico": true,
    "cantidad_equipos": 10,
    "tipo_inmueble": "Casa",
    "horas_alto_consumo": 8
  }'
```

