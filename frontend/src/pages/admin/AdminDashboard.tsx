import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api/client';
import { ActivityItem, MonthlyStats, OrganizationStats, Page, ResultsStats, StatisticsOverview, Tender, TenderTask } from '../../api/types';
import { ResultBadge } from '../../ui/Badge';
import { MonthlyTenderChart, OrganizationChart, ResultDonutChart } from '../../ui/charts';
import { Toast } from '../../ui/Toast';
import { deadlineText, formatDate, formatMoney, fullName } from '../../utils/labels';

export function AdminDashboard() {
  const [overview, setOverview] = useState<StatisticsOverview | null>(null);
  const [monthly, setMonthly] = useState<MonthlyStats[]>([]);
  const [results, setResults] = useState<ResultsStats>({ won: 0, lost: 0, pending: 0, cancelled: 0 });
  const [organizations, setOrganizations] = useState<OrganizationStats[]>([]);
  const [nextDeadlines, setNextDeadlines] = useState<Tender[]>([]);
  const [recentResults, setRecentResults] = useState<Tender[]>([]);
  const [tasks, setTasks] = useState<TenderTask[]>([]);
  const [activity, setActivity] = useState<ActivityItem[]>([]);
  const [year, setYear] = useState(String(new Date().getFullYear()));
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [error, setError] = useState('');

  const qs = () => {
    const params = new URLSearchParams();
    if (dateFrom) params.set('dateFrom', dateFrom);
    if (dateTo) params.set('dateTo', dateTo);
    return params.toString();
  };

  useEffect(() => {
    const suffix = qs();
    const yearParam = year || new Date().getFullYear();
    Promise.all([
      api.get<StatisticsOverview>(`/admin/statistics/overview${suffix ? `?${suffix}` : ''}`),
      api.get<MonthlyStats[]>(`/admin/statistics/monthly?year=${yearParam}${suffix ? `&${suffix}` : ''}`),
      api.get<ResultsStats>(`/admin/statistics/results${suffix ? `?${suffix}` : ''}`),
      api.get<OrganizationStats[]>(`/admin/statistics/organizations${suffix ? `?${suffix}` : ''}`),
      api.get<{ nextDeadlines: Tender[] }>('/admin/dashboard'),
      api.get<{ content: Tender[] }>('/admin/tenders?result=WON&size=5&sort=resultDate,desc'),
      api.get<Page<TenderTask>>('/admin/tasks?size=100'),
      api.get<Page<ActivityItem>>('/admin/activity?size=6')
    ]).then(([o, m, r, org, dash, recent, taskPage, activityPage]) => {
      setOverview(o.data);
      setMonthly(m.data);
      setResults(r.data);
      setOrganizations(org.data);
      setNextDeadlines(dash.data.nextDeadlines ?? []);
      setRecentResults(recent.data.content ?? []);
      setTasks(taskPage.data.content ?? []);
      setActivity(activityPage.data.content ?? []);
    }).catch(e => setError(errorMessage(e)));
  }, [year, dateFrom, dateTo]);

  if (!overview) return <div>Chargement...</div>;
  const cards = [
    ['Total AO', overview.totalTenders],
    ['En cours', overview.inProgress],
    ['Deposes', overview.submitted],
    ['Gagnes', overview.won],
    ['Perdus', overview.lost],
    ['Taux reussite', `${overview.winRate} %`],
    ['Montant remporte', formatMoney(overview.totalWonAmount)],
    ['Urgents', overview.urgent],
    ['Taches ouvertes', tasks.filter(t => t.status !== 'DONE').length],
    ['Taches en retard', tasks.filter(t => t.overdue).length],
    ['Taches bloquees', tasks.filter(t => t.status === 'BLOCKED').length],
    ['Dossiers incomplets', nextDeadlines.filter(t => !t.checklistComplete).length]
  ];
  return (
    <div className="space-y-6">
      {error && <Toast type="error" message={error} />}
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h1 className="text-2xl font-semibold">Dashboard admin</h1>
        <div className="grid gap-2 sm:grid-cols-3">
          <label><span className="label">Annee</span><input className="input mt-1" value={year} onChange={e => setYear(e.target.value)} /></label>
          <label><span className="label">Date debut</span><input className="input mt-1" type="date" value={dateFrom} onChange={e => setDateFrom(e.target.value)} /></label>
          <label><span className="label">Date fin</span><input className="input mt-1" type="date" value={dateTo} onChange={e => setDateTo(e.target.value)} /></label>
        </div>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{cards.map(([label, value]) => <div className="panel" key={label}><div className="text-sm text-muted">{label}</div><div className="mt-2 text-2xl font-semibold">{value}</div></div>)}</div>
      <div className="grid gap-5 xl:grid-cols-2"><section className="panel"><h2 className="mb-4 text-lg font-semibold">Evolution mensuelle</h2><MonthlyTenderChart data={monthly} /></section><section className="panel"><h2 className="mb-4 text-lg font-semibold">Repartition resultats</h2><ResultDonutChart data={results} /></section></div>
      <div className="grid gap-5 xl:grid-cols-2">
        <section className="panel"><h2 className="mb-4 text-lg font-semibold">Prochaines echeances</h2><div className="space-y-3">{nextDeadlines.map(t => <div key={t.id} className="rounded-md border border-line p-3"><div className="font-semibold">{t.reference}</div><div className="text-sm text-muted">{t.organization} - {fullName(t.assignedEmployee)}</div><div className="text-sm">{deadlineText(t.daysRemaining)}</div></div>)}</div>{nextDeadlines.length === 0 && <div className="text-sm text-muted">Aucune echeance a venir.</div>}</section>
        <section className="panel"><h2 className="mb-4 text-lg font-semibold">Top organismes</h2><OrganizationChart data={organizations} /></section>
      </div>
      <section className="panel"><h2 className="mb-4 text-lg font-semibold">Activite recente</h2><div className="space-y-3">{activity.map(a => <div key={a.id} className="border-l-2 border-slate-300 pl-3"><div className="text-xs text-muted">{new Date(a.createdAt).toLocaleString('fr-FR')} - {a.action}</div><div className="text-sm">{a.description}</div></div>)}</div>{activity.length === 0 && <div className="text-sm text-muted">Aucune activite recente.</div>}</section>
      <section className="panel overflow-x-auto"><h2 className="mb-4 text-lg font-semibold">Derniers resultats</h2><table className="table"><thead><tr><th className="th">Reference</th><th className="th">Organisme</th><th className="th">Responsable</th><th className="th">Resultat</th><th className="th">Montant</th><th className="th">Date resultat</th></tr></thead><tbody>{recentResults.map(t => <tr key={t.id}><td className="td">{t.reference}</td><td className="td">{t.organization}</td><td className="td">{fullName(t.assignedEmployee)}</td><td className="td"><ResultBadge value={t.result} /></td><td className="td">{formatMoney(t.finalAmount)}</td><td className="td">{formatDate(t.resultDate)}</td></tr>)}</tbody></table>{recentResults.length === 0 && <div className="py-6 text-center text-sm text-muted">Aucun resultat recent.</div>}</section>
    </div>
  );
}
