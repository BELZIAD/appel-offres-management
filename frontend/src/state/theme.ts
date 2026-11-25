import { api } from '../api/client';

const KEY = 'ao-theme';
let media: MediaQueryList | null = null;

function resolve(theme: string) {
  if (theme === 'DARK') return true;
  if (theme === 'LIGHT') return false;
  return typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches;
}

/** Applique le theme (LIGHT / DARK / SYSTEM) choisi dans les preferences du profil. */
export function applyTheme(theme?: string | null) {
  const value = theme || 'SYSTEM';
  try { localStorage.setItem(KEY, value); } catch { /* stockage indisponible */ }
  document.documentElement.classList.toggle('dark', !!resolve(value));
  if (!media && window.matchMedia) {
    media = window.matchMedia('(prefers-color-scheme: dark)');
    media.addEventListener?.('change', () => {
      let current = 'SYSTEM';
      try { current = localStorage.getItem(KEY) || 'SYSTEM'; } catch { /* ignore */ }
      if (current === 'SYSTEM') document.documentElement.classList.toggle('dark', media!.matches);
    });
  }
}

/** Theme memorise localement (application immediate au chargement, avant l'appel API). */
export function applyStoredTheme() {
  let stored: string | null = null;
  try { stored = localStorage.getItem(KEY); } catch { /* ignore */ }
  applyTheme(stored);
}

/** Recupere les preferences de l'utilisateur connecte et applique son theme. */
export function syncThemeFromProfile() {
  api.get<{ preferences?: { theme?: string } }>('/profile')
    .then(r => applyTheme(r.data.preferences?.theme))
    .catch(() => undefined);
}
