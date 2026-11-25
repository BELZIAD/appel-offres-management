import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import { TenderSubmission } from '../../api/types';
import { submissionStatusLabels } from '../../utils/labels';

export function EmployeeSubmissionsPage() {
  const [items, setItems] = useState<TenderSubmission[]>([]);
  useEffect(() => { api.get<TenderSubmission[]>('/employee/submissions').then(r => setItems(r.data)); }, []);
  return <div className="space-y-5"><h1 className="text-2xl font-semibold">Mes depots</h1><section className="panel overflow-x-auto"><table className="table"><thead><tr><th className="th">AO</th><th className="th">Etat dossier</th><th className="th">Etat depot</th><th className="th">Plateforme</th><th className="th">Actions</th></tr></thead><tbody>{items.map(s => <tr key={s.id}><td className="td">#{s.tenderId}</td><td className="td">{s.readiness}</td><td className="td">{submissionStatusLabels[s.status]}</td><td className="td">{s.platform}</td><td className="td"><Link className="btn-secondary" to={`/employee/tenders/${s.tenderId}`}>Ouvrir</Link></td></tr>)}</tbody></table>{items.length === 0 && <div className="py-8 text-center text-sm text-muted">Aucun depot affecte.</div>}</section></div>;
}
