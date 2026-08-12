import { login } from '../services/authApi.js';
import { isAuthenticated } from '../utils/tokenStorage.js';
import { initThemeToggle } from '../components/header.js';

if (isAuthenticated()) {
  window.location.replace('/index.html');
}

initThemeToggle();

const form = document.getElementById('loginForm');
const errorBox = document.getElementById('authError');
const submitBtn = document.getElementById('submitBtn');

function showError(message) {
  errorBox.textContent = message;
  errorBox.hidden = false;
}

function hideError() {
  errorBox.hidden = true;
  errorBox.textContent = '';
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  hideError();

  const loginName = document.getElementById('login').value.trim();
  const password = document.getElementById('password').value;

  if (!loginName || !password) {
    showError('Completa usuario y contraseña.');
    return;
  }

  submitBtn.disabled = true;
  submitBtn.textContent = 'Ingresando...';

  try {
    await login(loginName, password);
    window.location.href = '/index.html';
  } catch (err) {
    showError(err.message || 'No se pudo iniciar sesión.');
    submitBtn.disabled = false;
    submitBtn.textContent = 'Iniciar sesión';
  }
});
