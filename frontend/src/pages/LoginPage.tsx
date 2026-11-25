import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { assetUrl, errorMessage } from '../api/client';
import { useAuth } from '../state/AuthContext';
import { useSettings } from '../state/SettingsContext';
import { Toast } from '../ui/Toast';
import { ROUTES } from '../routes/paths';

export function LoginPage() {
  const { login } = useAuth();
  const { settings } = useSettings();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      const role = await login(email, password);
      navigate(role === 'ADMIN' ? ROUTES.ADMIN.DASHBOARD : ROUTES.EMPLOYEE.DASHBOARD);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      {error && <Toast type="error" message={error} />}
      <form onSubmit={submit} className="w-full max-w-md rounded-lg border border-line bg-white p-7 shadow-sm">
        <div className="mb-7 text-center">
          {settings.companyLogo && <img src={assetUrl(settings.companyLogo)} alt={settings.applicationName} className="mx-auto mb-4 max-h-16 max-w-56 object-contain" />}
          <h1 className="text-2xl font-semibold">Connexion</h1>
          <p className="mt-1 text-sm text-muted">{settings.applicationName}</p>
        </div>
        <div className="space-y-4">
          <label className="block"><span className="label">Email</span><input className="input mt-1" value={email} onChange={(e) => setEmail(e.target.value)} /></label>
          <label className="block"><span className="label">Mot de passe</span><input className="input mt-1" type="password" value={password} onChange={(e) => setPassword(e.target.value)} /></label>
          <button className="btn-primary w-full" disabled={loading}>{loading ? 'Connexion...' : 'Se connecter'}</button>
        </div>
      </form>
    </div>
  );
}
