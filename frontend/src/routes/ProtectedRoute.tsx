import { Navigate } from 'react-router-dom';
import { Role } from '../api/types';
import { ForbiddenPage } from '../pages/ForbiddenPage';
import { useAuth } from '../state/AuthContext';
import { ROUTES } from './paths';

export function ProtectedRoute({ role, children }: { role: Role; children: React.ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) {
    return <div className="flex min-h-screen items-center justify-center text-slate-600">Chargement...</div>;
  }
  if (!user) {
    return <Navigate to={ROUTES.LOGIN} replace />;
  }
  if (user.role !== role) {
    return <ForbiddenPage />;
  }
  return children;
}
