import { Eye } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, errorMessage } from '../../api/client';
import { Tender } from '../../api/types';
import { PriorityBadge, ResultBadge, StatusBadge } from '../../ui/Badge';
import { Toast } from '../../ui/Toast';
import { formatDate } from '../../utils/labels';

export function EmployeeTendersPage() {
  const [tenders, setTenders] = useState<Tender[]>([]);
  const [error, setError] = useState('');
  useEffect(() => { api.get<Tender[]>('/employee/tenders').then(r => setTenders(r.data)).catch(e => setError(errorMessage(e))); }, []);
  return (
    <div className="space-y-5">
      {error && <Toast type="error" message={error} />}
      <h1 className="text-2xl font-semibold">Mes marches</h1>
      <div className="panel overflow-x-auto">
        <table className="table"><thead><tr><th className="th">Reference</th><th className="th">Objet</th><th className="th">Organisme</th><th className="th">Deadline</th><th className="th">Statut</th><th className="th">Resultat</th><th className="th">Priorite</th><th className="th">Actions</th></tr></thead>
          <tbody>{tenders.map(t => <tr key={t.id}><td className="td">{t.reference}</td><td className="td">{t.title}</td><td className="td">{t.organization}</td><td className="td">{formatDate(t.deadline)}</td><td className="td"><StatusBadge value={t.status} /></td><td className="td"><ResultBadge value={t.result} /></td><td className="td"><PriorityBadge value={t.priority} /></td><td className="td"><Link className="btn-secondary" to={`/employee/tenders/${t.id}`}><Eye size={16} /> Voir</Link></td></tr>)}</tbody></table>
        {tenders.length === 0 && <div className="py-8 text-center text-sm text-muted">Aucun marche ne vous est affecte.</div>}
      </div>
    </div>
  );
}
