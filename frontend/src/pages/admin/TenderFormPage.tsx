import { FormEvent, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, errorMessage } from '../../api/client';
import { Employee, Priority, Tender, TenderPayload, TenderResult, TenderStatus } from '../../api/types';
import { Toast } from '../../ui/Toast';
import { priorityLabels, resultLabels, statusLabels } from '../../utils/labels';

const initial: TenderPayload = { reference: '', marketNumber: '', title: '', organization: '', description: '', publicationDate: '', deadline: '', estimatedAmount: null, provisionalGuarantee: null, priority: 'NORMAL', status: 'NEW', result: 'PENDING', assignedEmployeeId: null };

export function TenderFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [form, setForm] = useState<TenderPayload>(initial);
  const [file, setFile] = useState<File | null>(null);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const editing = Boolean(id);
  useEffect(() => {
    api.get<Employee[]>('/admin/employees').then(r => setEmployees(r.data));
    if (id) api.get<Tender>(`/tenders/${id}`).then(r => setForm({
      reference: r.data.reference, marketNumber: r.data.marketNumber ?? '', title: r.data.title, organization: r.data.organization,
      description: r.data.description ?? '', publicationDate: r.data.publicationDate ?? '', deadline: r.data.deadline,
      estimatedAmount: r.data.estimatedAmount ?? null, provisionalGuarantee: r.data.provisionalGuarantee ?? null,
      priority: r.data.priority, status: r.data.status, result: r.data.result, assignedEmployeeId: r.data.assignedEmployee?.id ?? null
    })).catch(e => setToast({ type: 'error', message: errorMessage(e) }));
  }, [id]);
  function set<K extends keyof TenderPayload>(key: K, value: TenderPayload[K]) { setForm({ ...form, [key]: value }); }
  async function submit(event: FormEvent) {
    event.preventDefault();
    try {
      const payload = { ...form, publicationDate: form.publicationDate || undefined, assignedEmployeeId: form.assignedEmployeeId || null };
      const saved = editing ? (await api.put<Tender>(`/admin/tenders/${id}`, payload)).data : (await api.post<Tender>('/admin/tenders', payload)).data;
      if (file) {
        const data = new FormData();
        data.append('documentType', 'RC');
        data.append('file', file);
        await api.post(`/tenders/${saved.id}/documents`, data);
      }
      navigate(`/admin/tenders/${saved.id}`);
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  return (
    <div className="max-w-5xl space-y-5">
      {toast && <Toast {...toast} />}
      <h1 className="text-2xl font-semibold">{editing ? "Modifier l'appel d'offres" : "Nouvel appel d'offres"}</h1>
      <form className="panel grid gap-4 md:grid-cols-2" onSubmit={submit}>
        <label><span className="label">Reference</span><input className="input mt-1" required value={form.reference} onChange={e => set('reference', e.target.value)} /></label>
        <label><span className="label">Numero marche</span><input className="input mt-1" value={form.marketNumber ?? ''} onChange={e => set('marketNumber', e.target.value)} /></label>
        <label className="md:col-span-2"><span className="label">Objet</span><input className="input mt-1" required value={form.title} onChange={e => set('title', e.target.value)} /></label>
        <label className="md:col-span-2"><span className="label">Organisme</span><input className="input mt-1" required value={form.organization} onChange={e => set('organization', e.target.value)} /></label>
        <label className="md:col-span-2"><span className="label">Description</span><textarea className="input mt-1 min-h-28" value={form.description ?? ''} onChange={e => set('description', e.target.value)} /></label>
        <label><span className="label">Publication</span><input className="input mt-1" type="date" value={form.publicationDate ?? ''} onChange={e => set('publicationDate', e.target.value)} /></label>
        <label><span className="label">Deadline</span><input className="input mt-1" required type="date" value={form.deadline} onChange={e => set('deadline', e.target.value)} /></label>
        <label><span className="label">Montant estime</span><input className="input mt-1" type="number" step="0.01" value={form.estimatedAmount ?? ''} onChange={e => set('estimatedAmount', e.target.value ? Number(e.target.value) : null)} /></label>
        <label><span className="label">Caution provisoire</span><input className="input mt-1" type="number" step="0.01" value={form.provisionalGuarantee ?? ''} onChange={e => set('provisionalGuarantee', e.target.value ? Number(e.target.value) : null)} /></label>
        <label><span className="label">Priorite</span><select className="input mt-1" value={form.priority} onChange={e => set('priority', e.target.value as Priority)}>{Object.entries(priorityLabels).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></label>
        <label><span className="label">Employe</span><select className="input mt-1" value={form.assignedEmployeeId ?? ''} onChange={e => set('assignedEmployeeId', e.target.value ? Number(e.target.value) : null)}><option value="">Non affecte</option>{employees.map(e => <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>)}</select></label>
        <label><span className="label">Statut</span><select className="input mt-1" value={form.status} onChange={e => set('status', e.target.value as TenderStatus)}>{Object.entries(statusLabels).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></label>
        <label><span className="label">Resultat</span><select className="input mt-1" value={form.result} onChange={e => set('result', e.target.value as TenderResult)}>{Object.entries(resultLabels).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></label>
        <label className="md:col-span-2"><span className="label">RC PDF</span><input className="input mt-1" type="file" accept="application/pdf" onChange={e => setFile(e.target.files?.[0] ?? null)} /></label>
        <div className="flex justify-end gap-2 md:col-span-2"><button type="button" className="btn-secondary" onClick={() => navigate('/admin/tenders')}>Annuler</button><button className="btn-primary">Enregistrer</button></div>
      </form>
    </div>
  );
}
