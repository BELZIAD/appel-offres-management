import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../../api/client';
import { EmployeePerformanceDetail } from '../../api/types';
import { MonthlyTenderChart } from '../../ui/charts';
import { formatMoney } from '../../utils/labels';

export function EmployeePerformanceDetailPage() {
  const { id } = useParams();
  const [data, setData] = useState<EmployeePerformanceDetail | null>(null);
  useEffect(() => { api.get<EmployeePerformanceDetail>(`/admin/statistics/employees/${id}`).then(r => setData(r.data)); }, [id]);
  if (!data) return <div>Chargement...</div>;
  const e = data.employee;
  const cards = [['Affectes', e.assigned], ['Clotures', data.closed], ['Gagnes', e.won], ['Perdus', e.lost], ['En cours', e.inProgress], ['Taux', `${e.winRate} %`], ['Montant', formatMoney(e.wonAmount)]];
  return <div className="space-y-5"><div><h1 className="text-2xl font-semibold">{e.employeeName}</h1><p className="text-sm text-muted">{e.email} - {e.enabled ? 'Actif' : 'Inactif'}</p></div><div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{cards.map(([label, value]) => <div className="panel" key={label}><div className="text-sm text-muted">{label}</div><div className="mt-2 text-2xl font-semibold">{value}</div></div>)}</div><section className="panel"><h2 className="mb-4 text-lg font-semibold">Evolution mensuelle</h2><MonthlyTenderChart data={data.monthly} /></section><section className="panel overflow-x-auto"><h2 className="mb-4 text-lg font-semibold">Appels d'offres</h2><table className="table"><thead><tr><th className="th">Reference</th><th className="th">Objet</th><th className="th">Statut</th><th className="th">Resultat</th><th className="th">Montant gagne</th></tr></thead><tbody>{data.tenders.map(t => <tr key={t.id}><td className="td">{t.reference}</td><td className="td">{t.title}</td><td className="td">{t.status}</td><td className="td">{t.result}</td><td className="td">{formatMoney(t.finalAmount)}</td></tr>)}</tbody></table></section></div>;
}
