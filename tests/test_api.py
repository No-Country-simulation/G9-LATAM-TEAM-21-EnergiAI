"""
Pruebas automatizadas de la API de eficiencia energética (esquema extendido).
Ejecutar con: pytest tests/test_api.py -v
"""
import sys
from pathlib import Path

sys.path.append(str(Path(__file__).resolve().parent.parent / "api"))

import pytest
from fastapi.testclient import TestClient
from main import app


@pytest.fixture(scope="module")
def client():
    with TestClient(app) as c:
        yield c


def payload_base(**overrides):
    base = {
        "consumo_kwh": 420,
        "uso_horario_pico": True,
        "cantidad_equipos": 10,
        "tipo_inmueble": "Casa",
        "horas_alto_consumo": 8,
        "superficie_m2": 120,
        "habitantes_ocupantes": 4,
        "factor_potencia": 0.85,
        "pct_iluminacion_led": 0.4,
        "pct_equipos_inteligentes": 0.15,
        "antiguedad_promedio_ponderada": 9,
        "tiene_paneles_solares": False,
        "capacidad_solar_kwp": 0,
        "region_pais": "Costa",
        "pais": "Peru",
    }
    base.update(overrides)
    return base


def test_health(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ok"


def test_analisis_energetico_caso_ineficiente(client):
    payload = payload_base(
        consumo_kwh=900, cantidad_equipos=20, tipo_inmueble="Local Comercial",
        horas_alto_consumo=12, pct_iluminacion_led=0.1, antiguedad_promedio_ponderada=18,
        factor_potencia=0.72,
    )
    resp = client.post("/analisis-energetico", json=payload)
    assert resp.status_code == 200
    body = resp.json()
    assert body["categoria"] in ["Eficiente", "Moderado", "Ineficiente"]
    assert 0 <= body["probabilidad"] <= 1
    assert body["costo_referencia_brief"] == round(900 * 0.75, 2)
    assert len(body["recomendaciones"]) > 0
    assert "consumo_especifico_kwh_m2" in body
    assert "co2_evitable_kg_mes" in body


def test_analisis_energetico_esquema_minimo_con_defaults(client):
    # Solo los campos obligatorios (los de Capa 2 usan sus valores por defecto)
    payload = {
        "consumo_kwh": 420,
        "uso_horario_pico": True,
        "cantidad_equipos": 10,
        "tipo_inmueble": "Casa",
        "horas_alto_consumo": 8,
        "superficie_m2": 120,
    }
    resp = client.post("/analisis-energetico", json=payload)
    assert resp.status_code == 200
    assert resp.json()["costo_referencia_brief"] == 315.0


def test_validacion_consumo_negativo(client):
    payload = payload_base(consumo_kwh=-10)
    resp = client.post("/analisis-energetico", json=payload)
    assert resp.status_code == 422


def test_validacion_superficie_negativa(client):
    payload = payload_base(superficie_m2=-5)
    resp = client.post("/analisis-energetico", json=payload)
    assert resp.status_code == 422


def test_validacion_tipo_inmueble_invalido(client):
    payload = payload_base(tipo_inmueble="Castillo")
    resp = client.post("/analisis-energetico", json=payload)
    assert resp.status_code == 422


def test_validacion_pais_invalido(client):
    payload = payload_base(pais="Narnia")
    resp = client.post("/analisis-energetico", json=payload)
    assert resp.status_code == 422


def test_consulta_resultado_inexistente(client):
    resp = client.get("/resultados/id-que-no-existe")
    assert resp.status_code == 404


def test_flujo_completo_analisis_y_consulta(client):
    payload = payload_base(consumo_kwh=150, cantidad_equipos=4, tipo_inmueble="Apartamento")
    creado = client.post("/analisis-energetico", json=payload).json()
    consultado = client.get(f"/resultados/{creado['id']}").json()
    assert consultado == creado


def test_estadisticas_refleja_analisis_creados(client):
    client.post("/analisis-energetico", json=payload_base())
    resp = client.get("/estadisticas")
    assert resp.status_code == 200
    body = resp.json()
    assert body["total_analizados"] >= 1
    assert set(body["conteo_por_categoria"].keys()) == {"Eficiente", "Moderado", "Ineficiente"}
