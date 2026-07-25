from enum import Enum
from typing import List
from pydantic import BaseModel, ConfigDict, Field


class TipoInmueble(str, Enum):
    apartamento = "Apartamento"
    casa = "Casa"
    casa_grande = "Casa Grande"
    local_comercial = "Local Comercial"


class ConsumoEnergeticoRequest(BaseModel):
    consumo_kwh: float = Field(..., gt=0, le=20000, description="Consumo mensual en kWh")
    uso_horario_pico: bool = Field(..., description="¿Hay uso significativo de equipos en horario pico?")
    cantidad_equipos: int = Field(..., ge=1, le=200, description="Cantidad de equipos eléctricos")
    tipo_inmueble: TipoInmueble
    horas_alto_consumo: int = Field(..., ge=0, le=24, description="Horas diarias de alto consumo")

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "consumo_kwh": 420,
                "uso_horario_pico": True,
                "cantidad_equipos": 10,
                "tipo_inmueble": "Casa",
                "horas_alto_consumo": 8,
            }
        }
    )


class ConsumoEnergeticoResponse(BaseModel):
    id: str
    categoria: str
    probabilidad: float
    recomendaciones: List[str]
    costo_estimado_mensual: float


class ErrorResponse(BaseModel):
    detail: str
