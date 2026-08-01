(function() {
  "use strict";

  const horasPicoInput = document.getElementById('horasPico');
  const perfilUsoSelect = document.getElementById('perfilUso');
  const analizarBtn = document.getElementById('analizarBtn');

  const perfilBadge = document.getElementById('perfilBadge');
  const clasificacionTexto = document.getElementById('clasificacionTexto');
  const consumoEstimado = document.getElementById('consumoEstimado');
  const costoMensual = document.getElementById('costoMensual');
  const indiceEficiencia = document.getElementById('indiceEficiencia');
  const confianzaIA = document.getElementById('confianzaIA');
  const recoList = document.getElementById('recoList');
  const resultadoPlaceholder = document.getElementById('resultadoPlaceholder');
  const resultadoDetalle = document.getElementById('resultadoDetalle');

  const aparatosBody = document.getElementById('aparatosBody');
  const totalAparatos = document.getElementById('totalAparatos');
  const totalConsumo = document.getElementById('totalConsumo');
  const aparatosCount = document.getElementById('aparatosCount');
  const agregarAparatoBtn = document.getElementById('agregarAparatoBtn');
  const resetAparatosBtn = document.getElementById('resetAparatosBtn');

  const themeToggle = document.getElementById('themeToggle');
  const themeIcon = document.getElementById('themeIcon');
  const themeLabel = document.getElementById('themeLabel');

  const DEFAULT = {
    horasPico: 5,
    perfil: 'comercio'
  };

  const APARATOS_POR_DEFECTO = [
    { nombre: 'Refrigerador', consumo: 60, cantidad: 1 },
    { nombre: 'Lavadora', consumo: 30, cantidad: 1 },
    { nombre: 'Microondas', consumo: 20, cantidad: 1 },
    { nombre: 'Televisor', consumo: 25, cantidad: 1 },
    { nombre: 'Computadora', consumo: 30, cantidad: 1 },
    { nombre: 'Router', consumo: 10, cantidad: 1 },
    { nombre: 'Aire acondicionado', consumo: 150, cantidad: 1 },
    { nombre: 'Bombillas LED', consumo: 15, cantidad: 5 }
  ];

  let aparatos = [];
  const APARATOS_KEY = 'energia_aparatos_v2';

  function cargarAparatos() {
    try {
      const data = localStorage.getItem(APARATOS_KEY);
      if (data) {
        aparatos = JSON.parse(data);
      } else {
        aparatos = JSON.parse(JSON.stringify(APARATOS_POR_DEFECTO));
      }
    } catch {
      aparatos = JSON.parse(JSON.stringify(APARATOS_POR_DEFECTO));
    }
  }

  function guardarAparatos() {
    localStorage.setItem(APARATOS_KEY, JSON.stringify(aparatos));
  }

  function actualizarEstadoBoton() {
    const hayAparatos = aparatos.length > 0;
    const datosValidos = aparatos.every(a => {
      const nombre = (a.nombre || '').trim();
      const consumo = parseFloat(a.consumo);
      const cantidad = parseInt(a.cantidad);
      return nombre !== '' && consumo > 0 && cantidad > 0;
    });
    analizarBtn.disabled = !(hayAparatos && datosValidos);
  }

  function corregirNegativos(value) {
    const num = parseFloat(value);
    return isNaN(num) || num < 0 ? 0 : num;
  }

  function mostrarToast(titulo, mensaje, tipo = 'success') {
    const existing = document.querySelector('.toast');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.className = `toast ${tipo === 'error' ? 'error' : ''}`;
    toast.innerHTML = `
      <div class="toast-title">${titulo}</div>
      <div class="toast-message">${mensaje}</div>
    `;
    document.body.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.3s';
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  }

  function calcularTotales() {
    let total = 0;
    let cantidad = 0;
    aparatos.forEach(a => {
      const consumo = parseFloat(a.consumo) || 0;
      const cant = parseInt(a.cantidad) || 0;
      total += consumo * cant;
      cantidad += cant;
    });
    return { total, cantidad };
  }

  function renderizarAparatos() {
    aparatosBody.innerHTML = '';
    let total = 0;
    let cantidad = 0;

    aparatos.forEach((aparato, index) => {
      const consumo = parseFloat(aparato.consumo) || 0;
      const cant = parseInt(aparato.cantidad) || 0;
      const subtotal = consumo * cant;
      total += subtotal;
      cantidad += cant;

      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>
          <input type="text" class="input-nombre" value="${aparato.nombre}"
                 data-index="${index}" data-field="nombre"
                 placeholder="Nombre del aparato">
        </td>
        <td>
          <input type="number" class="input-consumo" value="${consumo}"
                 data-index="${index}" data-field="consumo"
                 step="0.1" min="0" placeholder="0">
        </td>
        <td>
          <input type="number" class="input-cantidad" value="${cant}"
                 data-index="${index}" data-field="cantidad"
                 step="1" min="0" placeholder="1">
        </td>
        <td><strong>${subtotal.toFixed(1)}</strong> kWh</td>
        <td>
          <button class="btn-danger-small eliminar-aparato" data-index="${index}">✕</button>
        </td>
      `;
      aparatosBody.appendChild(tr);
    });

    totalAparatos.textContent = cantidad;
    totalConsumo.textContent = total.toFixed(1);
    aparatosCount.textContent = aparatos.length;

    document.querySelectorAll('.input-nombre, .input-consumo, .input-cantidad').forEach(input => {
      input.addEventListener('change', function() {
        const index = parseInt(this.dataset.index);
        const field = this.dataset.field;
        let value = this.value;
        if (field === 'consumo' || field === 'cantidad') {
          value = corregirNegativos(value);
          this.value = value;
        }
        aparatos[index][field] = value;
        guardarAparatos();
        renderizarAparatos();
        actualizarEstadoBoton();
      });
      input.addEventListener('input', function() {
        if (this.dataset.field === 'consumo' || this.dataset.field === 'cantidad') {
          if (parseFloat(this.value) < 0) this.value = 0;
        }
      });
    });

    document.querySelectorAll('.eliminar-aparato').forEach(btn => {
      btn.addEventListener('click', function() {
        const index = parseInt(this.dataset.index);
        aparatos.splice(index, 1);
        guardarAparatos();
        renderizarAparatos();
        actualizarEstadoBoton();
      });
    });

    actualizarEstadoBoton();
  }

  function agregarAparato() {
    aparatos.push({ nombre: 'Nuevo aparato', consumo: 0, cantidad: 1 });
    guardarAparatos();
    renderizarAparatos();
    actualizarEstadoBoton();
    const inputs = document.querySelectorAll('.input-nombre');
    if (inputs.length > 0) {
      inputs[inputs.length - 1].focus();
      inputs[inputs.length - 1].select();
    }
  }

  function resetearAparatos() {
    if (confirm('¿Vaciar la lista de aparatos?')) {
      aparatos = [];
      guardarAparatos();
      renderizarAparatos();
      actualizarEstadoBoton();
      mostrarToast('Lista vaciada', 'Se eliminaron todos los aparatos');
    }
  }

  function analizarConsumoLocal(consumoTotal, horasPico, equipos, perfil) {
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

  function renderizar(resultado) {
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

  const PERFIL_INMUEBLE_MAP = {
    hogar: 'Casa',
    oficina: 'Local Comercial',
    comercio: 'Local Comercial',
    mixto: 'Casa Grande'
  };

  function capitalizar(str) {
    return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
  }

  async function ejecutarAnalisis() {
    const { total, cantidad } = calcularTotales();
    const horasPico = parseFloat(horasPicoInput.value) || 0;
    const perfil = perfilUsoSelect.value;

    if (total <= 0 || cantidad <= 0) {
      alert('Por favor, agrega al menos un aparato con consumo > 0.');
      return;
    }

    const payload = {
      consumo_kwh: parseFloat(total.toFixed(2)),
      uso_horario_pico: horasPico > 0,
      cantidad_equipos: cantidad,
      tipo_inmueble: PERFIL_INMUEBLE_MAP[perfil] || 'Casa',
      horas_alto_consumo: Math.round(horasPico)
    };

    analizarBtn.disabled = true;
    analizarBtn.textContent = 'Analizando...';

    try {
      const response = await fetch('/api/analise-energetica', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
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

      const resultado = {
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
      };

      renderizar(resultado);
      mostrarToast('Análisis completado', `Clasificación: ${resultado.clasificacion}`);
    } catch (err) {
      console.warn('API no disponible, usando cálculo local:', err);
      const resultado = analizarConsumoLocal(total, horasPico, cantidad, perfil);
      renderizar(resultado);
      mostrarToast('Modo offline', 'Cálculo local (API no disponible)');
    }

    analizarBtn.textContent = 'Analizar';
    actualizarEstadoBoton();
  }

  function toggleTheme() {
    const html = document.documentElement;
    const current = html.getAttribute('data-theme');
    if (current === 'dark') {
      html.removeAttribute('data-theme');
      themeIcon.textContent = '🌙';
      themeLabel.textContent = 'Nocturno';
    } else {
      html.setAttribute('data-theme', 'dark');
      themeIcon.textContent = '☀️';
      themeLabel.textContent = 'Claro';
    }
  }

  analizarBtn.addEventListener('click', ejecutarAnalisis);
  themeToggle.addEventListener('click', toggleTheme);
  agregarAparatoBtn.addEventListener('click', agregarAparato);
  resetAparatosBtn.addEventListener('click', resetearAparatos);

  [horasPicoInput, perfilUsoSelect].forEach(el => {
    el.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') ejecutarAnalisis();
    });
  });

  cargarAparatos();
  renderizarAparatos();
  horasPicoInput.value = DEFAULT.horasPico;
  perfilUsoSelect.value = DEFAULT.perfil;
  actualizarEstadoBoton();

  horasPicoInput.addEventListener('input', function() {
    if (parseFloat(this.value) < 0) this.value = 0;
  });

})();
