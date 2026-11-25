import { ChangeEvent, FormEvent, useEffect, useState } from 'react';
import { Image, Save, Trash2, Upload } from 'lucide-react';
import { api, assetUrl, errorMessage } from '../../api/client';
import { CompanySettings } from '../../api/types';
import { useSettings } from '../../state/SettingsContext';
import { Toast } from '../../ui/Toast';

const fields: Array<{ key: keyof CompanySettings; label: string; type?: string }> = [
  { key: 'applicationName', label: 'Nom application' },
  { key: 'email', label: 'Email', type: 'email' },
  { key: 'phone', label: 'Telephone' },
  { key: 'website', label: 'Site web' },
  { key: 'address', label: 'Adresse' },
  { key: 'city', label: 'Ville' }
];

const colorFields: Array<{ key: keyof CompanySettings; label: string }> = [
  { key: 'primaryColor', label: 'Primaire' },
  { key: 'secondaryColor', label: 'Secondaire' },
  { key: 'accentColor', label: 'Accent' },
  { key: 'dangerColor', label: 'Danger' },
  { key: 'warningColor', label: 'Alerte' },
  { key: 'successColor', label: 'Succes' },
  { key: 'sidebarColor', label: 'Sidebar' },
  { key: 'headerColor', label: 'Header' }
];

export function CompanySettingsPage() {
  const { refresh } = useSettings();
  const [form, setForm] = useState<CompanySettings | null>(null);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api.get<CompanySettings>('/admin/company').then(response => setForm(response.data));
  }, []);

  function setValue(key: keyof CompanySettings, value: string) {
    setForm(current => current ? { ...current, [key]: value } : current);
  }

  async function save(event?: FormEvent) {
    event?.preventDefault();
    if (!form) return;
    setSaving(true);
    try {
      const response = await api.put<CompanySettings>('/admin/company', form);
      setForm(response.data);
      await refresh();
      setToast({ type: 'success', message: 'Parametres entreprise enregistres' });
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  async function upload(type: string, event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    const data = new FormData();
    data.append('file', file);
    try {
      await api.post(`/admin/company/assets/${type}`, data);
      const response = await api.get<CompanySettings>('/admin/company');
      setForm(response.data);
      await refresh();
      setToast({ type: 'success', message: 'Fichier mis a jour' });
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    } finally {
      event.target.value = '';
    }
  }

  async function removeAsset(type: string) {
    try {
      const response = await api.delete<CompanySettings>(`/admin/company/assets/${type}`);
      setForm(response.data);
      await refresh();
      setToast({ type: 'success', message: 'Image supprimee' });
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    }
  }

  if (!form) return <div className="panel">Chargement...</div>;

  return (
    <form onSubmit={save} className="space-y-5">
      {toast && <Toast type={toast.type} message={toast.message} />}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Entreprise</h1>
          <p className="text-sm text-muted">Identite, coordonnees, couleurs et actifs visuels.</p>
        </div>
        <button className="btn-primary" disabled={saving}><Save size={16} /> {saving ? 'Enregistrement...' : 'Enregistrer'}</button>
      </div>

      <section className="panel">
        <h2 className="mb-4 text-lg font-semibold">Actifs de marque</h2>
        <div className="grid gap-4 md:grid-cols-3">
          {[
            { type: 'logo', label: 'Logo', value: form.companyLogo },
            { type: 'logo-dark', label: 'Logo sombre', value: form.companyLogoDark },
            { type: 'favicon', label: 'Favicon', value: form.favicon }
          ].map(item => (
            <div key={item.type} className="rounded-md border border-line p-4">
              <span className="mb-3 flex items-center gap-2 text-sm font-medium"><Image size={16} /> {item.label}</span>
              <div className="mb-3 flex h-20 items-center justify-center rounded-md bg-slate-50">
                {item.value ? <img src={assetUrl(item.value)} alt={item.label} className="max-h-16 max-w-full object-contain" /> : <span className="text-sm text-muted">Aucun fichier</span>}
              </div>
              <div className="flex flex-wrap gap-2">
                <label className="btn-secondary flex-1"><Upload size={16} /> Televerser<input className="hidden" type="file" accept=".png,.jpg,.jpeg,.svg,.ico" onChange={event => upload(item.type, event)} /></label>
                {item.value && <button type="button" className="btn-secondary text-red-700" onClick={() => removeAsset(item.type)}><Trash2 size={16} /></button>}
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="panel">
        <h2 className="mb-4 text-lg font-semibold">Couleurs</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {colorFields.map(field => (
            <label key={field.key} className="block">
              <span className="label">{field.label}</span>
              <div className="mt-1 flex gap-2">
                <input className="h-10 w-14 rounded-md border border-line bg-white" type="color" value={(form[field.key] as string) || '#172033'} onChange={event => setValue(field.key, event.target.value)} />
                <input className="input" value={(form[field.key] as string) || ''} onChange={event => setValue(field.key, event.target.value)} />
              </div>
            </label>
          ))}
        </div>
      </section>

      <section className="panel">
        <h2 className="mb-4 text-lg font-semibold">Informations utiles</h2>
        <div className="grid gap-4 md:grid-cols-2">
          {fields.map(field => (
            <label key={field.key} className="block">
              <span className="label">{field.label}</span>
              <input className="input mt-1" type={field.type ?? 'text'} value={(form[field.key] as string) || ''} onChange={event => setValue(field.key, event.target.value)} />
            </label>
          ))}
        </div>
      </section>
    </form>
  );
}
