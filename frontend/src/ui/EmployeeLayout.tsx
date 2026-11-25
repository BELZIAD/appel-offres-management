import { useEffect } from 'react';
import { syncThemeFromProfile } from '../state/theme';
import { BarChart3, Bell, BriefcaseBusiness, Calculator, GitBranch, Info, KanbanSquare, LogOut, Send, Star, User } from 'lucide-react';
import { NavLink, Outlet } from 'react-router-dom';
import { assetUrl } from '../api/client';
import { useAuth } from '../state/AuthContext';
import { useSettings } from '../state/SettingsContext';
import { ROUTES } from '../routes/paths';
import { NotificationBell } from './NotificationBell';
import { GlobalSearch } from './GlobalSearch';
import { AppFooter } from './AppFooter';

export function EmployeeLayout() {
  const { user, logout } = useAuth();
  const { settings } = useSettings();
  useEffect(() => { syncThemeFromProfile(); }, []);
  const initials = `${user?.firstName?.[0] ?? ''}${user?.lastName?.[0] ?? ''}`;
  const navClass = ({ isActive }: { isActive: boolean }) => `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'text-white' : 'text-slate-700 hover:bg-slate-100'}`;
  const navStyle = ({ isActive }: { isActive: boolean }) => isActive ? { background: 'var(--color-primary)' } : undefined;
  return (
    <div className="min-h-screen md:flex">
      <aside className="border-r border-line md:w-64" style={{ background: 'var(--color-sidebar)' }}>
        <div className="border-b border-line p-5">
          <div className="flex items-center gap-3">
            {settings.companyLogo && <img src={assetUrl(settings.companyLogo)} alt={settings.applicationName} className="h-10 w-10 object-contain" />}
            <div>
              <div className="text-lg font-semibold">{settings.applicationName}</div>
              <div className="text-sm text-muted">Espace employe</div>
            </div>
          </div>
        </div>
        <nav className="space-y-1 p-3">
          <NavLink to={ROUTES.EMPLOYEE.DASHBOARD} className={navClass} style={navStyle}><BarChart3 size={18} /> Dashboard</NavLink>
          <NavLink to={ROUTES.EMPLOYEE.TENDERS} className={navClass} style={navStyle}><BriefcaseBusiness size={18} /> Mes appels d'offres</NavLink>
          <NavLink to={ROUTES.EMPLOYEE.TASKS} className={navClass} style={navStyle}><GitBranch size={18} /> Mes taches</NavLink>
          <NavLink to={ROUTES.EMPLOYEE.PRICING} className={navClass} style={navStyle}><Calculator size={18} /> Mes chiffrages</NavLink>
          <NavLink to={ROUTES.EMPLOYEE.SUBMISSIONS} className={navClass} style={navStyle}><Send size={18} /> Mes depots</NavLink>
          <NavLink to={ROUTES.EMPLOYEE.TASKS_KANBAN} className={navClass} style={navStyle}><KanbanSquare size={18} /> Kanban</NavLink>
          <NavLink to={ROUTES.EMPLOYEE.FAVORITES} className={navClass} style={navStyle}><Star size={18} /> Mes favoris</NavLink>
          <NavLink to={ROUTES.EMPLOYEE.NOTIFICATIONS} className={navClass} style={navStyle}><Bell size={18} /> Notifications</NavLink>
          <NavLink to={ROUTES.EMPLOYEE.PROFILE} className={navClass} style={navStyle}><User size={18} /> Mon profil</NavLink>
          <NavLink to={ROUTES.ABOUT} className={navClass} style={navStyle}><Info size={18} /> A propos</NavLink>
          <button className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100" onClick={logout}><LogOut size={18} /> Deconnexion</button>
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
