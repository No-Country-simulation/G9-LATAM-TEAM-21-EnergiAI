export function renderizarResultado(resultado) {
  const perfilBadge = document.getElementById('perfilBadge');
  const clasificacionTexto = document.getElementById('clasificacionTexto');
  const consumoEstimado = document.getElementById('consumoEstimado');
  const costoMensual = document.getElementById('costoMensual');
  const indiceEficiencia = document.getElementById('indiceEficiencia');
  const confianzaIA = document.getElementById('confianzaIA');
  const recoList = document.getElementById('recoList');
  const resultadoPlaceholder = document.getElementById('resultadoPlaceholder');
  const resultadoDetalle = document.getElementById('resultadoDetalle');

  const { clasificacion, consumo, costo, eficiencia, recomendaciones, moneda, probabilidad } = resultado;

  resultadoPlaceholder.style.display = 'none';
  resultadoDetalle.style.display = 'block';

  perfilBadge.textContent = clasificacion;
  perfilBadge.className = 'profile-badge';
  if (clasificacion === 'Eficiente') perfilBadge.classList.add('badge-eficiente');
  else if (clasificacion === 'Moderado') perfilBadge.classList.add('badge-moderado');
  else if (clasificacion === 'Ineficiente') perfilBadge.classList.add('badge-ineficiente');

  clasificacionTexto.textContent = `· Perfil ${resultado.perfil} · ${resultado.horasPico}h pico · ${resultado.equipos} equipos`;

  consumoEstimado.innerHTML = `${consumo.toFixed(1)} <small>kWh</small>`;
  costoMensual.innerHTML = `${moneda} ${costo.toFixed(2)}`;
  indiceEficiencia.innerHTML = `${eficiencia} <small>%</small>`;

  const probValor = probabilidad != null ? (probabilidad * 100).toFixed(1) : '—';
  confianzaIA.innerHTML = `${probValor} <small>%</small>`;

  recoList.innerHTML = '';
  if (recomendaciones.length === 0) {
    recoList.innerHTML = '<li>Sin recomendaciones adicionales.</li>';
  } else {
    recomendaciones.forEach(r => {
      const li = document.createElement('li');
      li.textContent = r;
      recoList.appendChild(li);
    });
  }
}

export function actualizarEstadoBoton(aparatos) {
  const analizarBtn = document.getElementById('analizarBtn');
  const hayAparatos = aparatos.length > 0;
  const datosValidos = aparatos.every(a => {
    const nombre = (a.nombre || '').trim();
    const consumo = parseFloat(a.consumo);
    const cantidad = parseInt(a.cantidad);
    return nombre !== '' && consumo > 0 && cantidad > 0;
  });
  analizarBtn.disabled = !(hayAparatos && datosValidos);
}
