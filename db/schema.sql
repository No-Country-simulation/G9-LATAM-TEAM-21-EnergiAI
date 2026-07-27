-- =====================================================================
-- Esquema de base de datos — MVP Eficiencia Energética (EnergiAI)
-- Hackathon ONE — Alura + Oracle | Equipo G9 LATAM
-- =====================================================================
-- Escrito en SQL estándar (compatible con PostgreSQL 14+ y SQLite 3.35+).
-- Notas de portabilidad a OCI Autonomous DB (Oracle) al final del archivo.
--
-- Objetivo: reemplazar el almacenamiento en memoria (_RESULTADOS en
-- api/main.py) por persistencia real, y llevar trazabilidad de qué
-- versión de modelo generó cada análisis (buena práctica de MLOps).
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. TIPOS_INMUEBLE
-- Catálogo/lookup de tipos de inmueble soportados y sus rangos típicos
-- de equipos, tal como los define data/generate_dataset.py. Permite que
-- las reglas de negocio (recommendations.py) y la validación de la API
-- se apoyen en datos, no en constantes hardcodeadas en el código.
-- ---------------------------------------------------------------------
CREATE TABLE tipos_inmueble (
    tipo_inmueble       VARCHAR(50)   PRIMARY KEY,
    consumo_base_kwh    NUMERIC(8,2)  NOT NULL,
    dispersion          NUMERIC(8,2)  NOT NULL,
    equipos_min         INTEGER       NOT NULL,
    equipos_max         INTEGER       NOT NULL,
    descripcion         TEXT,
    CONSTRAINT chk_equipos_rango CHECK (equipos_min <= equipos_max)
);

INSERT INTO tipos_inmueble (tipo_inmueble, consumo_base_kwh, dispersion, equipos_min, equipos_max, descripcion) VALUES
    ('Apartamento',     180, 40, 3,  10, 'Vivienda en edificio multifamiliar'),
    ('Casa',            280, 70, 5,  16, 'Vivienda unifamiliar estándar'),
    ('Casa Grande',     420, 90, 8,  22, 'Vivienda unifamiliar de mayor tamaño'),
    ('Local Comercial', 550, 130, 6, 20, 'Pequeño establecimiento comercial u oficina');


-- ---------------------------------------------------------------------
-- 2. MODELOS
-- Registro de versiones del modelo entrenado (model registry). Cada fila
-- corresponde a una corrida del notebook (models/model_metadata.json).
-- Solo un modelo debería estar "activo" (el que usa la API en producción).
-- ---------------------------------------------------------------------
CREATE TABLE modelos (
    id                          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre                      VARCHAR(50)   NOT NULL,          -- ej. 'XGBoost'
    version                     VARCHAR(20)   NOT NULL,          -- ej. '1.0.0'
    fecha_entrenamiento         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    n_registros_entrenamiento   INTEGER       NOT NULL,
    accuracy                    NUMERIC(5,4),
    f1_macro                    NUMERIC(5,4),
    ruta_artefacto              VARCHAR(255)  NOT NULL,          -- path local u OCI Object Storage key
    tarifa_referencia_usd_kwh   NUMERIC(6,4)  NOT NULL DEFAULT 0.75,
    activo                      BOOLEAN       NOT NULL DEFAULT FALSE,
    UNIQUE (nombre, version)
);

-- Garantiza que solo haya un modelo activo a la vez (PostgreSQL: índice
-- único parcial; en SQLite se valida a nivel de aplicación).
CREATE UNIQUE INDEX idx_modelo_unico_activo
    ON modelos (activo)
    WHERE activo = TRUE;


-- ---------------------------------------------------------------------
-- 3. ANALISIS_ENERGETICO
-- Tabla principal: un registro por cada llamada a POST /analisis-energetico.
-- Reemplaza el diccionario en memoria _RESULTADOS de api/main.py.
-- ---------------------------------------------------------------------
CREATE TABLE analisis_energetico (
    id                       UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    consumo_kwh              NUMERIC(10,2) NOT NULL CHECK (consumo_kwh > 0),
    uso_horario_pico         BOOLEAN       NOT NULL,
    cantidad_equipos         INTEGER       NOT NULL CHECK (cantidad_equipos >= 1),
    tipo_inmueble            VARCHAR(50)   NOT NULL REFERENCES tipos_inmueble(tipo_inmueble),
    horas_alto_consumo       INTEGER       NOT NULL CHECK (horas_alto_consumo BETWEEN 0 AND 24),
    categoria                VARCHAR(20)   NOT NULL CHECK (categoria IN ('Eficiente','Moderado','Ineficiente')),
    probabilidad             NUMERIC(5,4)  NOT NULL CHECK (probabilidad BETWEEN 0 AND 1),
    costo_estimado_mensual   NUMERIC(10,2) NOT NULL,
    modelo_id                UUID          NOT NULL REFERENCES modelos(id),
    fecha_creacion           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    origen                   VARCHAR(20)   NOT NULL DEFAULT 'api'   -- 'api' | 'batch_csv' | 'notebook'
);

CREATE INDEX idx_analisis_fecha ON analisis_energetico (fecha_creacion);
CREATE INDEX idx_analisis_categoria ON analisis_energetico (categoria);
CREATE INDEX idx_analisis_tipo_inmueble ON analisis_energetico (tipo_inmueble);


-- ---------------------------------------------------------------------
-- 4. RECOMENDACIONES
-- Recomendaciones generadas por cada análisis, normalizadas (en vez de
-- un array/JSON embebido) para poder consultarlas, contarlas o filtrar
-- por texto más adelante (ej. "¿cuál es la recomendación más frecuente?").
-- ---------------------------------------------------------------------
CREATE TABLE recomendaciones (
    id            SERIAL        PRIMARY KEY,      -- SQLite: INTEGER PRIMARY KEY AUTOINCREMENT
    analisis_id   UUID          NOT NULL REFERENCES analisis_energetico(id) ON DELETE CASCADE,
    texto         TEXT          NOT NULL,
    orden         INTEGER       NOT NULL DEFAULT 0
);

CREATE INDEX idx_recomendaciones_analisis ON recomendaciones (analisis_id);


-- =====================================================================
-- Consultas de ejemplo que este esquema habilita
-- =====================================================================

-- Historial de un usuario/sesión reciente (recurso opcional del brief)
-- SELECT * FROM analisis_energetico ORDER BY fecha_creacion DESC LIMIT 20;

-- Ranking de eficiencia energética por tipo de inmueble (recurso opcional)
-- SELECT tipo_inmueble, categoria, COUNT(*) AS total
-- FROM analisis_energetico
-- GROUP BY tipo_inmueble, categoria
-- ORDER BY tipo_inmueble, total DESC;

-- Recomendación más frecuente entregada (para priorizar contenido educativo)
-- SELECT texto, COUNT(*) AS veces
-- FROM recomendaciones
-- GROUP BY texto
-- ORDER BY veces DESC;

-- Comparación entre períodos (recurso opcional): consumo promedio por mes
-- SELECT DATE_TRUNC('month', fecha_creacion) AS mes, AVG(consumo_kwh) AS consumo_promedio
-- FROM analisis_energetico
-- GROUP BY 1 ORDER BY 1;


-- =====================================================================
-- Notas de portabilidad a OCI Autonomous DB (Oracle)
-- =====================================================================
-- Si decides usar OCI Autonomous DB en vez de PostgreSQL/SQLite, ajusta:
--   1. UUID              -> RAW(16) DEFAULT SYS_GUID()
--   2. BOOLEAN           -> NUMBER(1) CHECK (col IN (0,1))
--   3. TEXT               -> CLOB
--   4. VARCHAR(n)         -> VARCHAR2(n)
--   5. SERIAL             -> NUMBER GENERATED BY DEFAULT AS IDENTITY
--   6. CURRENT_TIMESTAMP  -> SYSTIMESTAMP
--   7. DATE_TRUNC(...)    -> TRUNC(fecha_creacion, 'MM')
--   8. Índice único parcial (WHERE activo = TRUE) no existe en Oracle;
--      usar un trigger o una función-based unique index en su lugar.
-- =====================================================================
