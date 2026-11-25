import { FormEvent, useEffect, useState } from 'react';
import { Edit, Plus, Save, Trash2, X } from 'lucide-react';
import { ChecklistTemplate, ChecklistTemplateItem, DocumentType } from '../../api/types';
import { api, errorMessage } from '../../api/client';
import { Toast } from '../../ui/Toast';

const documentTypes: Array<DocumentType | ''> = ['', 'RC', 'CPS', 'NOTICE', 'PRICE_SCHEDULE', 'TECHNICAL_DOCUMENT', 'ADMINISTRATIVE_DOCUMENT', 'FINANCIAL_DOCUMENT', 'OTHER'];
const emptyItem: ChecklistTemplateItem = { label: '', position: 1, required: true };
const emptyForm = { name: '', description: '', active: true, items: [emptyItem] };

export function ChecklistTemplatesPage() {
  const [templates, setTemplates] = useState<ChecklistTemplate[]>([]);
  const [form, setForm] = useState<{ name: string; description: string; active: boolean; items: ChecklistTemplateItem[] }>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const load = () => api.get<ChecklistTemplate[]>('/admin/checklist-templates').then(r => setTemplates(r.data));

  useEffect(() => { load(); }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    const items = form.items
      .filter(item => item.label.trim())
      .map((item, index) => ({ ...item, label: item.label.trim(), position: index + 1, expectedDocumentType: item.expectedDocumentType || undefined }));
    try {
      const payload = { ...form, items };
      if (editingId) {
        await api.put(`/admin/checklist-templates/${editingId}`, payload);
        setToast({ type: 'success', message: 'Modele modifie' });
      } else {
        await api.post('/admin/checklist-templates', payload);
        setToast({ type: 'success', message: 'Modele cree' });
      }
      reset();
      load();
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    }
  }

  function edit(template: ChecklistTemplate) {
    setEditingId(template.id);
    setForm({
      name: template.name,
      description: template.description || '',
      active: template.active,
      items: template.items.length ? template.items.map((item, index) => ({ ...item, position: index + 1 })) : [emptyItem]
    });
  }

  function reset() {
    setEditingId(null);
    setForm(emptyForm);
  }

  function setItem(index: number, value: Partial<ChecklistTemplateItem>) {
    setForm(current => ({ ...current, items: current.items.map((item, i) => i === index ? { ...item, ...value } : item) }));
  }

  function addItem() {
    setForm(current => ({ ...current, items: [...current.items, { ...emptyItem, position: current.items.length + 1 }] }));
  }

  function removeItem(index: number) {
    setForm(current => ({ ...current, items: current.items.filter((_, i) => i !== index).map((item, i) => ({ ...item, position: i + 1 })) }));
  }

  async function remove(template: ChecklistTemplate) {
    if (!window.confirm(`Supprimer le modele "${template.name}" ?`)) return;
    try {
      await api.delete(`/admin/checklist-templates/${template.id}`);
      setToast({ type: 'success', message: 'Modele supprime' });
      if (editingId === template.id) reset();
      load();
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    }
  }

  return (
    <div className="space-y-5">
      {toast && <Toast {...toast} />}
      <h1 className="text-2xl font-semibold">Modeles de checklist</h1>
      <form className="panel space-y-4" onSubmit={submit}>
        <div className="grid gap-3 md:grid-cols-3">
          <input className="input" required placeholder="Nom du modele" value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} />
          <input className="input md:col-span-2" placeholder="Description" value={form.description} onChange={event => setForm({ ...form, description: event.target.value })} />
          <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.active} onChange={event => setForm({ ...form, active: event.target.checked })} /> Actif</label>
        </div>
        <div className="space-y-2">
          {form.items.map((item, index) => (
            <div key={index} className="grid gap-2 rounded-md border border-line p-3 md:grid-cols-[1fr_180px_120px_auto]">
              <input className="input" required placeholder="Element de checklist" value={item.label} onChange={event => setItem(index, { label: event.target.value })} />
              <select className="input" value={item.expectedDocumentType || ''} onChange={event => setItem(index, { expectedDocumentType: event.target.value as DocumentType || undefined })}>{documentTypes.map(type => <option key={type || 'none'} value={type}>{type || 'Sans type'}</option>)}</select>
              <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={item.required} onChange={event => setItem(index, { required: event.target.checked })} /> Requis</label>
              <button type="button" className="btn-secondary text-red-700" onClick={() => removeItem(index)} disabled={form.items.length === 1}><Trash2 size={16} /></button>
            </div>
          ))}
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" className="btn-secondary" onClick={addItem}><Plus size={16} /> Ajouter ligne</button>
          <button className="btn-primary">{editingId ? <Save size={16} /> : <Plus size={16} />} {editingId ? 'Enregistrer' : 'Creer modele'}</button>
          {editingId && <button type="button" className="btn-secondary" onClick={reset}><X size={16} /> Annuler</button>}
        </div>
      </form>
      <section className="panel overflow-x-auto">
        <table className="table">
          <thead><tr><th className="th">Modele</th><th className="th">Elements</th><th className="th">Statut</th><th className="th">Actions</th></tr></thead>
          <tbody>{templates.map(template => <tr key={template.id}><td className="td"><div className="font-medium">{template.name}</div><div className="text-xs text-muted">{template.description}</div></td><td className="td">{template.items.length}</td><td className="td">{template.active ? 'Actif' : 'Inactif'}</td><td className="td"><div className="flex gap-2"><button className="btn-secondary" onClick={() => edit(template)}><Edit size={16} /></button><button className="btn-secondary text-red-700" onClick={() => remove(template)}><Trash2 size={16} /></button></div></td></tr>)}</tbody>
        </table>
      </section>
    </div>
  );
}
