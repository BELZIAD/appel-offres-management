import { useEffect } from 'react';
import { syncThemeFromProfile } from '../state/theme';
import { BarChart3, Bell, BriefcaseBusiness, Building2, ChartColumn, CheckSquare, Clock, DatabaseBackup, FileDown, GitBranch, Handshake, History, Info, KanbanSquare, LogOut, Star, User, Users } from 'lucide-react';
import { NavLink, Outlet } from 'react-router-dom';
import { assetUrl } from '../api/client';
import { useAuth } from '../state/AuthContext';
import { useSettings } from '../state/SettingsContext';
import { ROUTES } from '../routes/paths';
import { NotificationBell } from './NotificationBell';
import { GlobalSearch } from './GlobalSearch';
import { AppFooter } from './AppFooter';

const links = [
  { to: ROUTES.ADMIN.DASHBOARD, label: 'Dashboard', icon: BarChart3 },
  { to: ROUTES.ADMIN.TENDERS, label: "Appels d'offres", icon: BriefcaseBusiness },
  { to: ROUTES.ADMIN.DEADLINES, label: 'Echeances', icon: Clock },
  { to: ROUTES.ADMIN.TASKS, label: 'Taches', icon: GitBranch },
  { to: ROUTES.ADMIN.TASKS_KANBAN, label: 'Kanban', icon: KanbanSquare },
  { to: ROUTES.ADMIN.FAVORITES, label: 'Favoris', icon: Star },
  { to: ROUTES.ADMIN.PARTNERS, label: 'Partenaires', icon: Handshake },
  { to: ROUTES.ADMIN.CHECKLISTS, label: 'Checklists', icon: CheckSquare },
  { to: ROUTES.ADMIN.EMPLOYEES, label: 'Employes', icon: Users },
  { to: ROUTES.ADMIN.PERFORMANCE_EMPLOYEES, label: 'Performance', icon: ChartColumn },
  { to: ROUTES.ADMIN.ORGANIZATIONS, label: 'Organismes', icon: BarChart3 },
  { to: ROUTES.ADMIN.REPORTS, label: 'Rapports', icon: FileDown },
  { to: ROUTES.ADMIN.ACTIVITY, label: 'Activite', icon: History },
  { to: ROUTES.ADMIN.NOTIFICATIONS, label: 'Notifications', icon: Bell },
  { to: ROUTES.ADMIN.HISTORY, label: 'Historique', icon: History },
  { to: ROUTES.ADMIN.COMPANY, label: 'Entreprise', icon: Building2 },
  { to: ROUTES.ADMIN.BACKUPS, label: 'Sauvegardes', icon: DatabaseBackup },
  { to: ROUTES.ADMIN.PROFILE, label: 'Mon profil', icon: User },
  { to: ROUTES.ABOUT, label: 'A propos', icon: Info }
];

export function AdminLayout() {
  const { user, logout } = useAuth();
  const { settings } = useSettings();
  useEffect(() => { syncThemeFromProfile(); }, []);
  const initials = `${user?.firstName?.[0] ?? ''}${user?.lastName?.[0] ?? ''}`;
  return (
    <div className="min-h-screen md:flex">
      <aside className="border-r border-line md:w-64" style={{ background: 'var(--color-sidebar)' }}>
        <div className="border-b border-line p-5">
          <div className="flex items-center gap-3">
            {settings.companyLogo && <img src={assetUrl(settings.companyLogo)} alt={settings.applicationName} className="h-10 w-10 object-contain" />}
            <div>
              <div className="text-lg font-semibold">{settings.applicationName}</div>
              <div className="text-sm text-muted">Administration</div>
            </div>
          </div>
        </div>
        <nav className="space-y-1 p-3">
          {links.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className={({ isActive }) => `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'text-white' : 'text-slate-700 hover:bg-slate-100'}`} style={({ isActive }) => isActive ? { background: 'var(--color-primary)' } : undefined}>
              <Icon size={18} /> {label}
            </NavLink>
          ))}
          <button className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100" onClick={logout}>
            <LogOut size={18} /> Deconnexion
          </button>
        </nav>
      </aside>
      <main className="min-w-0 flex-1">
        <header className="flex items-center justify-between gap-3 border-b border-line px-6 py-4" style={{ background: 'var(--color-header)' }}>
          <GlobalSearch />
          <div className="flex items-center gap-3">
          <NotificationBell />
          <div className="text-right">
            <div className="text-sm font-semibold">{user?.firstName} {user?.lastName}</div>
            <div className="text-xs text-muted">{user?.role}</div>
          </div>
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-900 text-sm font-semibold text-white">{initials}</div>
          </div>
        </header>
        <div className="p-6"><Outlet /></div>
        <AppFooter />
      </main>
    </div>
  );
}
