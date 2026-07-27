from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, ConfigDict, Field


class TipoInmueble(str, Enum):
    apartamento = "Apartamento"
    casa = "Casa"
    casa_grande = "Casa Grande"
    local_comercial = "Local Comercial"


class RegionPais(str, Enum):
    costa = "Costa"
    sierra = "Sierra"
    selva = "Selva"


class Pais(str, Enum):
    peru = "Peru"
    colombia = "Colombia"
    chile = "Chile"
    mexico = "México"


class ConsumoEnergeticoRequest(BaseModel):
    # --- Capa 1: obligatorias del brief ---
    consumo_kwh: float = Field(..., gt=0, le=20000, description="Consumo mensual en kWh")
    uso_horario_pico: bool = Field(..., description="¿Hay uso significativo de equipos en horario pico?")
    cantidad_equipos: int = Field(..., ge=1, le=200, description="Cantidad de equipos eléctricos")
    tipo_inmueble: TipoInmueble
    horas_alto_consumo: int = Field(..., ge=0, le=24, description="Horas diarias de alto consumo")

    # --- Capa 2: variables extendidas (diccionario_datos_energiai2.xlsx) ---
    superficie_m2: float = Field(..., gt=0, le=5000, description="Superficie del inmueble en m²")
    habitantes_ocupantes: int = Field(1, ge=1, le=30, description="Personas/ocupantes del inmueble")
    factor_potencia: float = Field(0.87, ge=0.5, le=1.0, description="Factor de potencia eléctrico")
    pct_iluminacion_led: float = Field(0.5, ge=0, le=1, description="Fracción de iluminación LED (0-1)")
    pct_equipos_inteligentes: float = Field(0.1, ge=0, le=1, description="Fracción de equipos inteligentes (0-1)")
    antiguedad_promedio_ponderada: float = Field(5, ge=0, le=40, description="Antigüedad promedio de equipos, en años")
    tiene_paneles_solares: bool = Field(False, description="¿Tiene paneles solares instalados?")
    capacidad_solar_kwp: float = Field(0, ge=0, le=50, description="Capacidad instalada de paneles solares en kWp")
    region_pais: RegionPais = Field(RegionPais.costa, description="Región geográfica del inmueble")
    pais: Pais = Field(Pais.peru, description="País (usado para tarifas pico/valle y factor de CO2)")

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
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
        }
    )


class ConsumoEnergeticoResponse(BaseModel):
    id: str
    categoria: str
    probabilidad: float
    recomendaciones: List[str]
    costo_referencia_brief: float
    costo_estimado_mensual: float
    co2_evitable_kg_mes: float
    consumo_especifico_kwh_m2: float
    excedente_solar_kwh: float
    ingreso_estimado_excedente_mensual: float


class ErrorResponse(BaseModel):
    detail: str
