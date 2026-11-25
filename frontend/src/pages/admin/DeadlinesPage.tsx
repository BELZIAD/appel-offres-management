import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import { Page, Tender } from '../../api/types';
import { DeadlineBadge, StatusBadge } from '../../ui/Badge';
import { deadlineText, formatDate, fullName } from '../../utils/labels';

export function DeadlinesPage() {
  const [tenders, setTenders] = useState<Tender[]>([]);
  useEffect(() => { api.get<Page<Tender>>('/admin/tenders?size=200&sort=deadline,asc').then(r => setTenders(r.data.content)); }, []);
  const groups = useMemo(() => ({
    Urgents: tenders.filter(t => t.deadlineLevel === 'URGENT' || t.deadlineLevel === 'TODAY'),
    'Cette semaine': tenders.filter(t => t.deadlineLevel === 'WARNING'),
    'En retard': tenders.filter(t => t.deadlineLevel === 'OVERDUE'),
    'Plus tard': tenders.filter(t => t.deadlineLevel === 'NORMAL')
  }), [tenders]);
  return <div className="space-y-6"><h1 className="text-2xl font-semibold">Echeances</h1>{Object.entries(groups).map(([title, items]) => <section className="panel overflow-x-auto" key={title}><h2 className="mb-4 text-lg font-semibold">{title}</h2><table className="table"><thead><tr><th className="th">Reference</th><th className="th">Objet</th><th className="th">Organisme</th><th className="th">Responsable</th><th className="th">Date limite</th><th className="th">Jours restants</th><th className="th">Statut</th></tr></thead><tbody>{items.map(t => <tr key={t.id} className="hover:bg-slate-50"><td className="td"><Link className="font-medium" to={`/admin/tenders/${t.id}`}>{t.reference}</Link></td><td className="td">{t.title}</td><td className="td">{t.organization}</td><td className="td">{fullName(t.assignedEmployee)}</td><td className="td">{formatDate(t.deadline)}</td><td className="td"><DeadlineBadge value={t.deadlineLevel} text={deadlineText(t.daysRemaining)} /></td><td className="td"><StatusBadge value={t.status} /></td></tr>)}</tbody></table>{items.length === 0 && <div className="py-6 text-center text-sm text-muted">Aucun element.</div>}</section>)}</div>;
}
