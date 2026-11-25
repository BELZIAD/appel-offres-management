import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api/client';
import { EmployeeStats, Page, Tender, TenderTask } from '../../api/types';
import { useAuth } from '../../state/AuthContext';
import { Toast } from '../../ui/Toast';

export function EmployeeDashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState<EmployeeStats | null>(null);
  const [tasks, setTasks] = useState<TenderTask[]>([]);
  const [favorites, setFavorites] = useState<Tender[]>([]);
  const [error, setError] = useState('');
  useEffect(() => {
    Promise.all([
      api.get<EmployeeStats>('/employee/dashboard'),
      api.get<Page<TenderTask>>('/employee/tasks?size=100'),
      api.get<Tender[]>('/favorites')
    ]).then(([s, t, f]) => { setStats(s.data); setTasks(t.data.content); setFavorites(f.data); }).catch(e => setError(errorMessage(e)));
  }, []);
  if (!stats) return <div>Chargement...</div>;
  const denominator = stats.won + stats.lost;
  const winRate = denominator > 0 ? `${Math.round((stats.won * 10000) / denominator) / 100} %` : '0 %';
  const cards = [['Mes AO', stats.myTenders], ['En cours', stats.inProgress], ['Deposes', stats.submitted], ['Gagnes', stats.won], ['Perdus', stats.lost], ['Mon taux de reussite', winRate], ['Mes taches', tasks.length], ["A faire aujourd'hui", tasks.filter(t => t.daysRemaining === 0 && t.status !== 'DONE').length], ['En retard', tasks.filter(t => t.overdue).length], ['Bloquees', tasks.filter(t => t.status === 'BLOCKED').length], ['Terminees', tasks.filter(t => t.status === 'DONE').length], ['Mes favoris', favorites.length]];
  return <div className="space-y-6">{error && <Toast type="error" message={error} />}<h1 className="text-2xl font-semibold">Bonjour {user?.firstName}</h1><div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{cards.map(([label, value]) => <div className="panel" key={label}><div className="text-sm text-muted">{label}</div><div className="mt-2 text-3xl font-semibold">{value}</div></div>)}</div><section className="panel"><h2 className="mb-4 text-lg font-semibold">Mes prochaines echeances</h2><div className="space-y-3">{stats.nextDeadlines.map(t => <div key={t.id} className="rounded-md border border-line p-3"><div className="font-semibold">{t.reference}</div><div className="text-sm text-muted">{t.organization}</div><div className="text-sm">{t.daysRemaining === 0 ? "Aujourd'hui" : `${t.daysRemaining} jours`}</div></div>)}</div>{stats.nextDeadlines.length === 0 && <div className="text-sm text-muted">Aucune echeance a venir.</div>}</section></div>;
}
