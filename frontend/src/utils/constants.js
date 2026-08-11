export const DEFAULT = {
  horasPico: 5,
  perfil: 'comercio'
};

export const APARATOS_POR_DEFECTO = [
  { nombre: 'Refrigerador', consumo: 60, cantidad: 1 },
  { nombre: 'Lavadora', consumo: 30, cantidad: 1 },
  { nombre: 'Microondas', consumo: 20, cantidad: 1 },
  { nombre: 'Televisor', consumo: 25, cantidad: 1 },
  { nombre: 'Computadora', consumo: 30, cantidad: 1 },
  { nombre: 'Router', consumo: 10, cantidad: 1 },
  { nombre: 'Aire acondicionado', consumo: 150, cantidad: 1 },
  { nombre: 'Bombillas LED', consumo: 15, cantidad: 5 }
];

export const APARATOS_KEY = 'energia_aparatos_v2';

export const PERFIL_INMUEBLE_MAP = {
  hogar: 'Casa',
  oficina: 'Local Comercial',
  comercio: 'Local Comercial',
  mixto: 'Casa Grande'
};

export function capitalizar(str) {
  return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
}

export function corregirNegativos(value) {
  const num = parseFloat(value);
  return isNaN(num) || num < 0 ? 0 : num;
}
