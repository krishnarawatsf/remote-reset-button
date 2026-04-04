const tabs = Array.from(document.querySelectorAll('.tab'));
const panels = Array.from(document.querySelectorAll('.panel'));
const notesToggle = document.getElementById('notesToggle');
const slides = Array.from(document.querySelectorAll('.layout .card'));
const nav = document.getElementById('presenterNav');
const prevBtn = document.getElementById('prevSlide');
const nextBtn = document.getElementById('nextSlide');
const slideNow = document.getElementById('slideNow');
const slideTotal = document.getElementById('slideTotal');

const params = new URLSearchParams(window.location.search);
const isPresentMode = params.get('mode') === 'present';
let currentSlide = 0;

function setActive(targetId) {
  tabs.forEach((tab) => {
    const active = tab.dataset.target === targetId;
    tab.classList.toggle('active', active);
    tab.setAttribute('aria-selected', active ? 'true' : 'false');
  });

  panels.forEach((panel) => {
    panel.classList.toggle('active', panel.id === targetId);
  });
}

tabs.forEach((tab) => {
  tab.addEventListener('click', () => setActive(tab.dataset.target));
});

function setSlide(index) {
  if (!isPresentMode || !slides.length) return;

  currentSlide = Math.max(0, Math.min(index, slides.length - 1));

  slides.forEach((slide, i) => {
    slide.classList.toggle('active-slide', i === currentSlide);
  });

  if (slideNow) {
    slideNow.textContent = String(currentSlide + 1);
  }
}

if (isPresentMode) {
  document.body.classList.add('present-mode');

  if (nav) {
    nav.setAttribute('aria-hidden', 'false');
  }

  if (slideTotal) {
    slideTotal.textContent = String(slides.length);
  }

  setSlide(0);

  if (prevBtn) {
    prevBtn.addEventListener('click', () => setSlide(currentSlide - 1));
  }

  if (nextBtn) {
    nextBtn.addEventListener('click', () => setSlide(currentSlide + 1));
  }

  window.addEventListener('keydown', (event) => {
    if (event.key === 'ArrowRight') {
      setSlide(currentSlide + 1);
    }
    if (event.key === 'ArrowLeft') {
      setSlide(currentSlide - 1);
    }
    if (event.key.toLowerCase() === 'n') {
      document.body.classList.toggle('show-notes');
    }
  });
}

if (notesToggle) {
  notesToggle.addEventListener('click', () => {
    document.body.classList.toggle('show-notes');
  });
}
