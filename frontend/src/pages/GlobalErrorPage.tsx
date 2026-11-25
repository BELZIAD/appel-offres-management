import { isRouteErrorResponse, useRouteError } from 'react-router-dom';
import { ForbiddenPage } from './ForbiddenPage';
import { NotFoundPage } from './NotFoundPage';

export function GlobalErrorPage() {
  const error = useRouteError();

  if (isRouteErrorResponse(error)) {
    if (error.status === 404) return <NotFoundPage />;
    if (error.status === 403) return <ForbiddenPage />;
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      <section className="w-full max-w-xl rounded-lg border border-line bg-white p-8 text-center shadow-sm">
        <div className="text-sm font-semibold uppercase text-red-700">500</div>
        <h1 className="mt-2 text-2xl font-semibold">Erreur inattendue</h1>
        <p className="mx-auto mt-3 max-w-md text-sm text-muted">
          Une erreur est survenue. Veuillez reessayer ou revenir au tableau de bord.
        </p>
      </section>
    </div>
  );
}
