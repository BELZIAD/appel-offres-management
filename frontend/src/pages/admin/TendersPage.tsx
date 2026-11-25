import { Edit, Eye, Plus, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, errorMessage } from '../../api/client';
import { Employee, Page, Tender } from '../../api/types';
import { DeadlineBadge, PriorityBadge, ResultBadge, StatusBadge } from '../../ui/Badge';
import { ConfirmModal } from '../../ui/ConfirmModal';
import { Toast } from '../../ui/Toast';
import { deadlineText, formatDate, fullName, resultLabels, statusLabels } from '../../utils/labels';

export function TendersPage() {
  const [page, setPage] = useState<Page<Tender> | null>(null);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('');
  const [result, setResult] = useState('');
  const [employee, setEmployee] = useState('');
  const [size, setSize] = useState(10);
  const [pageNumber, setPageNumber] = useState(0);
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const load = () => {
    const params = new URLSearchParams({ page: String(pageNumber), size: String(size), sort: 'deadline,asc' });
    if (query) params.set('search', query);
    if (status) params.set('status', status);
    if (result) params.set('result', result);
    if (employee) params.set('employeeId', employee);
    api.get<Page<Tender>>(`/admin/tenders?${params}`).then(r => setPage(r.data)).catch(e => setToast({ type: 'error', message: errorMessage(e) }));
  };

  useEffect(() => { load(); }, [pageNumber, size, status, result, employee]);
  useEffect(() => { api.get<Employee[]>('/admin/employees').then(r => setEmployees(r.data)); }, []);

  async function remove() {
    if (!deleteId) return;
    try { await api.delete(`/admin/tenders/${deleteId}`); setDeleteId(null); setToast({ type: 'success', message: "Appel d'offres supprime" }); load(); } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }

  return (
    <div className="space-y-5">
      {toast && <Toast {...toast} />}
      {deleteId && <ConfirmModal title="Supprimer l'appel d'offres" message="Les documents lies seront aussi supprimes en base." onCancel={() => setDeleteId(null)} onConfirm={remove} />}
      <div className="flex flex-wrap items-center justify-between gap-3"><h1 className="text-2xl font-semibold">Appels d'offres</h1><Link className="btn-primary" to="/admin/tenders/new"><Plus size={18} /> Nouvel appel d'offres</Link></div>
      <div className="panel grid gap-3 md:grid-cols-5">
        <input className="input" placeholder="Recherche" value={query} onChange={e => setQuery(e.target.value)} onKeyDown={e => { if (e.key === 'Enter') { setPageNumber(0); load(); } }} />
        <select className="input" value={status} onChange={e => { setStatus(e.target.value); setPageNumber(0); }}><option value="">Tous statuts</option>{Object.entries(statusLabels).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select>
        <select className="input" value={result} onChange={e => { setResult(e.target.value); setPageNumber(0); }}><option value="">Tous resultats</option>{Object.entries(resultLabels).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select>
        <select className="input" value={employee} onChange={e => { setEmployee(e.target.value); setPageNumber(0); }}><option value="">Tous employes</option>{employees.map(e => <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>)}</select>
        <select className="input" value={size} onChange={e => { setSize(Number(e.target.value)); setPageNumber(0); }}><option>10</option><option>20</option><option>50</option></select>
      </div>
      <div className="panel overflow-x-auto">
        <table className="table"><thead><tr><th className="th">Reference</th><th className="th">Objet</th><th className="th">Organisme</th><th className="th">Responsable</th><th className="th">Publication</th><th className="th">Echeance</th><th className="th">Statut</th><th className="th">Resultat</th><th className="th">Priorite</th><th className="th">Actions</th></tr></thead>
          <tbody>{page?.content.map(t => <tr key={t.id}><td className="td">{t.reference}</td><td className="td">{t.title}</td><td className="td">{t.organization}</td><td className="td">{fullName(t.assignedEmployee)}</td><td className="td">{formatDate(t.publicationDate)}</td><td className="td"><DeadlineBadge value={t.deadlineLevel} text={deadlineText(t.daysRemaining)} /></td><td className="td"><StatusBadge value={t.status} /></td><td className="td"><ResultBadge value={t.result} /></td><td className="td"><PriorityBadge value={t.priority} /></td><td className="td"><div className="flex gap-2"><Link className="btn-secondary" to={`/admin/tenders/${t.id}`}><Eye size={16} /></Link><Link className="btn-secondary" to={`/admin/tenders/${t.id}/edit`}><Edit size={16} /></Link><button className="btn-secondary text-red-700" onClick={() => setDeleteId(t.id)}><Trash2 size={16} /></button></div></td></tr>)}</tbody></table>
        {page?.content.length === 0 && <div className="py-8 text-center text-sm text-muted">Aucun appel d'offres trouve.</div>}
      </div>
      {page && <div className="flex items-center justify-end gap-2"><button className="btn-secondary" disabled={page.number === 0} onClick={() => setPageNumber(page.number - 1)}>Precedent</button><span className="text-sm">Page {page.number + 1} / {Math.max(page.totalPages, 1)}</span><button className="btn-secondary" disabled={page.number + 1 >= page.totalPages} onClick={() => setPageNumber(page.number + 1)}>Suivant</button></div>}
    </div>
  );
}
