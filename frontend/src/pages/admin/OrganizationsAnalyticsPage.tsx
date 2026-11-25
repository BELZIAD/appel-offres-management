import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { OrganizationStats } from '../../api/types';
import { OrganizationChart } from '../../ui/charts';
import { formatMoney } from '../../utils/labels';

export function OrganizationsAnalyticsPage() {
  const [rows, setRows] = useState<OrganizationStats[]>([]);
  useEffect(() => { api.get<OrganizationStats[]>('/admin/statistics/organizations').then(r => setRows(r.data)); }, []);
  return <div className="space-y-5"><h1 className="text-2xl font-semibold">Analyses organismes</h1><section className="panel"><h2 className="mb-4 text-lg font-semibold">Top 10 organismes</h2><OrganizationChart data={rows} /></section><section className="panel overflow-x-auto"><table className="table"><thead><tr><th className="th">Organisme</th><th className="th">Nombre AO</th><th className="th">Gagnes</th><th className="th">Perdus</th><th className="th">En attente</th><th className="th">Taux</th><th className="th">Montant gagne</th></tr></thead><tbody>{rows.map(r => <tr key={r.organization}><td className="td font-medium">{r.organization}</td><td className="td">{r.total}</td><td className="td">{r.won}</td><td className="td">{r.lost}</td><td className="td">{r.pending}</td><td className="td">{r.winRate} %</td><td className="td">{formatMoney(r.wonAmount)}</td></tr>)}</tbody></table>{rows.length === 0 && <div className="py-8 text-center text-sm text-muted">Aucune donnee disponible.</div>}</section></div>;
}
