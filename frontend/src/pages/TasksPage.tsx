import { useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { Edit, Plus, Save, Trash2, X } from 'lucide-react';
import { Link } from 'react-router-dom';
import { api, errorMessage } from '../api/client';
import { Employee, Page, TaskPriority, TaskStatus, Tender, TenderTask } from '../api/types';
import { TaskPriorityBadge, TaskStatusBadge } from '../ui/Badge';
import { Toast } from '../ui/Toast';
import { deadlineText, formatDate, fullName, taskPriorityLabels, taskStatusLabels } from '../utils/labels';

const statuses: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'BLOCKED', 'DONE'];
const priorities: TaskPriority[] = ['LOW', 'NORMAL', 'HIGH', 'URGENT'];
const emptyForm = { title: '', description: '', tenderId: '', assignedEmployeeId: '', priority: 'NORMAL', status: 'TODO', deadline: '' };

export function AdminTasksPage() {
  const [page, setPage] = useState<Page<TenderTask> | null>(null);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [tenders, setTenders] = useState<Tender[]>([]);
  const [filters, setFilters] = useState({ search: '', status: '', priority: '', employeeId: '', deadlineFrom: '', deadlineTo: '' });
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const load = () => {
    const params = new URLSearchParams({ size: '30', sort: 'deadline,asc' });
    Object.entries(filters).forEach(([k, v]) => v && params.set(k, v));
    api.get<Page<TenderTask>>(`/admin/tasks?${params}`).then(r => setPage(r.data));
  };

  useEffect(load, [filters]);
  useEffect(() => {
    api.get<Employee[]>('/admin/employees').then(r => setEmployees(r.data)).catch(() => undefined);
    api.get<Page<Tender>>('/admin/tenders?size=100&sort=deadline,asc').then(r => setTenders(r.data.content)).catch(() => undefined);
  }, []);

  async function saveTask() {
    try {
      const payload = { ...form, tenderId: Number(form.tenderId), assignedEmployeeId: Number(form.assignedEmployeeId) };
      if (editingId) {
        await api.put(`/admin/tasks/${editingId}`, payload);
        setToast({ type: 'success', message: 'Tache modifiee' });
      } else {
        await api.post('/admin/tasks', payload);
        setToast({ type: 'success', message: 'Tache creee' });
      }
      reset();
      load();
    } catch (e) {
      setToast({ type: 'error', message: errorMessage(e) });
    }
  }

  function editTask(task: TenderTask) {
    setEditingId(task.id);
    setForm({
      title: task.title,
      description: task.description || '',
      tenderId: String(task.tender.id),
      assignedEmployeeId: task.assignedEmployee ? String(task.assignedEmployee.id) : '',
      priority: task.priority,
      status: task.status,
      deadline: task.deadline
    });
  }

  function reset() {
    setEditingId(null);
    setForm(emptyForm);
  }

  async function changeStatus(task: TenderTask, status: TaskStatus) {
    try {
      await api.patch(`/admin/tasks/${task.id}/status`, { status });
      load();
    } catch (e) {
      setToast({ type: 'error', message: errorMessage(e) });
    }
  }

  async function removeTask(task: TenderTask) {
    if (!window.confirm(`Supprimer la tache "${task.title}" ?`)) return;
    try {
      await api.delete(`/admin/tasks/${task.id}`);
      setToast({ type: 'success', message: 'Tache supprimee' });
      if (editingId === task.id) reset();
      load();
    } catch (e) {
      setToast({ type: 'error', message: errorMessage(e) });
    }
  }

  return <TaskListShell title="Taches" page={page} filters={filters} setFilters={setFilters} admin onStatus={changeStatus} onEdit={editTask} onDelete={removeTask}>
    {toast && <Toast {...toast} />}
    <section className="panel">
      <h2 className="mb-4 text-lg font-semibold">{editingId ? 'Modifier la tache' : 'Nouvelle tache'}</h2>
      <div className="grid gap-3 md:grid-cols-6">
        <input className="input md:col-span-2" placeholder="Titre" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} />
        <select className="input" value={form.tenderId} onChange={e => setForm({ ...form, tenderId: e.target.value })}><option value="">AO</option>{tenders.map(t => <option key={t.id} value={t.id}>{t.reference}</option>)}</select>
        <select className="input" value={form.assignedEmployeeId} onChange={e => setForm({ ...form, assignedEmployeeId: e.target.value })}><option value="">Employe</option>{employees.map(e => <option key={e.id} value={e.id}>{fullName(e)}</option>)}</select>
        <select className="input" value={form.priority} onChange={e => setForm({ ...form, priority: e.target.value })}>{priorities.map(p => <option key={p} value={p}>{taskPriorityLabels[p]}</option>)}</select>
        <select className="input" value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>{statuses.map(s => <option key={s} value={s}>{taskStatusLabels[s]}</option>)}</select>
        <input className="input" type="date" value={form.deadline} onChange={e => setForm({ ...form, deadline: e.target.value })} />
        <textarea className="input md:col-span-5" placeholder="Description" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
        <div className="flex flex-wrap gap-2 md:col-span-6">
          <button type="button" className="btn-primary" onClick={saveTask}>{editingId ? <Save size={16} /> : <Plus size={16} />} {editingId ? 'Enregistrer' : 'Ajouter tache'}</button>
          {editingId && <button type="button" className="btn-secondary" onClick={reset}><X size={16} /> Annuler</button>}
        </div>
      </div>
    </section>
  </TaskListShell>;
}

export function EmployeeTasksPage() {
  const [page, setPage] = useState<Page<TenderTask> | null>(null);
  const [filters, setFilters] = useState({ search: '', status: '', priority: '', employeeId: '', deadlineFrom: '', deadlineTo: '' });
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const load = () => {
    const params = new URLSearchParams({ size: '100', sort: 'deadline,asc' });
    Object.entries(filters).forEach(([k, v]) => v && k !== 'employeeId' && params.set(k, v));
    api.get<Page<TenderTask>>(`/employee/tasks?${params}`).then(r => setPage(r.data));
  };
  useEffect(load, [filters]);
  async function changeStatus(task: TenderTask, status: TaskStatus) {
    try { await api.patch(`/employee/tasks/${task.id}/status`, { status }); load(); } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  const tasks = page?.content ?? [];
  const kpi = useMemo(() => ({
    total: tasks.length,
    todo: tasks.filter(t => t.status === 'TODO').length,
    progress: tasks.filter(t => t.status === 'IN_PROGRESS').length,
    overdue: tasks.filter(t => t.overdue).length,
    done: tasks.filter(t => t.status === 'DONE').length
  }), [tasks]);
  return <div className="space-y-5">
    {toast && <Toast {...toast} />}
    <h1 className="text-2xl font-semibold">Mes taches</h1>
    <div className="grid gap-3 md:grid-cols-5">{Object.entries(kpi).map(([k, v]) => <div key={k} className="panel"><div className="text-xs uppercase text-muted">{k}</div><div className="text-2xl font-semibold">{v}</div></div>)}</div>
    <TaskListShell title="" page={page} filters={filters} setFilters={setFilters} onStatus={changeStatus} />
  </div>;
}

function TaskListShell({ title, page, filters, setFilters, children, admin, onStatus, onEdit, onDelete }: { title: string; page: Page<TenderTask> | null; filters: any; setFilters: (v: any) => void; children?: ReactNode; admin?: boolean; onStatus: (task: TenderTask, status: TaskStatus) => void; onEdit?: (task: TenderTask) => void; onDelete?: (task: TenderTask) => void }) {
  return <div className="space-y-5">
    {title && <h1 className="text-2xl font-semibold">{title}</h1>}
    {children}
    <section className="panel grid gap-3 md:grid-cols-6">
      <input className="input md:col-span-2" placeholder="Rechercher" value={filters.search} onChange={e => setFilters({ ...filters, search: e.target.value })} />
      <select className="input" value={filters.status} onChange={e => setFilters({ ...filters, status: e.target.value })}><option value="">Tous statuts</option>{statuses.map(s => <option key={s} value={s}>{taskStatusLabels[s]}</option>)}</select>
      <select className="input" value={filters.priority} onChange={e => setFilters({ ...filters, priority: e.target.value })}><option value="">Toutes priorites</option>{priorities.map(p => <option key={p} value={p}>{taskPriorityLabels[p]}</option>)}</select>
      <input className="input" type="date" value={filters.deadlineFrom} onChange={e => setFilters({ ...filters, deadlineFrom: e.target.value })} />
      <input className="input" type="date" value={filters.deadlineTo} onChange={e => setFilters({ ...filters, deadlineTo: e.target.value })} />
    </section>
    <section className="panel overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead><tr className="border-b border-line text-xs uppercase text-muted"><th className="py-2">Titre</th><th>AO</th><th>Responsable</th><th>Priorite</th><th>Statut</th><th>Deadline</th><th>Actions</th></tr></thead>
        <tbody>{page?.content.map(task => <tr key={task.id} className="border-b border-line last:border-0"><td className="py-3 font-medium">{task.title}</td><td><Link className="text-slate-900 underline" to={`${admin ? '/admin' : '/employee'}/tenders/${task.tender.id}`}>{task.tender.reference}</Link></td><td>{fullName(task.assignedEmployee)}</td><td><TaskPriorityBadge value={task.priority} /></td><td><TaskStatusBadge value={task.status} /></td><td>{formatDate(task.deadline)}<div className={task.overdue ? 'text-xs text-red-700' : 'text-xs text-muted'}>{deadlineText(task.daysRemaining)}</div></td><td><div className="flex flex-wrap gap-2"><select className="input min-w-36" value={task.status} onChange={e => onStatus(task, e.target.value as TaskStatus)}>{statuses.map(s => <option key={s} value={s}>{taskStatusLabels[s]}</option>)}</select>{admin && onEdit && <button className="btn-secondary" onClick={() => onEdit(task)}><Edit size={16} /></button>}{admin && onDelete && <button className="btn-secondary text-red-700" onClick={() => onDelete(task)}><Trash2 size={16} /></button>}</div></td></tr>)}</tbody>
      </table>
      {page?.content.length === 0 && <div className="py-8 text-center text-sm text-muted">Aucune tache.</div>}
    </section>
  </div>;
}
