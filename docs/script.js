'use strict';

const dialog = document.querySelector('#screenshot-dialog');
const dialogImage = document.querySelector('#dialog-image');
const dialogTitle = document.querySelector('#dialog-title');
let opener;

// Links work as ordinary image links if JavaScript or <dialog> is unavailable.
if (typeof dialog.showModal === 'function') {
  document.querySelectorAll('[data-screenshot]').forEach(link => {
    link.addEventListener('click', event => {
      event.preventDefault();
      opener = link;
      const image = link.querySelector('img');
      dialogTitle.textContent = link.dataset.title;
      dialogImage.src = link.getAttribute('href');
      dialogImage.alt = image ? image.alt : `${link.dataset.title} screenshot`;
      dialog.showModal();
      document.body.classList.add('dialog-open');
    });
  });
  document.querySelector('#close-dialog').addEventListener('click', () => dialog.close());
  dialog.addEventListener('click', event => {
    const rect = dialog.getBoundingClientRect();
    if (event.target === dialog && (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom)) dialog.close();
  });
  dialog.addEventListener('close', () => {
    document.body.classList.remove('dialog-open');
    opener?.focus({ preventScroll: true });
  });
}

const systemMotion = matchMedia('(prefers-reduced-motion: reduce)');
const motionToggle = document.querySelector('#motion-toggle');
let manualReduction = false;
let observer;
function updateMotion() {
  const reduced = manualReduction || systemMotion.matches;
  document.documentElement.classList.toggle('reduce-motion', reduced);
  document.documentElement.classList.toggle('motion-on', !reduced);
  motionToggle.setAttribute('aria-pressed', String(reduced));
  motionToggle.textContent = systemMotion.matches ? 'Reduced motion (system)' : reduced ? 'Reduced motion on' : 'Reduce motion';
  motionToggle.disabled = systemMotion.matches;
  observer?.disconnect();
  // Content stays visible by default, including browsers without the observer.
  if (!reduced && 'IntersectionObserver' in window) {
    observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.08 });
    document.querySelectorAll('.reveal').forEach(element => observer.observe(element));
  } else {
    document.querySelectorAll('.reveal').forEach(element => element.classList.add('visible'));
  }
}
motionToggle.addEventListener('click', () => { manualReduction = !manualReduction; updateMotion(); });
systemMotion.addEventListener('change', updateMotion);
updateMotion();
