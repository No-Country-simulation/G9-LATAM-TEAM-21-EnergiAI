import { initApp } from './app.js';
import { isAuthenticated } from './utils/tokenStorage.js';
import { initLogoutButton } from './components/header.js';

if (!isAuthenticated()) {
  window.location.replace('/login.html');
} else {
  initLogoutButton();
  initApp();
}
