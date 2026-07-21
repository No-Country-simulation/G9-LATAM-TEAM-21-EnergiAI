FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY api/ ./api/
COPY models/ ./models/

# Variables de entorno para descargar el modelo desde OCI Object Storage
# (opcional; si no se definen, la API usa el modelo local en ./models)
ENV OCI_BUCKET_NAME=""
ENV OCI_ACCESS_KEY_ID=""
ENV OCI_SECRET_ACCESS_KEY=""
ENV OCI_ENDPOINT_URL=""

EXPOSE 8000

WORKDIR /app/api
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
