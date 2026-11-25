import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api, assetUrl } from '../api/client';
import { CompanySettings } from '../api/types';

const defaults: CompanySettings = {
  id: 0,
  companyName: null,
  applicationName: "Gestion des Appels d'Offres",
  primaryColor: '#172033',
  secondaryColor: '#475569',
  accentColor: '#0ea5e9',
  dangerColor: '#dc2626',
  warningColor: '#d97706',
  successColor: '#059669',
  sidebarColor: '#ffffff',
  headerColor: '#ffffff',
  country: 'Maroc',
  currency: 'MAD',
  timezone: 'Africa/Casablanca',
  language: 'fr'
};

interface SettingsContextValue {
  settings: CompanySettings;
  loading: boolean;
  refresh: () => Promise<void>;
}

const SettingsContext = createContext<SettingsContextValue | null>(null);

function applyBranding(settings: CompanySettings) {
  const root = document.documentElement;
  root.style.setProperty('--color-primary', settings.primaryColor || defaults.primaryColor);
  root.style.setProperty('--color-secondary', settings.secondaryColor || defaults.secondaryColor || '#475569');
  root.style.setProperty('--color-accent', settings.accentColor || defaults.accentColor || '#0ea5e9');
  root.style.setProperty('--color-danger', settings.dangerColor || defaults.dangerColor || '#dc2626');
  root.style.setProperty('--color-warning', settings.warningColor || defaults.warningColor || '#d97706');
  root.style.setProperty('--color-success', settings.successColor || defaults.successColor || '#059669');
  root.style.setProperty('--color-sidebar', settings.sidebarColor || defaults.sidebarColor || '#ffffff');
  root.style.setProperty('--color-header', settings.headerColor || defaults.headerColor || '#ffffff');
  document.title = settings.applicationName || defaults.applicationName;

  if (settings.favicon) {
    let link = document.querySelector<HTMLLinkElement>("link[rel='icon']");
    if (!link) {
      link = document.createElement('link');
      link.rel = 'icon';
      document.head.appendChild(link);
    }
    link.href = assetUrl(settings.favicon);
  }
}

export function SettingsProvider({ children }: { children: React.ReactNode }) {
  const [settings, setSettings] = useState<CompanySettings>(defaults);
  const [loading, setLoading] = useState(true);

  async function refresh() {
    const response = await api.get<CompanySettings>('/company-settings/public');
    const next = { ...defaults, ...response.data };
    setSettings(next);
    applyBranding(next);
  }

  useEffect(() => {
    refresh()
      .catch(() => applyBranding(defaults))
      .finally(() => setLoading(false));
  }, []);

  const value = useMemo(() => ({ settings, loading, refresh }), [settings, loading]);
  return <SettingsContext.Provider value={value}>{children}</SettingsContext.Provider>;
}

export function useSettings() {
  const value = useContext(SettingsContext);
  if (!value) {
    throw new Error('SettingsContext manquant');
  }
  return value;
}
