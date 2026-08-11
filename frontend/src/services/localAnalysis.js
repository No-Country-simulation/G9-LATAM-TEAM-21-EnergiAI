export function analizarConsumoLocal(consumoTotal, horasPico, equipos, perfil) {
  const tarifa = 0.75;
  const moneda = 'R$';
  const promedioEstimado = 220;

  let puntaje = 0;
  let consumoEsperado = 0;

  if (perfil === 'hogar') consumoEsperado = promedioEstimado * 0.9;
  else if (perfil === 'oficina') consumoEsperado = promedioEstimado * 1.2;
  else if (perfil === 'comercio') consumoEsperado = promedioEstimado * 1.5;
  else consumoEsperado = promedioEstimado * 1.1;

  const horasFactor = Math.min(horasPico / 4, 3);
  const equiposFactor = Math.min(equipos / 6, 2.5);

  puntaje = (consumoTotal / consumoEsperado) * 0.5 + (horasFactor * 0.25) + (equiposFactor * 0.25);
  if (perfil === 'comercio' || perfil === 'oficina') puntaje = puntaje * 0.9;

  let clasificacion = '';
  if (puntaje < 0.65) clasificacion = 'Eficiente';
  else if (puntaje < 1.1) clasificacion = 'Moderado';
  else clasificacion = 'Ineficiente';

  const costo = consumoTotal * tarifa;
  let eficiencia = Math.max(0, Math.min(100, 100 - ((puntaje - 0.4) * 50)));
  if (clasificacion === 'Eficiente') eficiencia = Math.min(100, eficiencia + 15);
  else if (clasificacion === 'Ineficiente') eficiencia = Math.max(10, eficiencia - 10);

  let recomendaciones = [];
  if (clasificacion === 'Ineficiente') {
    recomendaciones.push('Tu consumo es elevado comparado con el promedio estimado.');
    recomendaciones.push('Desconecta equipos en stand-by, reducen hasta un 12% del consumo.');
    recomendaciones.push('Sustituye bombillas tradicionales por LED de bajo consumo.');
    if (horasPico > 4) recomendaciones.push('Traslada actividades de alto consumo a horas de menor demanda.');
    if (equipos > 6) recomendaciones.push('Revisa la antigüedad de tus equipos; los modernos son más eficientes.');
    recomendaciones.push('Ajusta la climatización: cada grado extra supone un 7% más de gasto.');
  } else if (clasificacion === 'Moderado') {
    recomendaciones.push('Tu consumo es moderado, hay margen de mejora.');
    recomendaciones.push('Programa apagado automático de equipos no esenciales.');
    if (horasPico > 3) recomendaciones.push('Intenta reducir las horas de uso intensivo en un 10%.');
    recomendaciones.push('Considera un sistema de energía solar para reducir costos a largo plazo.');
  } else {
    recomendaciones.push('Excelente gestión energética.');
    recomendaciones.push('Comparte tus hábitos sostenibles con tu comunidad.');
    recomendaciones.push('Explora tarifas con discriminación horaria para optimizar aún más.');
  }

  return {
    clasificacion,
    consumo: consumoTotal,
    costo,
    eficiencia: Math.round(eficiencia),
    probabilidad: clasificacion === 'Eficiente' ? 0.92 : clasificacion === 'Moderado' ? 0.75 : 0.85,
    recomendaciones,
    perfil,
    horasPico,
    equipos,
    moneda
  };
}
