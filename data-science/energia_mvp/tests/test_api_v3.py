"""
Ubicación esperada: data-science/energia_mvp/tests/test_api_v3.py
Importa main_v3 desde ../api/main_v3.py
"""
import sys
from pathlib import Path

# Permite importar main_v3 desde ../api (carpeta hermana de tests/)
sys.path.append(str(Path(__file__).resolve().parent.parent / "api"))

import pytest
from fastapi.testclient import TestClient
from main_v3 import app


@pytest.fixture(scope="module")
def client():
    with TestClient(app) as c:
        yield c


def payload_base(**overrides):
    base = {
        "consumo_kwh": 420, "cantidad_equipos": 10, "horas_alto_consumo": 8,
        "superficie_m2": 80, "habitantes_ocupantes": 4, "factor_potencia": 0.92,
        "porcentaje_iluminacion_led": 0.60, "porcentaje_equipos_inteligentes": 0.30,
        "antiguedad_promedio_ponderada": 6.5, "capacidad_solar_kwp": 0,
        "uso_horario_pico": True, "tiene_paneles_solares": False,
        "tipo_inmueble": "Casa", "region_pais": "CO-Bogota",
        "latitud": 4.7110, "longitud": -74.0721,
    }
    base.update(overrides)
    return base


def test_health(client):
    assert client.get("/health").status_code == 200


def test_endpoint_correcto_es_analise_energetica(client):
    resp = client.post("/analise-energetica", json=payload_base())
    assert resp.status_code == 200


def test_caso_del_documento_costo_exacto(client):
    resp = client.post("/analise-energetica", json=payload_base())
    assert resp.json()["costo_estimado_mensual"] == 315.00


def test_las_4_ciudades_reales_del_frontend(client):
    for ciudad in ["CO-Bogota", "CO-Medellin", "MX-CDMX", "BR-Brasilia"]:
        resp = client.post("/analise-energetica", json=payload_base(region_pais=ciudad))
        assert resp.status_code == 200, f"Falló con {ciudad}"


def test_ciudad_formato_viejo_guion_bajo_falla(client):
    resp = client.post("/analise-energetica", json=payload_base(region_pais="CO_Bogota"))
    assert resp.status_code == 422


def test_validacion_cruzada_paneles_sin_capacidad(client):
    resp = client.post("/analise-energetica", json=payload_base(
        tiene_paneles_solares=True, capacidad_solar_kwp=0
    ))
    assert resp.status_code == 422


def test_validacion_tipo_inmueble_fuera_de_enum(client):
    resp = client.post("/analise-energetica", json=payload_base(tipo_inmueble="Local Comercial"))
    assert resp.status_code == 422


def test_id_analisis_es_uuid_valido(client):
    import uuid
    resp = client.post("/analise-energetica", json=payload_base())
    uuid.UUID(resp.json()["id_analisis"])


def test_sin_api_key_no_rompe_el_analisis(client, monkeypatch):
    monkeypatch.delenv("OPENWEATHER_API_KEY", raising=False)
    resp = client.post("/analise-energetica", json=payload_base())
    assert resp.status_code == 200
