import { Edit, Plus, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, errorMessage } from '../../api/client';
import { Employee } from '../../api/types';
import { ConfirmModal } from '../../ui/ConfirmModal';
import { Toast } from '../../ui/Toast';

export function EmployeesPage() {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const load = () => api.get<Employee[]>('/admin/employees').then(r => setEmployees(r.data)).catch(e => setToast({ type: 'error', message: errorMessage(e) }));
  useEffect(() => { load(); }, []);
  async function remove() {
    if (!deleteId) return;
    try {
      await api.delete(`/admin/employees/${deleteId}`);
      setToast({ type: 'success', message: 'Employe supprime' });
      setDeleteId(null);
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  return (
    <div className="space-y-5">
      {toast && <Toast {...toast} />}
      {deleteId && <ConfirmModal title="Supprimer l'employe" message="Cette action est definitive." onCancel={() => setDeleteId(null)} onConfirm={remove} />}
      <div className="flex items-center justify-between"><h1 className="text-2xl font-semibold">Employes</h1><Link to="/admin/employees/new" className="btn-primary"><Plus size={18} /> Nouvel employe</Link></div>
      <div className="panel overflow-x-auto">
        <table className="table">
          <thead><tr><th className="th">Nom</th><th className="th">Prenom</th><th className="th">Email</th><th className="th">Statut</th><th className="th">Marches affectes</th><th className="th">Actions</th></tr></thead>
          <tbody>{employees.map(e => <tr key={e.id}><td className="td">{e.lastName}</td><td className="td">{e.firstName}</td><td className="td">{e.email}</td><td className="td">{e.enabled ? 'Actif' : 'Inactif'}</td><td className="td">{e.assignedTenderCount}</td><td className="td"><div className="flex gap-2"><Link className="btn-secondary" to={`/admin/employees/${e.id}`}><Edit size={16} /></Link><button className="btn-secondary text-red-700" onClick={() => setDeleteId(e.id)}><Trash2 size={16} /></button></div></td></tr>)}</tbody>
        </table>
        {employees.length === 0 && <div className="py-8 text-center text-sm text-muted">Aucun employe cree.</div>}
      </div>
    </div>
  );
}
