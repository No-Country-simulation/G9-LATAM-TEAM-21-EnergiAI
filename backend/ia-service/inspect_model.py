"""Inspecciona el modelo serializado para revisar su estructura y el
preprocesamiento de cada columna (uso: python inspect_model.py)."""
import joblib

MODEL_PATH = "model.pkl"

modelo = joblib.load(MODEL_PATH)

print("Tipo:", type(modelo))

if hasattr(modelo, "steps"):
    print("\nPipeline con pasos:")
    for nombre, paso in modelo.steps:
        print(f"  - {nombre}: {type(paso)}")

    for nombre, paso in modelo.steps:
        if hasattr(paso, "transformers_"):
            print(f"\nColumnTransformer en '{nombre}':")
            for tname, transformer, cols in paso.transformers_:
                print(f"  - {tname}: {type(transformer)} -> columnas: {cols}")
                if hasattr(transformer, "categories_"):
                    print(f"    categorías: {transformer.categories_}")

if hasattr(modelo, "feature_names_in_"):
    print("\nfeature_names_in_:", list(modelo.feature_names_in_))

if hasattr(modelo, "get_booster"):
    booster = modelo.get_booster()
    print("\nXGBClassifier directo.")
    print("Feature names:", booster.feature_names)
    print("Feature types:", booster.feature_types)
