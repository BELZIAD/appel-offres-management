import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import { EmployeePerformance } from '../../api/types';
import { formatMoney } from '../../utils/labels';

export function EmployeePerformancePage() {
  const [rows, setRows] = useState<EmployeePerformance[]>([]);
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState('employeeName');
  useEffect(() => { api.get<EmployeePerformance[]>(`/admin/statistics/employees?sort=${sort}`).then(r => setRows(r.data)); }, [sort]);
  const filtered = useMemo(() => rows.filter(r => r.employeeName.toLowerCase().includes(search.toLowerCase())), [rows, search]);
  return <div className="space-y-5"><div className="flex flex-wrap items-center justify-between gap-3"><h1 className="text-2xl font-semibold">Performance employes</h1><div className="flex gap-2"><input className="input" placeholder="Recherche nom" value={search} onChange={e => setSearch(e.target.value)} /><select className="input" value={sort} onChange={e => setSort(e.target.value)}><option value="employeeName">Nom</option><option value="assigned">Affectes</option><option value="won">Gagnes</option><option value="winRate">Taux</option><option value="wonAmount">Montant</option></select></div></div><section className="panel overflow-x-auto"><table className="table"><thead><tr><th className="th">Employe</th><th className="th">Affectes</th><th className="th">En cours</th><th className="th">Deposes</th><th className="th">Gagnes</th><th className="th">Perdus</th><th className="th">Taux reussite</th><th className="th">Montant gagne</th></tr></thead><tbody>{filtered.map(r => <tr key={r.employeeId}><td className="td"><Link className="font-medium" to={`/admin/performance/employees/${r.employeeId}`}>{r.employeeName}</Link><div className="text-xs text-muted">{r.email}</div></td><td className="td">{r.assigned}</td><td className="td">{r.inProgress}</td><td className="td">{r.submitted}</td><td className="td">{r.won}</td><td className="td">{r.lost}</td><td className="td">{r.winRate} %</td><td className="td">{formatMoney(r.wonAmount)}</td></tr>)}</tbody></table>{filtered.length === 0 && <div className="py-8 text-center text-sm text-muted">Aucune donnee disponible.</div>}</section></div>;
}
