import { FormEvent, useEffect, useState } from 'react';
import { Edit, Plus, Save, Trash2, X } from 'lucide-react';
import { api, errorMessage } from '../../api/client';
import { PartnerCompany } from '../../api/types';
import { Toast } from '../../ui/Toast';

const emptyForm = { name: '', ice: '', rc: '', contactPerson: '', phone: '', email: '' };

export function PartnersPage() {
  const [partners, setPartners] = useState<PartnerCompany[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const load = () => api.get<PartnerCompany[]>('/admin/partners').then(r => setPartners(r.data));

  useEffect(() => { load(); }, []);

  async function submit(e: FormEvent) {
    e.preventDefault();
    try {
      if (editingId) {
        await api.put(`/admin/partners/${editingId}`, form);
        setToast({ type: 'success', message: 'Partenaire modifie' });
      } else {
        await api.post('/admin/partners', form);
        setToast({ type: 'success', message: 'Partenaire ajoute' });
      }
      reset();
      load();
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    }
  }

  function edit(p: PartnerCompany) {
    setEditingId(p.id);
    setForm({
      name: p.name,
      ice: p.ice || '',
      rc: p.rc || '',
      contactPerson: p.contactPerson || '',
      phone: p.phone || '',
      email: p.email || ''
    });
  }

  function reset() {
    setEditingId(null);
    setForm(emptyForm);
  }

  async function toggle(p: PartnerCompany) {
    try {
      await api.patch(`/admin/partners/${p.id}/status`, { active: !p.active });
      load();
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    }
  }

  async function remove(p: PartnerCompany) {
    if (!window.confirm(`Supprimer ${p.name} ?`)) return;
    try {
      await api.delete(`/admin/partners/${p.id}`);
      setToast({ type: 'success', message: 'Partenaire supprime' });
      if (editingId === p.id) reset();
      load();
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    }
  }

  return (
    <div className="space-y-5">
      {toast && <Toast {...toast} />}
      <h1 className="text-2xl font-semibold">Societes partenaires</h1>
      <form className="panel grid gap-3 md:grid-cols-3" onSubmit={submit}>
        <input className="input" required placeholder="Societe" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
        <input className="input" placeholder="ICE" value={form.ice} onChange={e => setForm({ ...form, ice: e.target.value })} />
        <input className="input" placeholder="RC" value={form.rc} onChange={e => setForm({ ...form, rc: e.target.value })} />
        <input className="input" placeholder="Contact" value={form.contactPerson} onChange={e => setForm({ ...form, contactPerson: e.target.value })} />
        <input className="input" placeholder="Telephone" value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} />
        <input className="input" placeholder="Email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
        <div className="flex flex-wrap gap-2 md:col-span-3">
          <button className="btn-primary">{editingId ? <Save size={16} /> : <Plus size={16} />} {editingId ? 'Enregistrer' : 'Ajouter partenaire'}</button>
          {editingId && <button type="button" className="btn-secondary" onClick={reset}><X size={16} /> Annuler</button>}
        </div>
      </form>
      <section className="panel overflow-x-auto">
        <table className="table">
          <thead><tr><th className="th">Societe</th><th className="th">ICE</th><th className="th">RC</th><th className="th">Contact</th><th className="th">Telephone</th><th className="th">Statut</th><th className="th">AO communs</th><th className="th">Actions</th></tr></thead>
          <tbody>{partners.map(p => <tr key={p.id}><td className="td">{p.name}</td><td className="td">{p.ice}</td><td className="td">{p.rc}</td><td className="td">{p.contactPerson}</td><td className="td">{p.phone}</td><td className="td">{p.active ? 'Actif' : 'Inactif'}</td><td className="td">{p.sharedTenderCount}</td><td className="td"><div className="flex flex-wrap gap-2"><button className="btn-secondary" onClick={() => edit(p)}><Edit size={16} /></button><button className="btn-secondary" onClick={() => toggle(p)}>{p.active ? 'Desactiver' : 'Reactiver'}</button><button className="btn-secondary text-red-700" onClick={() => remove(p)}><Trash2 size={16} /></button></div></td></tr>)}</tbody>
        </table>
      </section>
    </div>
  );
}
