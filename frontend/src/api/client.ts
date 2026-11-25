import axios from 'axios';

let accessToken: string | null = null;
let refreshPromise: Promise<RefreshResult | null> | null = null;

export interface RefreshResult { token: string; user: any }

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  withCredentials: true
});

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function getAccessToken() {
  return accessToken;
}

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (!axios.isAxiosError(error) || error.response?.status !== 401 || error.config?.url === '/auth/refresh' || error.config?.url === '/auth/login') {
      return Promise.reject(error);
    }
    const original = error.config as typeof error.config & { _retry?: boolean };
    if (original?._retry) return Promise.reject(error);
    original._retry = true;
    const refreshed = await refreshSession();
    const token = refreshed?.token;
    if (!token) {
      if (!window.location.pathname.startsWith('/login')) window.location.href = '/login';
      return Promise.reject(error);
    }
    original.headers = original.headers ?? {};
    original.headers.Authorization = `Bearer ${token}`;
    return api(original);
  }
);

/**
 * Rafraichit la session une seule fois a la fois : les appels concurrents (meme onglet)
 * partagent la meme requete, et un verrou navigateur serialise les onglets entre eux.
 * Sans cela, deux rafraichissements simultanes utilisaient le meme jeton : le second
 * echouait et l'utilisateur etait deconnecte.
 */
export function refreshSession(): Promise<RefreshResult | null> {
  if (refreshPromise) return refreshPromise;
  const doRefresh = () => api.post<RefreshResult>('/auth/refresh').then((response) => response.data);
  const locks = (navigator as unknown as { locks?: { request: (name: string, cb: () => Promise<RefreshResult>) => Promise<unknown> } }).locks;
  const run: Promise<RefreshResult> = locks ? (locks.request('ao-auth-refresh', doRefresh) as Promise<RefreshResult>) : doRefresh();
  refreshPromise = run
    .then((data) => {
      setAccessToken(data.token);
      return data;
    })
    .catch(() => {
      setAccessToken(null);
      return null;
    })
    .finally(() => {
      refreshPromise = null;
    });
  return refreshPromise;
}

export function errorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data;
    const validationErrors = data?.validationErrors;
    if (validationErrors && typeof validationErrors === 'object') {
      const details = Object.values(validationErrors).filter(Boolean).join(', ');
      if (details) return `${data?.message ?? 'Donnees invalides'}: ${details}`;
    }
    return data?.message ?? 'Erreur de communication avec le serveur';
  }
  return 'Erreur inattendue';
}

export function assetUrl(url?: string | null) {
  if (!url) return '';
  if (/^https?:\/\//i.test(url)) return url;
  const baseURL = api.defaults.baseURL;
  if (typeof baseURL === 'string' && /^https?:\/\//i.test(baseURL) && url.startsWith('/api/')) {
    return new URL(url.replace(/^\/api\//, ''), baseURL.endsWith('/') ? baseURL : `${baseURL}/`).toString();
  }
  return url;
}
