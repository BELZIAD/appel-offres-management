import { ChangeEvent, FormEvent, useEffect, useState } from 'react';
import { Image, KeyRound, LogOut, Monitor, Save, Trash2, Upload, UserRound } from 'lucide-react';
import { api, assetUrl, errorMessage } from '../api/client';
import { CompanySettings, Profile, User } from '../api/types';
import { useAuth } from '../state/AuthContext';
import { useSettings } from '../state/SettingsContext';
import { Toast } from '../ui/Toast';
import { applyTheme } from '../state/theme';

const defaultPreferences: Profile['preferences'] = {
  theme: 'SYSTEM',
  language: 'fr',
  emailNotifications: true,
  browserNotifications: true,
  timezone: 'Africa/Casablanca',
  dateFormat: 'dd/MM/yyyy',
  timeFormat: 'HH:mm',
  pageSize: 20
};

function profileFromUser(user: User): Profile {
  return {
    id: user.id,
    firstName: user.firstName,
    lastName: user.lastName,
    email: user.email,
    role: user.role,
    language: 'fr',
    timezone: 'Africa/Casablanca',
    preferences: defaultPreferences
  };
}

export function ProfilePage() {
  const { user } = useAuth();
  const { refresh } = useSettings();
  const [profile, setProfile] = useState<Profile | null>(() => user ? profileFromUser(user) : null);
  const [company, setCompany] = useState<CompanySettings | null>(null);
  const [loading, setLoading] = useState(!user);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [saving, setSaving] = useState(false);
  const [savingCompany, setSavingCompany] = useState(false);

  useEffect(() => {
    let mounted = true;
    setLoading(!profile);
    Promise.all([
      api.get<Profile>('/profile'),
      user?.role === 'ADMIN' ? api.get<CompanySettings>('/admin/company') : Promise.resolve(null)
    ])
      .then(([profileResponse, companyResponse]) => {
        if (!mounted) return;
        setProfile(profileResponse.data);
        if (companyResponse) setCompany(companyResponse.data);
      })
      .catch(err => {
        if (mounted) setToast({ type: 'error', message: errorMessage(err) });
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => { mounted = false; };
  }, []);

  function setField(key: keyof Profile, value: string) {
    setProfile(current => current ? { ...current, [key]: value } : current);
  }

  function setPreference(key: keyof Profile['preferences'], value: string | number | boolean) {
    setProfile(current => current ? { ...current, preferences: { ...current.preferences, [key]: value } } : current);
  }

  function setCompanyValue(key: keyof CompanySettings, value: string) {
    setCompany(current => current ? { ...current, [key]: value } : current);
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!profile) return;
    setSaving(true);
    try {
      const saved = await api.put<Profile>('/profile/full', {
        profile: {
          firstName: profile.firstName,
          lastName: profile.lastName,
          jobTitle: profile.jobTitle,
          phone: profile.phone,
          language: profile.language,
          timezone: profile.timezone
        },
        preferences: profile.preferences
      });
      setProfile(saved.data);
      applyTheme(saved.data.preferences?.theme);
      setToast({ type: 'success', message: 'Profil enregistre' });
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  async function saveCompany(event?: FormEvent) {
    event?.preventDefault();
    if (!company) return;
    setSavingCompany(true);
    try {
      const response = await api.put<CompanySettings>('/admin/company', company);
      setCompany(response.data);
      await refresh();
      setToast({ type: 'success', message: 'Informations application enregistrees' });
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    } finally {
      setSavingCompany(false);
    }
  }

  async function upload(type: 'photo' | 'signature', event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    const data = new FormData();
    data.append('file', file);
    try {
      const response = await api.post<{ url: string }>(`/profile/assets/${type}`, data);
      setProfile(current => current ? { ...current, [type]: response.data.url } : current);
      setToast({ type: 'success', message: 'Fichier mis a jour' });
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    } finally {
      event.target.value = '';
    }
  }

  async function removeProfileAsset(type: 'photo' | 'signature') {
    try {
      const response = await api.delete<Profile>(`/profile/assets/${type}`);
      setProfile(response.data);
      setToast({ type: 'success', message: type === 'photo' ? 'Photo supprimee' : 'Signature supprimee' });
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    }
  }

  async function uploadCompanyAsset(type: 'logo' | 'logo-dark' | 'favicon', event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    const data = new FormData();
    data.append('file', file);
    try {
      await api.post(`/admin/company/assets/${type}`, data);
      const response = await api.get<CompanySettings>('/admin/company');
      setCompany(response.data);
      await refresh();
      setToast({ type: 'success', message: 'Image application mise a jour' });
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    } finally {
      event.target.value = '';
    }
  }

  async function removeCompanyAsset(type: 'logo' | 'logo-dark' | 'favicon') {
    try {
      const response = await api.delete<CompanySettings>(`/admin/company/assets/${type}`);
      setCompany(response.data);
      await refresh();
      setToast({ type: 'success', message: 'Image application supprimee' });
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    }
  }

  if (!profile) return <div className="panel">Chargement...</div>;

  return (
    <div className="space-y-5">
      {toast && <Toast type={toast.type} message={toast.message} />}
      <form onSubmit={save} className="space-y-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Mon profil</h1>
          <p className="text-sm text-muted">{loading ? 'Synchronisation du profil...' : profile.email}</p>
        </div>
        <button className="btn-primary" disabled={saving}><Save size={16} /> {saving ? 'Enregistrement...' : 'Enregistrer'}</button>
        </div>

        <section className="panel grid gap-5 lg:grid-cols-[240px_1fr]">
        <div className="space-y-4">
          <div className="flex h-32 w-32 items-center justify-center overflow-hidden rounded-full bg-slate-100">
            {profile.photo ? <img src={assetUrl(profile.photo)} alt="Photo profil" className="h-full w-full object-cover" /> : <UserRound size={42} className="text-slate-400" />}
          </div>
          <label className="btn-secondary w-full"><Upload size={16} /> Photo<input className="hidden" type="file" accept=".png,.jpg,.jpeg" onChange={event => upload('photo', event)} /></label>
          {profile.photo && <button type="button" className="btn-secondary w-full text-red-700" onClick={() => removeProfileAsset('photo')}><Trash2 size={16} /> Supprimer photo</button>}
          <label className="btn-secondary w-full"><Upload size={16} /> Signature<input className="hidden" type="file" accept=".png,.jpg,.jpeg,.svg" onChange={event => upload('signature', event)} /></label>
          {profile.signature && <img src={assetUrl(profile.signature)} alt="Signature" className="max-h-20 rounded-md border border-line bg-white p-2" />}
          {profile.signature && <button type="button" className="btn-secondary w-full text-red-700" onClick={() => removeProfileAsset('signature')}><Trash2 size={16} /> Supprimer signature</button>}
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <label className="block"><span className="label">Prenom</span><input className="input mt-1" value={profile.firstName} onChange={e => setField('firstName', e.target.value)} /></label>
          <label className="block"><span className="label">Nom</span><input className="input mt-1" value={profile.lastName} onChange={e => setField('lastName', e.target.value)} /></label>
          <label className="block"><span className="label">Email</span><input className="input mt-1 bg-slate-50" value={profile.email} disabled /></label>
          <label className="block"><span className="label">Poste</span><input className="input mt-1" value={profile.jobTitle || ''} onChange={e => setField('jobTitle', e.target.value)} /></label>
          <label className="block"><span className="label">Telephone</span><input className="input mt-1" value={profile.phone || ''} onChange={e => setField('phone', e.target.value)} /></label>
        </div>
        </section>

        <section className="panel">
        <h2 className="mb-4 text-lg font-semibold">Preferences</h2>
        <div className="grid gap-4 md:grid-cols-3">
          <label className="block"><span className="label">Theme</span><select className="input mt-1" value={profile.preferences.theme} onChange={e => setPreference('theme', e.target.value)}><option value="LIGHT">Clair</option><option value="DARK">Sombre</option><option value="SYSTEM">Systeme</option></select></label>
          <label className="mt-7 flex items-center gap-2 text-sm"><input type="checkbox" checked={profile.preferences.emailNotifications} onChange={e => setPreference('emailNotifications', e.target.checked)} /> Notifications email</label>
          <label className="mt-7 flex items-center gap-2 text-sm"><input type="checkbox" checked={profile.preferences.browserNotifications} onChange={e => setPreference('browserNotifications', e.target.checked)} /> Notifications navigateur</label>
        </div>
        </section>
      </form>

      <SecuritySection onMessage={setToast} />

      {user?.role === 'ADMIN' && company && (
        <form onSubmit={saveCompany} className="space-y-5">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-2xl font-semibold">Application</h2>
              <p className="text-sm text-muted">Informations visibles dans l'application.</p>
            </div>
            <button className="btn-primary" disabled={savingCompany}><Save size={16} /> {savingCompany ? 'Enregistrement...' : 'Enregistrer application'}</button>
          </div>

          <section className="panel">
            <h3 className="mb-4 text-lg font-semibold">Images</h3>
            <div className="grid gap-4 md:grid-cols-3">
              {[
                { type: 'logo' as const, label: 'Logo', value: company.companyLogo },
                { type: 'logo-dark' as const, label: 'Logo sombre', value: company.companyLogoDark },
                { type: 'favicon' as const, label: 'Favicon', value: company.favicon }
              ].map(item => (
                <div key={item.type} className="rounded-md border border-line p-4">
                  <span className="mb-3 flex items-center gap-2 text-sm font-medium"><Image size={16} /> {item.label}</span>
                  <div className="mb-3 flex h-20 items-center justify-center rounded-md bg-slate-50">
                    {item.value ? <img src={assetUrl(item.value)} alt={item.label} className="max-h-16 max-w-full object-contain" /> : <span className="text-sm text-muted">Aucun fichier</span>}
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <label className="btn-secondary flex-1"><Upload size={16} /> Televerser<input className="hidden" type="file" accept=".png,.jpg,.jpeg,.svg,.ico" onChange={event => uploadCompanyAsset(item.type, event)} /></label>
                    {item.value && <button type="button" className="btn-secondary text-red-700" onClick={() => removeCompanyAsset(item.type)}><Trash2 size={16} /></button>}
                  </div>
                </div>
              ))}
            </div>
          </section>

          <section className="panel">
            <h3 className="mb-4 text-lg font-semibold">Informations</h3>
            <div className="grid gap-4 md:grid-cols-2">
              <label className="block"><span className="label">Nom application</span><input className="input mt-1" value={company.applicationName} onChange={event => setCompanyValue('applicationName', event.target.value)} /></label>
              <label className="block"><span className="label">Email</span><input className="input mt-1" type="email" value={company.email || ''} onChange={event => setCompanyValue('email', event.target.value)} /></label>
              <label className="block"><span className="label">Telephone</span><input className="input mt-1" value={company.phone || ''} onChange={event => setCompanyValue('phone', event.target.value)} /></label>
              <label className="block"><span className="label">Site web</span><input className="input mt-1" value={company.website || ''} onChange={event => setCompanyValue('website', event.target.value)} /></label>
              <label className="block"><span className="label">Ville</span><input className="input mt-1" value={company.city || ''} onChange={event => setCompanyValue('city', event.target.value)} /></label>
              <label className="block md:col-span-2"><span className="label">Adresse</span><input className="input mt-1" value={company.address || ''} onChange={event => setCompanyValue('address', event.target.value)} /></label>
            </div>
          </section>
        </form>
      )}
    </div>
  );
}

interface SessionItem { id: string; ipAddress?: string; userAgent?: string; loginAt?: string; lastSeenAt?: string; expiresAt?: string; active: boolean }

function SecuritySection({ onMessage }: { onMessage: (toast: { type: 'success' | 'error'; message: string }) => void }) {
  const { logout } = useAuth();
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [sessions, setSessions] = useState<SessionItem[]>([]);
  const loadSessions = () => api.get<{ sessions: SessionItem[] }>('/profile/sessions').then(r => setSessions(r.data.sessions.filter(s => s.active))).catch(() => undefined);
  useEffect(() => { loadSessions(); }, []);

  async function changePassword(event: FormEvent) {
    event.preventDefault();
    if (passwords.newPassword !== passwords.confirm) {
      onMessage({ type: 'error', message: 'La confirmation ne correspond pas au nouveau mot de passe' });
      return;
    }
    try {
      await api.put('/profile/password', { currentPassword: passwords.currentPassword, newPassword: passwords.newPassword });
      setPasswords({ currentPassword: '', newPassword: '', confirm: '' });
      onMessage({ type: 'success', message: 'Mot de passe modifie' });
    } catch (e) { onMessage({ type: 'error', message: errorMessage(e) }); }
  }

  async function revoke(id: string) {
    try { await api.delete(`/profile/sessions/${id}`); loadSessions(); } catch (e) { onMessage({ type: 'error', message: errorMessage(e) }); }
  }

  async function logoutEverywhere() {
    if (!window.confirm('Deconnecter toutes vos sessions (y compris celle-ci) ?')) return;
    try { await api.post('/auth/logout-all'); } catch { /* deja deconnecte */ }
    await logout();
  }

  return (
    <section className="panel grid gap-6 lg:grid-cols-2">
      <form className="space-y-3" onSubmit={changePassword}>
        <h2 className="flex items-center gap-2 text-lg font-semibold"><KeyRound size={18} /> Mot de passe</h2>
        <label className="block"><span className="label">Mot de passe actuel</span><input className="input mt-1" type="password" autoComplete="current-password" required value={passwords.currentPassword} onChange={e => setPasswords({ ...passwords, currentPassword: e.target.value })} /></label>
        <label className="block"><span className="label">Nouveau mot de passe</span><input className="input mt-1" type="password" autoComplete="new-password" required minLength={8} value={passwords.newPassword} onChange={e => setPasswords({ ...passwords, newPassword: e.target.value })} /></label>
        <label className="block"><span className="label">Confirmer</span><input className="input mt-1" type="password" autoComplete="new-password" required value={passwords.confirm} onChange={e => setPasswords({ ...passwords, confirm: e.target.value })} /></label>
        <p className="text-xs text-muted">8 caracteres minimum, avec majuscule, minuscule, chiffre et caractere special.</p>
        <button className="btn-primary"><Save size={16} /> Changer le mot de passe</button>
      </form>
      <div className="space-y-3">
        <div className="flex items-center justify-between gap-2">
          <h2 className="flex items-center gap-2 text-lg font-semibold"><Monitor size={18} /> Sessions actives</h2>
          <button type="button" className="btn-secondary text-red-700" onClick={logoutEverywhere}><LogOut size={16} /> Tout deconnecter</button>
        </div>
        <ul className="space-y-2">
          {sessions.map(s => (
            <li key={s.id} className="flex items-center justify-between gap-3 rounded-md border border-line p-3 text-sm">
              <div className="min-w-0">
                <div className="truncate font-medium">{describeAgent(s.userAgent)}</div>
                <div className="text-xs text-muted">{s.ipAddress || 'IP inconnue'} - derniere activite {s.lastSeenAt ? new Date(s.lastSeenAt).toLocaleString('fr-FR') : '-'}</div>
              </div>
              <button type="button" className="btn-secondary" onClick={() => revoke(s.id)}>Revoquer</button>
            </li>
          ))}
          {sessions.length === 0 && <li className="text-sm text-muted">Aucune session active.</li>}
        </ul>
      </div>
    </section>
  );
}

function describeAgent(agent?: string) {
  if (!agent) return 'Appareil inconnu';
  const browser = /Edg\//.test(agent) ? 'Edge' : /Chrome\//.test(agent) ? 'Chrome' : /Firefox\//.test(agent) ? 'Firefox' : /Safari\//.test(agent) ? 'Safari' : 'Navigateur';
  const os = /Windows/.test(agent) ? 'Windows' : /Android/.test(agent) ? 'Android' : /iPhone|iPad/.test(agent) ? 'iOS' : /Mac OS/.test(agent) ? 'macOS' : /Linux/.test(agent) ? 'Linux' : '';
  return os ? `${browser} sur ${os}` : browser;
}
