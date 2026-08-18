from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, ConfigDict, Field, model_validator


class TipoInmueble(str, Enum):
    casa = "Casa"
    apartamento = "Apartamento"
    local = "Local"


class RegionPais(str, Enum):
    """Valores EXACTOS del <select id="region_pais"> en frontend/index.html
    (rama qa/frontend-mvp-version-2.0) — formato con guion, 4 ciudades."""
    co_bogota = "CO-Bogota"
    co_medellin = "CO-Medellin"
    mx_cdmx = "MX-CDMX"
    br_brasilia = "BR-Brasilia"


class ConsumoEnergeticoRequest(BaseModel):
    consumo_kwh: float = Field(..., gt=0, le=100000)
    cantidad_equipos: int = Field(..., ge=0)
    horas_alto_consumo: float = Field(..., ge=0, le=24)
    superficie_m2: float = Field(..., gt=0)
    habitantes_ocupantes: int = Field(..., ge=1)
    factor_potencia: float = Field(..., gt=0, le=1)
    porcentaje_iluminacion_led: float = Field(..., ge=0, le=1)
    porcentaje_equipos_inteligentes: float = Field(..., ge=0, le=1)
    antiguedad_promedio_ponderada: float = Field(..., ge=0, le=50)
    capacidad_solar_kwp: float = Field(0, ge=0)
    uso_horario_pico: bool
    tiene_paneles_solares: bool
    tipo_inmueble: TipoInmueble
    region_pais: RegionPais
    latitud: float = Field(..., ge=-90, le=90)
    longitud: float = Field(..., ge=-180, le=180)

    @model_validator(mode="after")
    def _validar_paneles_solares(self):
        if not self.tiene_paneles_solares:
            self.capacidad_solar_kwp = 0.0
        elif self.capacidad_solar_kwp <= 0:
            raise ValueError("tiene_paneles_solares=true requiere capacidad_solar_kwp > 0")
        return self

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "consumo_kwh": 420, "cantidad_equipos": 10, "horas_alto_consumo": 8,
                "superficie_m2": 80, "habitantes_ocupantes": 4, "factor_potencia": 0.92,
                "porcentaje_iluminacion_led": 0.60, "porcentaje_equipos_inteligentes": 0.30,
                "antiguedad_promedio_ponderada": 6.5, "capacidad_solar_kwp": 0,
                "uso_horario_pico": True, "tiene_paneles_solares": False,
                "tipo_inmueble": "Casa", "region_pais": "CO-Bogota",
                "latitud": 4.7110, "longitud": -74.0721,
            }
        }
    )


class ConsumoEnergeticoResponse(BaseModel):
    categoria: str
    probabilidad: float
    recomendaciones: List[str]
    costo_estimado_mensual: float
    id_analisis: str
    fecha: str


class ErrorResponse(BaseModel):
    detail: str
