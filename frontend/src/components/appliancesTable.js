import { corregirNegativos } from '../utils/constants.js';
import { guardarAparatos } from '../utils/storage.js';
import { mostrarToast } from './toast.js';

export function calcularTotales(aparatos) {
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

export function crearAparatosController({ onChange }) {
  let aparatos = [];

  const aparatosBody = document.getElementById('aparatosBody');
  const totalAparatos = document.getElementById('totalAparatos');
  const totalConsumo = document.getElementById('totalConsumo');
  const aparatosCount = document.getElementById('aparatosCount');
  const agregarAparatoBtn = document.getElementById('agregarAparatoBtn');
  const resetAparatosBtn = document.getElementById('resetAparatosBtn');

  function getAparatos() {
    return aparatos;
  }

  function setAparatos(lista) {
    aparatos = lista;
    renderizarAparatos();
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
      input.addEventListener('change', function () {
        const index = parseInt(this.dataset.index);
        const field = this.dataset.field;
        let value = this.value;
        if (field === 'consumo' || field === 'cantidad') {
          value = corregirNegativos(value);
          this.value = value;
        }
        aparatos[index][field] = value;
        guardarAparatos(aparatos);
        renderizarAparatos();
        onChange?.();
      });
      input.addEventListener('input', function () {
        if (this.dataset.field === 'consumo' || this.dataset.field === 'cantidad') {
          if (parseFloat(this.value) < 0) this.value = 0;
        }
      });
    });

    document.querySelectorAll('.eliminar-aparato').forEach(btn => {
      btn.addEventListener('click', function () {
        const index = parseInt(this.dataset.index);
        aparatos.splice(index, 1);
        guardarAparatos(aparatos);
        renderizarAparatos();
        onChange?.();
      });
    });

    onChange?.();
  }

  function agregarAparato() {
    aparatos.push({ nombre: 'Nuevo aparato', consumo: 0, cantidad: 1 });
    guardarAparatos(aparatos);
    renderizarAparatos();
    const inputs = document.querySelectorAll('.input-nombre');
    if (inputs.length > 0) {
      inputs[inputs.length - 1].focus();
      inputs[inputs.length - 1].select();
    }
  }

  function resetearAparatos() {
    if (confirm('¿Vaciar la lista de aparatos?')) {
      aparatos = [];
      guardarAparatos(aparatos);
      renderizarAparatos();
      mostrarToast('Lista vaciada', 'Se eliminaron todos los aparatos');
    }
  }

  agregarAparatoBtn.addEventListener('click', agregarAparato);
  resetAparatosBtn.addEventListener('click', resetearAparatos);

  return { getAparatos, setAparatos, calcularTotales: () => calcularTotales(aparatos) };
}
