# EnergiAl - Inteligencia para el Consumo Energético ⚡

**Sector:** Sostenibilidad, Energía y Hogares Inteligentes

## 📖 Descripción del Proyecto
EnergiAl es una solución inteligente diseñada para analizar patrones de consumo de energía eléctrica y generar información clave que apoye la toma de decisiones sobre eficiencia energética. Recibe datos de consumo y clasifica el perfil energético del usuario en categorías como:
- 🟢 **Eficiente**
- 🟡 **Moderado**
- 🔴 **Ineficiente**

Además, ofrece recomendaciones para reducir el desperdicio energético, adoptar hábitos más sostenibles y estima impactos financieros basados en una tarifa de referencia (R$ 0,75 por kWh).

## 🎯 Necesidad del Cliente
Muchos usuarios reciben facturas de energía elevadas sin tener visibilidad clara sobre qué hábitos impactan más en sus gastos. Esta solución busca transformar datos de consumo en información útil y comprensible, permitiendo a los usuarios entender su perfil, identificar posibles desperdicios, recibir sugerencias de mejora y estimar costos operativos.

## 🚀 Objetivos del MVP (Hackathon)
El objetivo principal es desarrollar un Producto Mínimo Viable (MVP) funcional que cumpla con lo siguiente:
1. Analizar patrones de consumo energético.
2. Clasificar perfiles de eficiencia.
3. Generar recomendaciones personalizadas basadas en datos.
4. Estimar impactos financieros utilizando la tarifa de R$ 0,75/kWh.
5. Exponer estos servicios y resultados a través de una API REST.
6. Integrar y utilizar al menos un servicio de OCI (Oracle Cloud Infrastructure) en la arquitectura.

## 🛠 Tecnologías y Arquitectura

### 1. Ciencia de Datos (Data Science)
- **Lenguaje y Librerías:** Python, Pandas, Scikit-Learn.
- **Modelos Sugeridos:** Regresión Logística, Random Forest, Árboles de Decisión (se permiten otros).
- **Entregables:** Jupyter Notebook que incluya Análisis Exploratorio de Datos (EDA), limpieza, análisis de patrones, entrenamiento del modelo supervisado, evaluación con métricas y serialización del modelo.
- **Nota:** El equipo debe construir o simular su propia base de datos de consumo.

### 2. Back-End (API REST)
- **Tecnología Recomendada:** Java con Spring Boot.
- **Entregables:** 
  - Endpoint para análisis de consumo.
  - Endpoint para consulta de resultados.
  - Validación de entradas y manejo de errores.
  - Documentación de los endpoints de la API.

### 3. Infraestructura Cloud (OCI)
Uso obligatorio de al menos uno de los siguientes servicios:
- **Object Storage:** Para almacenamiento de datos o modelos.
- **OCI Compute:** Para alojamiento y despliegue de la API.
- **OCI Functions:** Para procesamiento serverless específico.
- **Base de Datos OCI:** Para persistencia de información (opcional, pero válido como requisito de OCI).

## 🔌 Especificaciones de la API (MVP)

### `POST /analisis-energetico`

**Ejemplo de Entrada (Payload):**
```json
{
  "consumo_kwh": 420,
  "uso_horario_pico": true,
  "cantidad_equipos": 10,
  "tipo_inmueble": "Casa",
  "horas_alto_consumo": 8
}
```

**Ejemplo de Salida (Response):**
```json
{
  "categoria": "Ineficiente",
  "probabilidad": 0.81,
  "recomendaciones": [
    "Reducir el uso de equipos durante horarios pico",
    "Evaluar aparatos con alto consumo energético",
    "Distribuir actividades de mayor consumo a lo largo del día"
  ],
  "costo_estimado_mensual": 315.00
}
```

## ✅ Criterios de Aceptación (Requisitos Mínimos)
- [ ] Modelo entrenado y cargado correctamente en la solución.
- [ ] Clasificación funcional del consumo.
- [ ] Generación automática de recomendaciones.
- [ ] Estimación del costo energético incluida en la respuesta.
- [ ] API REST completamente documentada.
- [ ] Integración verificable con al menos un servicio de OCI.
- [ ] Disponibilidad de un mínimo de tres ejemplos (reales o simulados) listos para prueba.

## ✨ Recursos Opcionales (Bonus)
- Dashboard visual para seguimiento del consumo y métricas.
- Procesamiento en lote mediante carga de archivos CSV.
- Contenedorización de la aplicación utilizando Docker.
- Implementación de pruebas automatizadas.
- Alertas para picos de alto consumo.
- Comparativa de consumo entre diferentes períodos o ranking de eficiencia.
- **Front-End:** Interfaz web sencilla para el ingreso de información y visualización de resultados (no es obligatorio para el MVP).
