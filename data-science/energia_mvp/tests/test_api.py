"""
Pruebas automatizadas de la API de eficiencia energética.
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


def test_health(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ok"


def test_analisis_energetico_caso_ineficiente(client):
    payload = {
        "consumo_kwh": 900,
        "uso_horario_pico": True,
        "cantidad_equipos": 20,
        "tipo_inmueble": "Local Comercial",
        "horas_alto_consumo": 12,
    }
    resp = client.post("/analisis-energetico", json=payload)
    assert resp.status_code == 200
    body = resp.json()
    assert body["categoria"] in ["Eficiente", "Moderado", "Ineficiente"]
    assert 0 <= body["probabilidad"] <= 1
    assert body["costo_estimado_mensual"] == round(900 * 0.75, 2)
    assert len(body["recomendaciones"]) > 0


def test_analisis_energetico_caso_brief_exacto(client):
    payload = {
        "consumo_kwh": 420,
        "uso_horario_pico": True,
        "cantidad_equipos": 10,
        "tipo_inmueble": "Casa",
        "horas_alto_consumo": 8,
    }
    resp = client.post("/analisis-energetico", json=payload)
    assert resp.status_code == 200
    body = resp.json()
    assert body["costo_estimado_mensual"] == 315.0


def test_validacion_consumo_negativo(client):
    payload = {
        "consumo_kwh": -10,
        "uso_horario_pico": True,
        "cantidad_equipos": 10,
        "tipo_inmueble": "Casa",
        "horas_alto_consumo": 8,
    }
    resp = client.post("/analisis-energetico", json=payload)
    assert resp.status_code == 422


def test_validacion_tipo_inmueble_invalido(client):
    payload = {
        "consumo_kwh": 420,
        "uso_horario_pico": True,
        "cantidad_equipos": 10,
        "tipo_inmueble": "Castillo",
        "horas_alto_consumo": 8,
    }
    resp = client.post("/analisis-energetico", json=payload)
    assert resp.status_code == 422


def test_validacion_cantidad_equipos_cero(client):
    payload = {
        "consumo_kwh": 420,
        "uso_horario_pico": True,
        "cantidad_equipos": 0,
        "tipo_inmueble": "Casa",
        "horas_alto_consumo": 8,
    }
    resp = client.post("/analisis-energetico", json=payload)
    assert resp.status_code == 422


def test_consulta_resultado_inexistente(client):
    resp = client.get("/resultados/id-que-no-existe")
    assert resp.status_code == 404


def test_flujo_completo_analisis_y_consulta(client):
    payload = {
        "consumo_kwh": 150,
        "uso_horario_pico": False,
        "cantidad_equipos": 4,
        "tipo_inmueble": "Apartamento",
        "horas_alto_consumo": 2,
    }
    creado = client.post("/analisis-energetico", json=payload).json()
    consultado = client.get(f"/resultados/{creado['id']}").json()
    assert consultado == creado
