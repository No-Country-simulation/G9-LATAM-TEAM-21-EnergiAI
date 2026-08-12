import { PERFIL_INMUEBLE_MAP, capitalizar } from '../utils/constants.js';
import { analizarConsumoLocal } from './localAnalysis.js';
import { apiFetch } from './http.js';

export async function ejecutarAnalisis({ total, cantidad, horasPico, perfil }) {
  const payload = {
    consumo_kwh: parseFloat(total.toFixed(2)),
    uso_horario_pico: horasPico > 0,
    cantidad_equipos: cantidad,
    tipo_inmueble: PERFIL_INMUEBLE_MAP[perfil] || 'Casa',
    horas_alto_consumo: Math.round(horasPico)
  };

  try {
    const response = await apiFetch('/analise-energetica', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      const errBody = await response.text().catch(() => '');
      throw new Error(`API ${response.status}: ${errBody}`);
    }

    const data = await response.json();

    let eficiencia = 50;
    if (data.categoria === 'EFICIENTE') eficiencia = 85;
    else if (data.categoria === 'MODERADO') eficiencia = 55;
    else eficiencia = 25;

    return {
      offline: false,
      resultado: {
        clasificacion: capitalizar(data.categoria),
        consumo: total,
        costo: data.costo_estimado_mensual,
        eficiencia,
        probabilidad: data.probabilidad,
        recomendaciones: data.recomendaciones,
        perfil,
        horasPico,
        equipos: cantidad,
        moneda: 'R$'
      }
    };
  } catch (err) {
    console.warn('API no disponible, usando cálculo local:', err);
    return {
      offline: true,
      resultado: analizarConsumoLocal(total, horasPico, cantidad, perfil)
    };
  }
}
