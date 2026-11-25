import { Search } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { api } from '../api/client';
import { SearchResults } from '../api/types';

export function GlobalSearch() {
  const [q, setQ] = useState('');
  const [results, setResults] = useState<SearchResults | null>(null);
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const admin = location.pathname.startsWith('/admin');
  const timer = useRef<number | undefined>();

  useEffect(() => {
    window.clearTimeout(timer.current);
    if (q.trim().length < 2) { setResults(null); return; }
    timer.current = window.setTimeout(() => {
      api.get<SearchResults>(`/search?q=${encodeURIComponent(q)}`).then(r => { setResults(r.data); setOpen(true); }).catch(() => setResults(null));
    }, 350);
    return () => window.clearTimeout(timer.current);
  }, [q]);

  const prefix = admin ? '/admin' : '/employee';
  return <div className="relative w-full max-w-xl">
    <Search className="pointer-events-none absolute left-3 top-2.5 text-slate-400" size={18} />
    <input className="input w-full pl-10" placeholder="Rechercher un AO, une tache, un document..." value={q} onChange={e => setQ(e.target.value)} onFocus={() => setOpen(true)} />
    {open && results && <div className="absolute left-0 right-0 z-20 mt-2 max-h-96 overflow-auto rounded-md border border-line bg-white p-3 shadow-lg">
      <Group title="Appels d'offres">{results.tenders.map(t => <Link key={t.id} className="block rounded px-2 py-1 text-sm hover:bg-slate-50" to={`${prefix}/tenders/${t.id}`} onClick={() => setOpen(false)}>{t.reference} - {t.title}</Link>)}</Group>
      <Group title="Taches">{results.tasks.map(t => <div key={t.id} className="rounded px-2 py-1 text-sm">{t.title}<div className="text-xs text-muted">{t.tender.reference}</div></div>)}</Group>
      <Group title="Documents">{results.documents.map(d => <Link key={d.id} className="block rounded px-2 py-1 text-sm hover:bg-slate-50" to={`${prefix}/tenders/${d.tenderId}`} onClick={() => setOpen(false)}>{d.originalFileName}</Link>)}</Group>
      {results.users.length > 0 && <Group title="Employes">{results.users.map(u => <Link key={u.id} className="block rounded px-2 py-1 text-sm hover:bg-slate-50" to={`/admin/employees/${u.id}`} onClick={() => setOpen(false)}>{u.firstName} {u.lastName}</Link>)}</Group>}
      {results.tenders.length + results.tasks.length + results.documents.length + results.users.length === 0 && <div className="p-2 text-sm text-muted">Aucun resultat.</div>}
    </div>}
  </div>;
}

function Group({ title, children }: { title: string; children: ReactNode }) {
  return <div className="mb-2 last:mb-0"><div className="mb-1 text-xs font-semibold uppercase text-muted">{title}</div>{children}</div>;
}
