import { ArrowLeft, LayoutDashboard } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../state/AuthContext';
import { ROUTES } from '../routes/paths';

export function ForbiddenPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const dashboard = user?.role === 'ADMIN' ? ROUTES.ADMIN.DASHBOARD : ROUTES.EMPLOYEE.DASHBOARD;

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      <section className="w-full max-w-xl rounded-lg border border-line bg-white p-8 text-center shadow-sm">
        <div className="text-sm font-semibold uppercase text-red-700">403</div>
        <h1 className="mt-2 text-2xl font-semibold">Acces refuse</h1>
        <p className="mx-auto mt-3 max-w-md text-sm text-muted">
          Vous ne disposez pas des autorisations necessaires pour acceder a cette page.
        </p>
        <div className="mt-6 flex flex-wrap justify-center gap-2">
          <button className="btn-secondary" onClick={() => navigate(-1)}><ArrowLeft size={16} /> Retour</button>
          <button className="btn-primary" onClick={() => navigate(dashboard)}><LayoutDashboard size={16} /> Retour au tableau de bord</button>
        </div>
      </section>
    </div>
  );
}
