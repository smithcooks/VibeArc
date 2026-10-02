const root = document.documentElement;
const themeButtons = document.querySelectorAll('[data-theme].theme-option');
const themeStatus = document.querySelector('.palette-status');
themeButtons.forEach(button => button.addEventListener('click', () => {
  root.dataset.theme = button.dataset.theme;
  themeButtons.forEach(option => option.setAttribute('aria-pressed', String(option === button)));
  themeStatus.textContent = `${button.dataset.theme[0].toUpperCase()}${button.dataset.theme.slice(1)} atmosphere selected.`;
}));

const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
const desktop = matchMedia('(min-width: 768px) and (pointer: fine)');
if (!reducedMotion.matches && desktop.matches) {
  let scheduled = false;
  addEventListener('scroll', () => {
    if (scheduled || document.hidden) return;
    scheduled = true;
    requestAnimationFrame(() => {
      root.style.setProperty('--scroll', `${reducedMotion.matches || !desktop.matches ? 0 : Math.min(scrollY, 1000)}px`);
      scheduled = false;
    });
  }, { passive: true });
  const resetMotion = () => root.style.setProperty('--scroll', '0px');
  reducedMotion.addEventListener('change', resetMotion);
  desktop.addEventListener('change', resetMotion);
}
