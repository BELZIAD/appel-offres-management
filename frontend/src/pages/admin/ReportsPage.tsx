import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { Employee, StatisticsOverview } from '../../api/types';
import { formatMoney } from '../../utils/labels';

export function ReportsPage() {
  const [overview, setOverview] = useState<StatisticsOverview | null>(null);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [filters, setFilters] = useState({ dateFrom: '', dateTo: '', status: '', result: '', employeeId: '', organization: '' });
  useEffect(() => { api.get<Employee[]>('/admin/employees').then(r => setEmployees(r.data)); }, []);
  useEffect(() => {
    const params = new URLSearchParams();
    if (filters.dateFrom) params.set('dateFrom', filters.dateFrom);
    if (filters.dateTo) params.set('dateTo', filters.dateTo);
    api.get<StatisticsOverview>(`/admin/statistics/overview?${params}`).then(r => setOverview(r.data));
  }, [filters.dateFrom, filters.dateTo]);
  function set(key: keyof typeof filters, value: string) { setFilters({ ...filters, [key]: value }); }
  function download(path: string, filename: string) {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([k, v]) => { if (v) params.set(k, v); });
    api.get(`${path}?${params}`, { responseType: 'blob' }).then(response => {
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      link.click();
      URL.revokeObjectURL(url);
    });
  }
  return <div className="space-y-5"><h1 className="text-2xl font-semibold">Rapports</h1><section className="panel grid gap-3 md:grid-cols-3"><input className="input" type="date" value={filters.dateFrom} onChange={e => set('dateFrom', e.target.value)} /><input className="input" type="date" value={filters.dateTo} onChange={e => set('dateTo', e.target.value)} /><select className="input" value={filters.employeeId} onChange={e => set('employeeId', e.target.value)}><option value="">Tous employes</option>{employees.map(e => <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>)}</select><input className="input" placeholder="Organisme" value={filters.organization} onChange={e => set('organization', e.target.value)} /><select className="input" value={filters.status} onChange={e => set('status', e.target.value)}><option value="">Tous statuts</option><option>NEW</option><option>IN_PROGRESS</option><option>SUBMITTED</option><option>CLOSED</option></select><select className="input" value={filters.result} onChange={e => set('result', e.target.value)}><option value="">Tous resultats</option><option>WON</option><option>LOST</option><option>PENDING</option><option>CANCELLED</option></select></section>{overview && <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4"><div className="panel"><div className="text-sm text-muted">AO concernees</div><div className="text-2xl font-semibold">{overview.totalTenders}</div></div><div className="panel"><div className="text-sm text-muted">Gagnes</div><div className="text-2xl font-semibold">{overview.won}</div></div><div className="panel"><div className="text-sm text-muted">Perdus</div><div className="text-2xl font-semibold">{overview.lost}</div></div><div className="panel"><div className="text-sm text-muted">Montant gagne</div><div className="text-2xl font-semibold">{formatMoney(overview.totalWonAmount)}</div></div></section>}<div className="flex gap-2"><button className="btn-primary" onClick={() => download('/admin/exports/tenders.xlsx', `appels-offres-${new Date().toISOString().slice(0,10)}.xlsx`)}>Exporter Excel</button><button className="btn-secondary" onClick={() => download('/admin/exports/report.pdf', `rapport-appels-offres-${new Date().toISOString().slice(0,10)}.pdf`)}>Generer rapport PDF</button></div></div>;
}
