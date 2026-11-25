import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { api, errorMessage } from '../api/client';
import { Page, TaskStatus, TenderTask } from '../api/types';
import { TaskPriorityBadge } from '../ui/Badge';
import { Toast } from '../ui/Toast';
import { formatDate, taskStatusLabels } from '../utils/labels';

const statuses: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'BLOCKED', 'DONE'];

export function KanbanPage() {
  const admin = useLocation().pathname.startsWith('/admin');
  const [tasks, setTasks] = useState<TenderTask[]>([]);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const load = () => api.get<Page<TenderTask>>(`${admin ? '/admin' : '/employee'}/tasks?size=200&sort=deadline,asc`).then(r => setTasks(r.data.content));
  useEffect(() => { load(); }, [admin]);
  async function move(task: TenderTask, status: TaskStatus) {
    const before = tasks;
    setTasks(tasks.map(t => t.id === task.id ? { ...t, status } : t));
    try { await api.patch(`${admin ? '/admin' : '/employee'}/tasks/${task.id}/status`, { status }); load(); } catch (e) { setTasks(before); setToast({ type: 'error', message: errorMessage(e) }); }
  }
  return <div className="space-y-5">
    {toast && <Toast {...toast} />}
    <h1 className="text-2xl font-semibold">Kanban</h1>
    <div className="grid gap-4 lg:grid-cols-4">
      {statuses.map(status => <section key={status} className="min-h-96 rounded-md border border-line bg-white p-3">
        <h2 className="mb-3 font-semibold">{taskStatusLabels[status]}</h2>
        <div className="space-y-3">{tasks.filter(t => t.status === status).map(task => <article key={task.id} draggable onDragStart={e => e.dataTransfer.setData('taskId', String(task.id))} className="rounded-md border border-line p-3 shadow-sm">
          <div className="font-medium">{task.title}</div>
          <Link className="text-xs text-slate-700 underline" to={`${admin ? '/admin' : '/employee'}/tenders/${task.tender.id}`}>{task.tender.reference}</Link>
          <div className="mt-2 flex items-center justify-between gap-2"><TaskPriorityBadge value={task.priority} /><span className="text-xs text-muted">{formatDate(task.deadline)}</span></div>
          <select className="input mt-3" value={task.status} onChange={e => move(task, e.target.value as TaskStatus)}>{statuses.map(s => <option key={s} value={s}>{taskStatusLabels[s]}</option>)}</select>
        </article>)}</div>
        <div className="mt-3 h-10 rounded border border-dashed border-slate-300" onDragOver={e => e.preventDefault()} onDrop={e => { const id = Number(e.dataTransfer.getData('taskId')); const task = tasks.find(t => t.id === id); if (task) move(task, status); }} />
      </section>)}
    </div>
  </div>;
}
