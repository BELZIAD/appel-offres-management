import { FormEvent, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, errorMessage } from '../../api/client';
import { Employee } from '../../api/types';
import { Toast } from '../../ui/Toast';

export function EmployeeFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const editing = Boolean(id);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '', enabled: true });

  useEffect(() => {
    if (id) api.get<Employee>(`/admin/employees/${id}`).then(r => setForm({ firstName: r.data.firstName, lastName: r.data.lastName, email: r.data.email, password: '', enabled: r.data.enabled })).catch(e => setToast({ type: 'error', message: errorMessage(e) }));
  }, [id]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    try {
      const payload = { ...form, password: form.password || undefined };
      if (editing) await api.put(`/admin/employees/${id}`, payload); else await api.post('/admin/employees', payload);
      navigate('/admin/employees');
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }

  return (
    <div className="max-w-3xl space-y-5">
      {toast && <Toast {...toast} />}
      <h1 className="text-2xl font-semibold">{editing ? 'Modifier employe' : 'Nouvel employe'}</h1>
      <form className="panel grid gap-4 sm:grid-cols-2" onSubmit={submit}>
        <label><span className="label">Prenom</span><input className="input mt-1" required value={form.firstName} onChange={e => setForm({ ...form, firstName: e.target.value })} /></label>
        <label><span className="label">Nom</span><input className="input mt-1" required value={form.lastName} onChange={e => setForm({ ...form, lastName: e.target.value })} /></label>
        <label className="sm:col-span-2"><span className="label">Email</span><input className="input mt-1" required value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} /></label>
        <label><span className="label">Mot de passe</span><input className="input mt-1" type="password" required={!editing} minLength={8} value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} /><span className="mt-1 block text-xs text-muted">Minimum 8 caracteres.</span></label>
        <label className="flex items-center gap-2 pt-7 text-sm"><input type="checkbox" checked={form.enabled} onChange={e => setForm({ ...form, enabled: e.target.checked })} /> Actif</label>
        <div className="flex justify-end gap-2 sm:col-span-2"><button type="button" className="btn-secondary" onClick={() => navigate('/admin/employees')}>Annuler</button><button className="btn-primary">Enregistrer</button></div>
      </form>
    </div>
  );
}
