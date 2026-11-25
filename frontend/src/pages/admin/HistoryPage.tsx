import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { HistoryItem, Page, TenderHistoryAction } from '../../api/types';

const actions: TenderHistoryAction[] = ['CREATED', 'UPDATED', 'ASSIGNED', 'REASSIGNED', 'STATUS_CHANGED', 'RESULT_CHANGED', 'DOCUMENT_ADDED', 'DOCUMENT_DELETED', 'DELETED'];

export function HistoryPage() {
  const [page, setPage] = useState<Page<HistoryItem> | null>(null);
  const [filters, setFilters] = useState({ search: '', action: '', userId: '', tenderId: '', dateFrom: '', dateTo: '' });

  useEffect(() => {
    const params = new URLSearchParams({ size: '50' });
    Object.entries(filters).forEach(([key, value]) => {
      if (value) params.set(key, value);
    });
    api.get<Page<HistoryItem>>(`/admin/history?${params.toString()}`).then(r => setPage(r.data));
  }, [filters]);

  return <div className="space-y-5">
    <h1 className="text-2xl font-semibold">Historique</h1>
    <section className="panel grid gap-3 md:grid-cols-6">
      <input className="input md:col-span-2" placeholder="Rechercher" value={filters.search} onChange={e => setFilters({ ...filters, search: e.target.value })} />
      <select className="input" value={filters.action} onChange={e => setFilters({ ...filters, action: e.target.value })}>
        <option value="">Toutes actions</option>
        {actions.map(action => <option key={action} value={action}>{action}</option>)}
      </select>
      <input className="input" placeholder="ID utilisateur" value={filters.userId} onChange={e => setFilters({ ...filters, userId: e.target.value.replace(/\D/g, '') })} />
      <input className="input" placeholder="ID AO" value={filters.tenderId} onChange={e => setFilters({ ...filters, tenderId: e.target.value.replace(/\D/g, '') })} />
      <button className="btn-outline" onClick={() => setFilters({ search: '', action: '', userId: '', tenderId: '', dateFrom: '', dateTo: '' })}>Reinitialiser</button>
      <input className="input" type="date" value={filters.dateFrom} onChange={e => setFilters({ ...filters, dateFrom: e.target.value })} />
      <input className="input" type="date" value={filters.dateTo} onChange={e => setFilters({ ...filters, dateTo: e.target.value })} />
    </section>
    <section className="panel">
      <div className="space-y-4">{page?.content.map(h => <div key={h.id} className="border-l-2 border-slate-300 pl-4">
        <div className="text-xs text-muted">{new Date(h.createdAt).toLocaleString('fr-FR')} {h.tenderId ? `AO #${h.tenderId}` : 'Global'} - {h.action}</div>
        <div className="font-medium">{h.performedBy ? `${h.performedBy.firstName} ${h.performedBy.lastName}` : 'Systeme'}</div>
        <div className="text-sm text-slate-700">{h.description}</div>
      </div>)}</div>
      {page?.content.length === 0 && <div className="text-sm text-muted">Aucun historique.</div>}
    </section>
  </div>;
}
