import { Navigate, useParams } from 'react-router-dom';
import { useAuth } from '../state/AuthContext';
import { ROUTES, adminTenderRoute, employeeTenderRoute } from './paths';

export function RoleHomeRedirect() {
  const { user, loading } = useAuth();
  if (loading) return <div className="flex min-h-screen items-center justify-center text-slate-600">Chargement...</div>;
  if (!user) return <Navigate to={ROUTES.LOGIN} replace />;
  return <Navigate to={user.role === 'ADMIN' ? ROUTES.ADMIN.DASHBOARD : ROUTES.EMPLOYEE.DASHBOARD} replace />;
}

export function LegacyTenderRedirect() {
  const { user, loading } = useAuth();
  const { id, '*': suffix } = useParams();
  if (loading) return <div className="flex min-h-screen items-center justify-center text-slate-600">Chargement...</div>;
  if (!user) return <Navigate to={ROUTES.LOGIN} replace />;
  const base = user.role === 'ADMIN' ? adminTenderRoute(id ?? '') : employeeTenderRoute(id ?? '');
  return <Navigate to={`${base}${suffix ? `/${suffix}` : ''}`} replace />;
}
