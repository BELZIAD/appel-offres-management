import { Link } from 'react-router-dom';
import { useSettings } from '../state/SettingsContext';

export function AppFooter() {
  const { settings } = useSettings();
  return (
    <footer className="border-t border-line px-6 py-4 text-xs text-muted">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <span>{settings.applicationName}</span>
        <Link className="font-medium text-slate-700 hover:text-ink" to="/about">A propos</Link>
      </div>
    </footer>
  );
}
