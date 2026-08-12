import { apiFetch } from './http.js';
import {
  clearTokens,
  getRefreshToken,
  saveTokens
} from '../utils/tokenStorage.js';

async function parseError(response) {
  try {
    const data = await response.json();
    if (data.detalle) return data.detalle;
    if (data.error) return data.error;
    if (data.detalles) {
      return Object.values(data.detalles).join('. ');
    }
    return JSON.stringify(data);
  } catch {
    return `Error ${response.status}`;
  }
}

export async function login(loginName, password) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ login: loginName, password })
  });

  if (!response.ok) {
    throw new Error(await parseError(response));
  }

  const data = await response.json();
  saveTokens({
    accessToken: data.accessToken,
    refreshToken: data.refreshToken
  });
  return data;
}

export async function register(loginName, password) {
  const response = await fetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ login: loginName, password })
  });

  if (!response.ok) {
    throw new Error(await parseError(response));
  }

  const data = await response.json();
  saveTokens({
    accessToken: data.accessToken,
    refreshToken: data.refreshToken
  });
  return data;
}

export async function logout() {
  const refreshToken = getRefreshToken();
  try {
    if (refreshToken) {
      await apiFetch('/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ refreshToken })
      }, false);
    }
  } catch {
    // limpiar sesión local aunque falle la red
  } finally {
    clearTokens();
  }
}
