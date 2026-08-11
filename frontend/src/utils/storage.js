import { APARATOS_KEY, APARATOS_POR_DEFECTO } from './constants.js';

export function cargarAparatos() {
  try {
    const data = localStorage.getItem(APARATOS_KEY);
    if (data) {
      return JSON.parse(data);
    }
    return JSON.parse(JSON.stringify(APARATOS_POR_DEFECTO));
  } catch {
    return JSON.parse(JSON.stringify(APARATOS_POR_DEFECTO));
  }
}

export function guardarAparatos(aparatos) {
  localStorage.setItem(APARATOS_KEY, JSON.stringify(aparatos));
}
