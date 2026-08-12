import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  saveTokens
} from '../utils/tokenStorage.js';

const API_BASE = '/api';

let refreshPromise = null;

async function refreshAccessToken() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    throw new Error('Sin refresh token');
  }

  const response = await fetch(`${API_BASE}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });

  if (!response.ok) {
    throw new Error(`Refresh falló: ${response.status}`);
  }

  const data = await response.json();
  saveTokens({
    accessToken: data.accessToken,
    refreshToken: data.refreshToken
  });
  return data.accessToken;
}

function redirectToLogin() {
  clearTokens();
  const path = window.location.pathname || '';
  if (!path.endsWith('/login.html') && !path.endsWith('/register.html')) {
    window.location.href = '/login.html';
  }
}

export async function apiFetch(path, options = {}, retry = true) {
  const headers = new Headers(options.headers || {});
  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json');
  }

  const accessToken = getAccessToken();
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }

  const url = path.startsWith('http') ? path : `${API_BASE}${path.startsWith('/') ? path : `/${path}`}`;
  const response = await fetch(url, { ...options, headers });

  if (response.status !== 401 || !retry) {
    return response;
  }

  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    redirectToLogin();
    return response;
  }

  try {
    if (!refreshPromise) {
      refreshPromise = refreshAccessToken().finally(() => {
        refreshPromise = null;
      });
    }
    await refreshPromise;
    return apiFetch(path, options, false);
  } catch {
    redirectToLogin();
    return response;
  }
}
