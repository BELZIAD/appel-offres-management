import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { ActivityItem, Page } from '../../api/types';
import { fullName } from '../../utils/labels';

export function ActivityPage() {
  const [page, setPage] = useState<Page<ActivityItem> | null>(null);
  const [filters, setFilters] = useState({ action: '', userId: '', tenderId: '', dateFrom: '', dateTo: '' });
  useEffect(() => {
    const params = new URLSearchParams({ size: '30' });
    Object.entries(filters).forEach(([k, v]) => v && params.set(k, v));
    api.get<Page<ActivityItem>>(`/admin/activity?${params}`).then(r => setPage(r.data));
  }, [filters]);
  return <div className="space-y-5">
    <h1 className="text-2xl font-semibold">Activite</h1>
    <section className="panel grid gap-3 md:grid-cols-5">
      <input className="input" placeholder="Action" value={filters.action} onChange={e => setFilters({ ...filters, action: e.target.value })} />
      <input className="input" placeholder="ID utilisateur" value={filters.userId} onChange={e => setFilters({ ...filters, userId: e.target.value.replace(/\D/g, '') })} />
      <input className="input" placeholder="ID AO" value={filters.tenderId} onChange={e => setFilters({ ...filters, tenderId: e.target.value.replace(/\D/g, '') })} />
      <input className="input" type="date" value={filters.dateFrom} onChange={e => setFilters({ ...filters, dateFrom: e.target.value })} />
      <input className="input" type="date" value={filters.dateTo} onChange={e => setFilters({ ...filters, dateTo: e.target.value })} />
    </section>
    <section className="panel"><div className="space-y-4">{page?.content.map(a => <div key={a.id} className="border-l-2 border-slate-300 pl-4"><div className="text-xs text-muted">{new Date(a.createdAt).toLocaleString('fr-FR')} - {a.action} - {a.tenderReference ?? 'Global'}</div><div className="font-medium">{a.user ? fullName(a.user) : 'Systeme'}</div><div className="text-sm text-slate-700">{a.description}</div></div>)}</div>{page?.content.length === 0 && <div className="text-sm text-muted">Aucune activite.</div>}</section>
  </div>;
}
