import { ChangeEvent, FormEvent, useEffect, useState } from 'react';
import axios from 'axios';
import { Download, Edit, FileUp, Plus, Save, Trash2, X } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { api, errorMessage } from '../api/client';
import { PricingLine, PricingStatus, TenderPricing } from '../api/types';
import { useAuth } from '../state/AuthContext';
import { Toast } from '../ui/Toast';
import { formatMoney, pricingStatusLabels } from '../utils/labels';

export function PricingPage() {
  const { id } = useParams();
  const [pricing, setPricing] = useState<TenderPricing | null>(null);
  const [line, setLine] = useState({ designation: '', unit: '', quantity: '1', unitPrice: '0', reference: '' });
  const [editingLineId, setEditingLineId] = useState<number | null>(null);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [preview, setPreview] = useState<ImportPreview | null>(null);
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const load = () => api.get<TenderPricing>(`/tenders/${id}/pricing`).then(r => setPricing(r.data)).catch(e => setToast({ type: 'error', message: errorMessage(e) }));
  useEffect(() => { load(); }, [id]);

  async function saveLine(event: FormEvent) {
    event.preventDefault();
    try {
      const payload = { ...line, quantity: Number(line.quantity), unitPrice: Number(line.unitPrice) };
      if (editingLineId) await api.put(`/tenders/${id}/pricing/lines/${editingLineId}`, payload);
      else await api.post(`/tenders/${id}/pricing/lines`, payload);
      resetLine();
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  function editLine(row: PricingLine) {
    setEditingLineId(row.id);
    setLine({ designation: row.designation, unit: row.unit || '', quantity: String(row.quantity), unitPrice: String(row.unitPrice), reference: row.reference || '' });
  }
  function resetLine() {
    setEditingLineId(null);
    setLine({ designation: '', unit: '', quantity: '1', unitPrice: '0', reference: '' });
  }
  async function remove(row: PricingLine) {
    try { await api.delete(`/tenders/${id}/pricing/lines/${row.id}`); load(); } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function status(next: PricingStatus) {
    try { await api.patch(`/tenders/${id}/pricing/status`, { status: next, notes: pricing?.notes }); load(); } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function previewImport(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    const data = new FormData();
    data.append('file', file);
    try {
      const response = await api.post<ImportPreview>(`/tenders/${id}/pricing/import`, data);
      setPreview(response.data);
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function applyImport(replace = false) {
    if (!preview) return;
    try {
      await api.post(`/tenders/${id}/pricing/import/apply${replace ? '?replace=true' : ''}`, preview);
      setPreview(null);
      setToast({ type: 'success', message: `${preview.lines.length} lignes importees` });
      load();
    } catch (e) {
      if (!replace && axios.isAxiosError(e) && e.response?.status === 409) {
        if (window.confirm(`${errorMessage(e)}\n\nRemplacer toutes les lignes actuelles du bordereau ?`)) return applyImport(true);
        return;
      }
      setToast({ type: 'error', message: errorMessage(e) });
    }
  }
  async function exportExcel() {
    const response = await api.get(`/tenders/${id}/pricing/export`, { responseType: 'blob' });
    const url = URL.createObjectURL(response.data);
    const link = document.createElement('a');
    link.href = url;
    link.download = `bordereau-${id}.xlsx`;
    link.click();
    URL.revokeObjectURL(url);
  }

  if (!pricing) return <div className="panel">Chargement...</div>;
  return (
    <div className="space-y-5">
      {toast && <Toast {...toast} />}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div><h1 className="text-2xl font-semibold">Chiffrage</h1><p className="text-sm text-muted">{pricing.assignedPricingManager ? `${pricing.assignedPricingManager.firstName} ${pricing.assignedPricingManager.lastName}` : 'Responsable non affecte'}</p></div>
        <div className="flex gap-2"><select className="input" value={pricing.status} onChange={e => status(e.target.value as PricingStatus)}>{Object.entries(pricingStatusLabels).filter(([k]) => isAdmin || k !== 'VALIDATED' || pricing.status === 'VALIDATED').map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select><label className="btn-secondary cursor-pointer" title="Importer un bordereau Excel (colonnes : N, Designation, Unite, Quantite, Prix unitaire)"><FileUp size={16} /> Importer<input className="hidden" type="file" accept=".xlsx,.xls" onChange={previewImport} /></label><button className="btn-secondary" onClick={exportExcel}><Download size={16} /> Excel</button></div>
      </div>
      <section className="grid gap-4 md:grid-cols-3"><div className="panel"><div className="text-sm text-muted">Etat</div><div className="text-xl font-semibold">{pricingStatusLabels[pricing.status]}</div></div><div className="panel"><div className="text-sm text-muted">Total HT</div><div className="text-xl font-semibold">{formatMoney(pricing.finalTotal)}</div></div><div className="panel"><div className="text-sm text-muted">Validation</div><div className="text-xl font-semibold">{pricing.validatedAt ? new Date(pricing.validatedAt).toLocaleDateString('fr-FR') : '-'}</div></div></section>
      {preview && (
        <section className="panel space-y-3 border-sky-200">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div><h2 className="text-lg font-semibold">Apercu de l'import</h2><p className="text-sm text-muted">{preview.lines.length} lignes detectees - total {formatMoney(preview.lines.reduce((sum, l) => sum + Number(l.quantity || 0) * Number(l.unitPrice || 0), 0))}. L'import remplace les lignes actuelles.</p></div>
            <div className="flex gap-2"><button className="btn-primary" onClick={() => applyImport()}><FileUp size={16} /> Importer ces lignes</button><button className="btn-secondary" onClick={() => setPreview(null)}><X size={16} /> Annuler</button></div>
          </div>
          <div className="max-h-72 overflow-auto"><table className="table"><thead><tr><th className="th">N</th><th className="th">Designation</th><th className="th">Unite</th><th className="th">Quantite</th><th className="th">PU HT</th></tr></thead><tbody>{preview.lines.map((l, i) => <tr key={i}><td className="td">{l.lineNumber}</td><td className="td">{l.designation}</td><td className="td">{l.unit}</td><td className="td">{l.quantity}</td><td className="td">{formatMoney(l.unitPrice)}</td></tr>)}</tbody></table></div>
        </section>
      )}
      <form className="panel grid gap-3 md:grid-cols-6" onSubmit={saveLine}>
        <input className="input" placeholder="Reference" value={line.reference} onChange={e => setLine({ ...line, reference: e.target.value })} />
        <input className="input md:col-span-2" required placeholder="Designation" value={line.designation} onChange={e => setLine({ ...line, designation: e.target.value })} />
        <input className="input" placeholder="Unite" value={line.unit} onChange={e => setLine({ ...line, unit: e.target.value })} />
        <input className="input" type="number" step="0.0001" placeholder="Quantite" value={line.quantity} onChange={e => setLine({ ...line, quantity: e.target.value })} />
        <input className="input" type="number" step="0.0001" placeholder="PU HT" value={line.unitPrice} onChange={e => setLine({ ...line, unitPrice: e.target.value })} />
        <div className="flex flex-wrap gap-2 md:col-span-6">
          <button className="btn-primary">{editingLineId ? <Save size={16} /> : <Plus size={16} />} {editingLineId ? 'Enregistrer ligne' : 'Ajouter ligne'}</button>
          {editingLineId && <button type="button" className="btn-secondary" onClick={resetLine}><X size={16} /> Annuler</button>}
        </div>
      </form>
      <section className="panel overflow-x-auto"><table className="table"><thead><tr><th className="th">N</th><th className="th">Reference</th><th className="th">Designation</th><th className="th">Unite</th><th className="th">Quantite</th><th className="th">PU HT</th><th className="th">Total HT</th><th className="th"></th></tr></thead><tbody>{pricing.lines.map(row => <tr key={row.id}><td className="td">{row.position}</td><td className="td">{row.reference}</td><td className="td">{row.designation}</td><td className="td">{row.unit}</td><td className="td">{row.quantity}</td><td className="td">{formatMoney(row.unitPrice)}</td><td className="td">{formatMoney(row.totalPrice)}</td><td className="td"><div className="flex gap-2"><button className="btn-secondary" onClick={() => editLine(row)}><Edit size={16} /></button><button className="btn-secondary text-red-700" onClick={() => remove(row)}><Trash2 size={16} /></button></div></td></tr>)}</tbody></table></section>
    </div>
  );
}

interface ImportPreview {
  lines: Array<{ lineNumber?: string; reference?: string; designation: string; unit?: string; quantity: number; unitPrice: number; notes?: string; position?: number }>;
  containsPrices: boolean;
}
