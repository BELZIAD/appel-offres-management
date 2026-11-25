import { useEffect, useState } from 'react';
import { api, errorMessage } from '../api/client';
import { NotificationItem, Page } from '../api/types';
import { Toast } from '../ui/Toast';

export function NotificationsPage() {
  const [page, setPage] = useState<Page<NotificationItem> | null>(null);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [pageNumber, setPageNumber] = useState(0);
  const [size, setSize] = useState(10);
  const load = () => api.get<Page<NotificationItem>>(`/notifications?page=${pageNumber}&size=${size}`).then(r => setPage(r.data)).catch(e => setToast({ type: 'error', message: errorMessage(e) }));
  useEffect(() => { load(); }, [pageNumber, size]);
  async function markRead(id: number) {
    await api.patch(`/notifications/${id}/read`);
    load();
  }
  return (
    <div className="space-y-5">
      {toast && <Toast {...toast} />}
      <div className="flex items-center justify-between"><h1 className="text-2xl font-semibold">Notifications</h1><select className="input w-28" value={size} onChange={e => { setSize(Number(e.target.value)); setPageNumber(0); }}><option>10</option><option>20</option><option>50</option></select></div>
      <div className="panel overflow-x-auto">
        <table className="table"><thead><tr><th className="th">Type</th><th className="th">Titre</th><th className="th">Message</th><th className="th">Date</th><th className="th">Statut</th></tr></thead>
          <tbody>{page?.content.map(n => <tr key={n.id} className={!n.read ? 'bg-sky-50' : ''}><td className="td">{n.type}</td><td className="td font-medium">{n.title}</td><td className="td">{n.message}</td><td className="td">{new Date(n.createdAt).toLocaleString('fr-FR')}</td><td className="td">{n.read ? 'Lu' : <button className="btn-secondary" onClick={() => markRead(n.id)}>Marquer lu</button>}</td></tr>)}</tbody></table>
        {page?.content.length === 0 && <div className="py-8 text-center text-sm text-muted">Aucune notification.</div>}
      </div>
      {page && <Pager page={page} setPage={setPageNumber} />}
    </div>
  );
}

function Pager<T>({ page, setPage }: { page: Page<T>; setPage: (value: number) => void }) {
  return <div className="flex items-center justify-end gap-2"><button className="btn-secondary" disabled={page.number === 0} onClick={() => setPage(page.number - 1)}>Precedent</button><span className="text-sm">Page {page.number + 1} / {Math.max(page.totalPages, 1)}</span><button className="btn-secondary" disabled={page.number + 1 >= page.totalPages} onClick={() => setPage(page.number + 1)}>Suivant</button></div>;
}
