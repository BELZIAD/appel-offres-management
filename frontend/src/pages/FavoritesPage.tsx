import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { api } from '../api/client';
import { Tender } from '../api/types';
import { ChecklistBadge, DeadlineBadge, PriorityBadge, StatusBadge } from '../ui/Badge';
import { deadlineText, formatDate } from '../utils/labels';

export function FavoritesPage() {
  const admin = useLocation().pathname.startsWith('/admin');
  const [items, setItems] = useState<Tender[]>([]);
  useEffect(() => { api.get<Tender[]>('/favorites').then(r => setItems(r.data)); }, []);
  return <div className="space-y-5">
    <h1 className="text-2xl font-semibold">Mes favoris</h1>
    <section className="panel overflow-x-auto">
      <table className="w-full text-left text-sm"><thead><tr className="border-b border-line text-xs uppercase text-muted"><th className="py-2">AO</th><th>Organisme</th><th>Statut</th><th>Priorite</th><th>Checklist</th><th>Echeance</th></tr></thead>
        <tbody>{items.map(t => <tr key={t.id} className="border-b border-line last:border-0"><td className="py-3"><Link className="font-medium underline" to={`${admin ? '/admin' : '/employee'}/tenders/${t.id}`}>{t.reference}</Link><div className="text-xs text-muted">{t.title}</div></td><td>{t.organization}</td><td><StatusBadge value={t.status} /></td><td><PriorityBadge value={t.priority} /></td><td><ChecklistBadge complete={t.checklistComplete} /><div className="text-xs text-muted">{t.progressPercentage}%</div></td><td>{formatDate(t.deadline)}<div className="text-xs text-muted"><DeadlineBadge value={t.deadlineLevel} text={deadlineText(t.daysRemaining)} /></div></td></tr>)}</tbody></table>
      {items.length === 0 && <div className="py-8 text-center text-sm text-muted">Aucun favori.</div>}
    </section>
  </div>;
}
