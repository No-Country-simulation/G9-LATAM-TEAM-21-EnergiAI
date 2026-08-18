"""
Entrena el modelo con el contrato real del frontend (4 ciudades, formato
con guion). Registra cada corrida en MLflow.

Ubicación esperada: data-science/energia_mvp/scripts/train_v3.py
Lee el dataset desde ../data/generate_dataset.py
Guarda el modelo en ../models/
"""
import json
import sys
import warnings
from pathlib import Path

warnings.filterwarnings("ignore")

# Permite importar generate_dataset desde ../data (carpeta hermana de scripts/)
sys.path.append(str(Path(__file__).resolve().parent.parent / "data"))

import joblib
import mlflow
import mlflow.sklearn
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import GradientBoostingClassifier, RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report, f1_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import LabelEncoder, OneHotEncoder
from xgboost import XGBClassifier

from generate_dataset import generar_dataset  # type: ignore[import]

RANDOM_STATE = 42
ORDEN = ["Eficiente", "Moderado", "Ineficiente"]

FEATURES_NUM = [
    "consumo_kwh", "cantidad_equipos", "horas_alto_consumo", "superficie_m2",
    "habitantes_ocupantes", "factor_potencia", "porcentaje_iluminacion_led",
    "porcentaje_equipos_inteligentes", "antiguedad_promedio_ponderada",
    "capacidad_solar_kwp", "consumo_especifico_kwh_m2",
]
FEATURES_BOOL = ["uso_horario_pico", "tiene_paneles_solares"]
FEATURES_CAT = ["tipo_inmueble", "region_pais"]

MLFLOW_EXPERIMENT = "energia-eficiencia-clasificacion"

# Carpeta de salida: ../models (hermana de scripts/, NO scripts/models_v3)
MODELS_DIR = Path(__file__).resolve().parent.parent / "models"

MODELOS_CANDIDATOS = {
    "RandomForest": lambda: RandomForestClassifier(
        n_estimators=300, max_depth=12, class_weight="balanced", n_jobs=-1, random_state=RANDOM_STATE),
    "GradientBoosting": lambda: GradientBoostingClassifier(
        n_estimators=250, max_depth=3, random_state=RANDOM_STATE),
    "XGBoost": lambda: XGBClassifier(
        n_estimators=350, max_depth=5, learning_rate=0.1, eval_metric="mlogloss",
        n_jobs=-1, random_state=RANDOM_STATE),
}


def preparar_X_y(df):
    X = df[FEATURES_NUM + FEATURES_BOOL + FEATURES_CAT].copy()
    for col in FEATURES_BOOL:
        X[col] = X[col].astype(int)
    y = df["categoria"]
    return X, y


def entrenar_uno(nombre, constructor, X_train, X_test, y_train, y_test, preprocessor):
    with mlflow.start_run(run_name=nombre, nested=True):
        mlflow.log_param("algoritmo", nombre)
        mlflow.log_param("n_train", len(X_train))

        pipe = Pipeline([("prep", preprocessor), ("clf", constructor())])
        le = None
        if nombre == "XGBoost":
            le = LabelEncoder()
            pipe.fit(X_train, le.fit_transform(y_train))
            preds = le.inverse_transform(pipe.predict(X_test))
        else:
            pipe.fit(X_train, y_train)
            preds = pipe.predict(X_test)

        acc = accuracy_score(y_test, preds)
        f1 = f1_score(y_test, preds, average="macro")
        reporte = classification_report(y_test, preds, target_names=ORDEN, output_dict=True, zero_division=0)

        mlflow.log_metric("accuracy", acc)
        mlflow.log_metric("f1_macro", f1)
        for clase in ORDEN:
            mlflow.log_metric(f"f1_{clase.lower()}", reporte[clase]["f1-score"])

        mlflow.sklearn.log_model(pipe, name="modelo", serialization_format="cloudpickle")
        print(f"  {nombre:>18} | accuracy={acc:.3f} | f1_macro={f1:.3f}")
        return {"nombre": nombre, "pipe": pipe, "le": le, "accuracy": acc, "f1_macro": f1}


def main():
    mlflow.set_experiment(MLFLOW_EXPERIMENT)
    print("Generando dataset (contrato real del frontend, 100,000 filas)...")
    df = generar_dataset(n=100_000, seed=RANDOM_STATE)
    X, y = preparar_X_y(df)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=RANDOM_STATE, stratify=y
    )
    preprocessor = ColumnTransformer(
        [("cat", OneHotEncoder(handle_unknown="ignore"), FEATURES_CAT)], remainder="passthrough"
    )

    with mlflow.start_run(run_name="comparacion_algoritmos"):
        resultados = [
            entrenar_uno(nombre, constructor, X_train, X_test, y_train, y_test, preprocessor)
            for nombre, constructor in MODELOS_CANDIDATOS.items()
        ]
        mejor = max(resultados, key=lambda r: r["f1_macro"])
        mlflow.log_param("mejor_modelo", mejor["nombre"])
        mlflow.log_metric("mejor_f1_macro", mejor["f1_macro"])

    print(f"\nMejor modelo: {mejor['nombre']} (f1_macro={mejor['f1_macro']:.4f})")

    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    joblib.dump(mejor["pipe"], MODELS_DIR / "modelo_eficiencia_energetica_v3.pkl")
    if mejor["le"] is not None:
        joblib.dump(mejor["le"], MODELS_DIR / "label_encoder_v3.pkl")

    metadata = {
        "modelo": mejor["nombre"],
        "esquema": "contrato_frontend_real_v3",
        "n_registros_entrenamiento": len(df),
        "features_num": FEATURES_NUM,
        "features_bool": FEATURES_BOOL,
        "features_cat": FEATURES_CAT,
        "categorias": ORDEN,
        "ciudades_soportadas": ["CO-Bogota", "CO-Medellin", "MX-CDMX", "BR-Brasilia"],
        "metricas_test": {"accuracy": float(mejor["accuracy"]), "f1_macro": float(mejor["f1_macro"])},
        "tarifa_referencia_usd_kwh": 0.75,
    }
    with open(MODELS_DIR / "model_metadata_v3.json", "w") as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)
    print(f"Modelo guardado en {MODELS_DIR}/")


if __name__ == "__main__":
    main()
