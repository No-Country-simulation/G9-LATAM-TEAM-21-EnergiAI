import { DEFAULT } from './utils/constants.js';
import { cargarAparatos } from './utils/storage.js';
import { initThemeToggle } from './components/header.js';
import { crearAparatosController } from './components/appliancesTable.js';
import { renderizarResultado, actualizarEstadoBoton } from './components/analysisResult.js';
import { mostrarToast } from './components/toast.js';
import { ejecutarAnalisis } from './services/analysisApi.js';

export function initApp() {
  const horasPicoInput = document.getElementById('horasPico');
  const perfilUsoSelect = document.getElementById('perfilUso');
  const analizarBtn = document.getElementById('analizarBtn');

  initThemeToggle();

  const aparatosCtrl = crearAparatosController({
    onChange: () => actualizarEstadoBoton(aparatosCtrl.getAparatos())
  });

  aparatosCtrl.setAparatos(cargarAparatos());

  horasPicoInput.value = DEFAULT.horasPico;
  perfilUsoSelect.value = DEFAULT.perfil;
  actualizarEstadoBoton(aparatosCtrl.getAparatos());

  horasPicoInput.addEventListener('input', function () {
    if (parseFloat(this.value) < 0) this.value = 0;
  });

  async function onAnalizar() {
    const { total, cantidad } = aparatosCtrl.calcularTotales();
    const horasPico = parseFloat(horasPicoInput.value) || 0;
    const perfil = perfilUsoSelect.value;

    if (total <= 0 || cantidad <= 0) {
      alert('Por favor, agrega al menos un aparato con consumo > 0.');
      return;
    }

    analizarBtn.disabled = true;
    analizarBtn.textContent = 'Analizando...';

    const { offline, resultado } = await ejecutarAnalisis({ total, cantidad, horasPico, perfil });
    renderizarResultado(resultado);

    if (offline) {
      mostrarToast('Modo offline', 'Cálculo local (API no disponible)');
    } else {
      mostrarToast('Análisis completado', `Clasificación: ${resultado.clasificacion}`);
    }

    analizarBtn.textContent = 'Analizar';
    actualizarEstadoBoton(aparatosCtrl.getAparatos());
  }

  analizarBtn.addEventListener('click', onAnalizar);

  [horasPicoInput, perfilUsoSelect].forEach(el => {
    el.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') onAnalizar();
    });
  });
}
