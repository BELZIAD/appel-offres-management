import { FormEvent, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api, errorMessage } from '../api/client';
import { Employee, ParticipationType, PartnerCompany, TenderAssignmentRole, TenderTeam } from '../api/types';
import { useAuth } from '../state/AuthContext';
import { Toast } from '../ui/Toast';
import { assignmentRoleLabels, fullName } from '../utils/labels';

const participationLabels: Record<ParticipationType, string> = {
  SOLE_BIDDER: 'Soumissionnaire seul',
  CONSORTIUM: 'Groupement',
  JOINT_VENTURE: 'Co-entreprise',
  SUBCONTRACTOR: 'Sous-traitant',
  PARTNER: 'Partenaire'
};

interface Participation { id: number; partnerCompanyId?: number; companyName?: string; participationType: ParticipationType; ourCompany: boolean; leadCompany: boolean; sharePercentage?: number; roleDescription?: string }

const roles: TenderAssignmentRole[] = ['TENDER_MANAGER', 'PRICING_MANAGER', 'SUBMISSION_MANAGER', 'CONTRIBUTOR'];

export function TenderTeamPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const [team, setTeam] = useState<TenderTeam | null>(null);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [form, setForm] = useState({ employeeId: '', assignmentRole: 'CONTRIBUTOR' as TenderAssignmentRole });
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const admin = user?.role === 'ADMIN';
  const [participations, setParticipations] = useState<Participation[]>([]);
  const [partners, setPartners] = useState<PartnerCompany[]>([]);
  const [pForm, setPForm] = useState({ partnerCompanyId: '', participationType: 'CONSORTIUM' as ParticipationType, leadCompany: false, sharePercentage: '', roleDescription: '' });
  const loadParticipations = () => api.get<{ participations: Participation[] }>(`/tenders/${id}/participations`).then(r => setParticipations(r.data.participations)).catch(() => undefined);

  const load = () => api.get<TenderTeam>(`/tenders/${id}/team`).then(r => setTeam(r.data)).catch(e => setToast({ type: 'error', message: errorMessage(e) }));
  useEffect(() => {
    load();
    loadParticipations();
    if (admin) {
      api.get<Employee[]>('/admin/employees').then(r => setEmployees(r.data)).catch(() => undefined);
      api.get<PartnerCompany[] | { content: PartnerCompany[] }>('/admin/partners').then(r => setPartners(Array.isArray(r.data) ? r.data : r.data.content)).catch(() => undefined);
    }
  }, [id, admin]);

  async function assign(event: FormEvent) {
    event.preventDefault();
    if (!form.employeeId) return;
    try {
      await api.post(`/admin/tenders/${id}/assignments`, { employeeId: Number(form.employeeId), assignmentRole: form.assignmentRole });
      setToast({ type: 'success', message: 'Affectation ajoutee' });
      setForm({ employeeId: '', assignmentRole: 'CONTRIBUTOR' });
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }

  async function deactivate(assignmentId: number) {
    try {
      await api.delete(`/admin/tenders/${id}/assignments/${assignmentId}`);
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }

  async function addParticipation(event: FormEvent) {
    event.preventDefault();
    const ourCompany = pForm.partnerCompanyId === 'OURS';
    if (!pForm.partnerCompanyId) return;
    try {
      await api.post(`/admin/tenders/${id}/participations`, {
        partnerCompanyId: ourCompany ? null : Number(pForm.partnerCompanyId),
        participationType: pForm.participationType,
        ourCompany,
        leadCompany: pForm.leadCompany,
        sharePercentage: pForm.sharePercentage ? Number(pForm.sharePercentage) : null,
        roleDescription: pForm.roleDescription || null
      });
      setPForm({ partnerCompanyId: '', participationType: 'CONSORTIUM', leadCompany: false, sharePercentage: '', roleDescription: '' });
      loadParticipations();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }

  async function removeParticipation(participationId: number) {
    try { await api.delete(`/admin/tenders/${id}/participations/${participationId}`); loadParticipations(); }
    catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }

  const totalShare = participations.reduce((sum, p) => sum + Number(p.sharePercentage || 0), 0);

  if (!team) return <div className="panel">Chargement...</div>;
  return (
    <div className="space-y-5">
      {toast && <Toast {...toast} />}
      <h1 className="text-2xl font-semibold">Equipe AO</h1>
      {admin && (
        <form className="panel grid gap-3 md:grid-cols-3" onSubmit={assign}>
          <select className="input" value={form.employeeId} onChange={e => setForm({ ...form, employeeId: e.target.value })}>
            <option value="">Employe</option>
            {employees.map(e => <option key={e.id} value={e.id}>{fullName(e)}</option>)}
          </select>
          <select className="input" value={form.assignmentRole} onChange={e => setForm({ ...form, assignmentRole: e.target.value as TenderAssignmentRole })}>
            {roles.map(role => <option key={role} value={role}>{assignmentRoleLabels[role]}</option>)}
          </select>
          <button className="btn-primary">Ajouter</button>
        </form>
      )}
      <section className="panel overflow-x-auto">
        <table className="table">
          <thead><tr><th className="th">Employe</th><th className="th">Role</th><th className="th">Date</th><th className="th">Statut</th>{admin && <th className="th">Actions</th>}</tr></thead>
          <tbody>{team.assignments.map(a => <tr key={a.id}><td className="td">{fullName(a.employee)}</td><td className="td">{assignmentRoleLabels[a.assignmentRole]}</td><td className="td">{new Date(a.assignedAt).toLocaleDateString('fr-FR')}</td><td className="td">{a.active ? 'Actif' : 'Inactif'}</td>{admin && <td className="td">{a.active && <button className="btn-secondary" onClick={() => deactivate(a.id)}>Retirer</button>}</td>}</tr>)}</tbody>
        </table>
        {team.assignments.length === 0 && <div className="py-8 text-center text-sm text-muted">Aucune affectation.</div>}
      </section>
      <section className="panel space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-lg font-semibold">Groupement / partenaires</h2>
          {participations.length > 0 && <span className={`text-sm ${totalShare === 100 || totalShare === 0 ? 'text-muted' : 'text-amber-700'}`}>Total des parts : {totalShare} %{totalShare !== 100 && totalShare !== 0 ? ' (devrait faire 100 %)' : ''}</span>}
        </div>
        {admin && (
          <form className="grid gap-3 md:grid-cols-6" onSubmit={addParticipation}>
            <select className="input md:col-span-2" value={pForm.partnerCompanyId} onChange={e => setPForm({ ...pForm, partnerCompanyId: e.target.value })}>
              <option value="">Societe</option>
              <option value="OURS">Notre societe</option>
              {partners.filter(p => p.active).map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
            <select className="input" value={pForm.participationType} onChange={e => setPForm({ ...pForm, participationType: e.target.value as ParticipationType })}>
              {Object.entries(participationLabels).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
            <input className="input" type="number" min="0" max="100" step="0.01" placeholder="Part %" value={pForm.sharePercentage} onChange={e => setPForm({ ...pForm, sharePercentage: e.target.value })} />
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={pForm.leadCompany} onChange={e => setPForm({ ...pForm, leadCompany: e.target.checked })} /> Mandataire</label>
            <button className="btn-primary">Ajouter</button>
            <input className="input md:col-span-6" placeholder="Role / lot pris en charge (optionnel)" value={pForm.roleDescription} onChange={e => setPForm({ ...pForm, roleDescription: e.target.value })} />
          </form>
        )}
        <div className="overflow-x-auto">
          <table className="table">
            <thead><tr><th className="th">Societe</th><th className="th">Type</th><th className="th">Part</th><th className="th">Mandataire</th><th className="th">Role</th>{admin && <th className="th"></th>}</tr></thead>
            <tbody>{participations.map(p => <tr key={p.id}><td className="td">{p.ourCompany ? 'Notre societe' : p.companyName}</td><td className="td">{participationLabels[p.participationType]}</td><td className="td">{p.sharePercentage != null ? `${p.sharePercentage} %` : '-'}</td><td className="td">{p.leadCompany ? 'Oui' : 'Non'}</td><td className="td">{p.roleDescription || '-'}</td>{admin && <td className="td"><button className="btn-secondary" onClick={() => removeParticipation(p.id)}>Retirer</button></td>}</tr>)}</tbody>
          </table>
          {participations.length === 0 && <div className="py-6 text-center text-sm text-muted">Soumission en solo (aucun partenaire).</div>}
        </div>
      </section>
    </div>
  );
}
