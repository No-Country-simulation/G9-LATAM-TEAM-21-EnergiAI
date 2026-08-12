import { logout } from '../services/authApi.js';

export function initThemeToggle() {
  const themeToggle = document.getElementById('themeToggle');
  const themeIcon = document.getElementById('themeIcon');
  const themeLabel = document.getElementById('themeLabel');

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

  themeToggle.addEventListener('click', toggleTheme);
}

export function initLogoutButton() {
  const logoutBtn = document.getElementById('logoutBtn');
  if (!logoutBtn) return;

  logoutBtn.addEventListener('click', async () => {
    logoutBtn.disabled = true;
    logoutBtn.textContent = 'Saliendo...';
    await logout();
    window.location.href = '/login.html';
  });
}
