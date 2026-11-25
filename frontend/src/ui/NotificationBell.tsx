import { Bell } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { NotificationItem, Page } from '../api/types';
import { useAuth } from '../state/AuthContext';
import { ROUTES } from '../routes/paths';

export function NotificationBell() {
  const { user } = useAuth();
  const [open, setOpen] = useState(false);
  const [count, setCount] = useState(0);
  const [items, setItems] = useState<NotificationItem[]>([]);

  const load = () => {
    api.get<{ count: number }>('/notifications/unread-count').then(r => setCount(r.data.count)).catch(() => undefined);
    api.get<Page<NotificationItem>>('/notifications?size=5').then(r => setItems(r.data.content)).catch(() => undefined);
  };

  useEffect(() => { if (user) load(); }, [user]);

  async function readAll() {
    await api.patch('/notifications/read-all');
    load();
  }

  const target = user?.role === 'ADMIN' ? ROUTES.ADMIN.NOTIFICATIONS : ROUTES.EMPLOYEE.NOTIFICATIONS;
  return (
    <div className="relative">
      <button className="btn-secondary relative px-2" onClick={() => setOpen(!open)} title="Notifications">
        <Bell size={18} />
        {count > 0 && <span className="absolute -right-1 -top-1 rounded-full bg-red-600 px-1.5 text-[10px] font-semibold text-white">{count}</span>}
      </button>
      {open && (
        <div className="absolute right-0 z-30 mt-2 w-80 rounded-lg border border-line bg-white shadow-lg">
          <div className="flex items-center justify-between border-b border-line p-3">
            <div className="font-semibold">Notifications</div>
            <button className="text-xs text-slate-600 hover:text-ink" onClick={readAll}>Tout marquer comme lu</button>
          </div>
          <div className="max-h-80 overflow-auto">
            {items.map(item => (
              <Link key={item.id} to={target} className={`block border-b border-slate-100 p-3 text-sm hover:bg-slate-50 ${!item.read ? 'bg-sky-50' : ''}`}>
                <div className="font-medium">{item.title}</div>
                <div className="mt-1 text-xs text-slate-600">{item.message}</div>
              </Link>
            ))}
            {items.length === 0 && <div className="p-5 text-center text-sm text-muted">Aucune notification.</div>}
          </div>
          <Link to={target} className="block p-3 text-center text-sm font-medium text-ink hover:bg-slate-50">Voir toutes</Link>
        </div>
      )}
    </div>
  );
}
