import { register } from '../services/authApi.js';
import { isAuthenticated } from '../utils/tokenStorage.js';
import { initThemeToggle } from '../components/header.js';

if (isAuthenticated()) {
  window.location.replace('/index.html');
}

initThemeToggle();

const form = document.getElementById('registerForm');
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
  const confirm = document.getElementById('confirmPassword').value;

  if (loginName.length < 3) {
    showError('El usuario debe tener al menos 3 caracteres.');
    return;
  }

  if (password.length < 8) {
    showError('La contraseña debe tener al menos 8 caracteres.');
    return;
  }

  if (password !== confirm) {
    showError('Las contraseñas no coinciden.');
    return;
  }

  submitBtn.disabled = true;
  submitBtn.textContent = 'Registrando...';

  try {
    await register(loginName, password);
    window.location.href = '/index.html';
  } catch (err) {
    showError(err.message || 'No se pudo registrar.');
    submitBtn.disabled = false;
    submitBtn.textContent = 'Crear cuenta';
  }
});
