// Fichier externe car la CSP prod (`script-src 'self'`) bloque tout script inline.
if (localStorage.getItem('nml-theme') === 'light') {
  document.documentElement.setAttribute('data-theme', 'light');
}
