import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, errorMessage } from '../api/client';
import { DocumentAnalysis, DocumentSearchHit, ExtractedField, ExtractedRequirement, Tender } from '../api/types';
import { AnalysisBadge } from '../ui/Badge';
import { Toast } from '../ui/Toast';
import { extractionFieldLabels, formatDate } from '../utils/labels';

export function TenderAnalysisPage() {
  const { id } = useParams();
  const [tender, setTender] = useState<Tender | null>(null);
  const [analyses, setAnalyses] = useState<DocumentAnalysis[]>([]);
  const [selected, setSelected] = useState<DocumentAnalysis | null>(null);
  const [search, setSearch] = useState('');
  const [hits, setHits] = useState<DocumentSearchHit[]>([]);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const load = () => {
    api.get<Tender>(`/tenders/${id}`).then(r => setTender(r.data));
    api.get<DocumentAnalysis[]>(`/tenders/${id}/analyses`).then(r => {
      setAnalyses(r.data);
      setSelected(current => current ? r.data.find(a => a.id === current.id) ?? current : r.data[0] ?? null);
    }).catch(() => undefined);
  };
  useEffect(load, [id]);
  useEffect(() => {
    if (!analyses.some(a => a.status === 'PROCESSING' || a.status === 'PENDING')) return;
    const timer = window.setInterval(load, 1500);
    return () => window.clearInterval(timer);
  }, [analyses]);

  const latestByDocument = useMemo(() => {
    const map = new Map<number, DocumentAnalysis>();
    analyses.forEach(a => { if (!map.has(a.documentId)) map.set(a.documentId, a); });
    return map;
  }, [analyses]);

  async function analyze(documentId: number) {
    try {
      const response = await api.post<DocumentAnalysis>(`/tenders/${id}/documents/${documentId}/analyze`);
      setSelected(response.data);
      setToast({ type: 'success', message: 'Analyse lancee' });
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }

  async function applyField(field: ExtractedField, value = field.value ?? '') {
    try {
      const response = await api.post<DocumentAnalysis>(`/tenders/${id}/analysis/apply-field`, { field: field.field, value, extractedFieldId: field.id });
      setSelected(response.data);
      setToast({ type: 'success', message: 'Information appliquee apres validation' });
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }

  async function applyChecklist(requirements: ExtractedRequirement[]) {
    if (!selected) return;
    try {
      await api.post(`/tenders/${id}/analysis/apply-checklist`, { analysisId: selected.id, requirementIds: requirements.map(r => r.id) });
      setToast({ type: 'success', message: 'Checklist proposee appliquee' });
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }

  async function searchDocument() {
    if (!selected || !search.trim()) return;
    try {
      const response = await api.get<DocumentSearchHit[]>(`/documents/${selected.documentId}/search?q=${encodeURIComponent(search)}`);
      setHits(response.data);
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }

  if (!tender) return <div>Chargement...</div>;
  return <div className="space-y-5">
    {toast && <Toast {...toast} />}
    <div className="flex items-start justify-between gap-3">
      <div><h1 className="text-2xl font-semibold">Analyse documentaire</h1><p className="text-sm text-muted">{tender.reference} - {tender.title}</p></div>
      <Link className="btn-secondary" to="..">Retour AO</Link>
    </div>
    <div className="grid gap-5 xl:grid-cols-[360px_1fr]">
      <section className="panel">
        <h2 className="mb-4 text-lg font-semibold">Documents</h2>
        <div className="space-y-3">{tender.documents.map(document => {
          const latest = latestByDocument.get(document.id);
          return <div key={document.id} className="rounded-md border border-line p-3">
            <div className="font-medium">{document.originalFileName}</div>
            <div className="mt-1 flex items-center justify-between gap-2 text-xs text-muted"><span>v{document.currentVersion}</span><AnalysisBadge status={latest?.status ?? 'NONE'} /></div>
            <div className="mt-3 flex gap-2"><button className="btn-primary" onClick={() => analyze(document.id)}>Analyser</button>{latest && <button className="btn-secondary" onClick={() => setSelected(latest)}>Voir</button>}</div>
          </div>;
        })}</div>
        {tender.documents.length === 0 && <div className="text-sm text-muted">Aucun document PDF.</div>}
      </section>
      <section className="panel min-h-[520px]">
        {!selected && <div className="py-10 text-center text-sm text-muted">Selectionnez ou lancez une analyse.</div>}
        {selected && <div className="space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div><h2 className="text-lg font-semibold">{selected.documentName}</h2><div className="text-sm text-muted">Version {selected.versionNumber} - {selected.extractionMethod} - {selected.detectedLanguage} - {selected.textLength} caracteres</div></div>
            <AnalysisBadge status={selected.status} />
          </div>
          {selected.errorMessage && <div className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">{selected.errorMessage}</div>}
          {selected.requiresOcr && <div className="rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">Ce PDF semble scanne ou peu textuel. OCR requis si disponible.</div>}
          {selected.summary?.shortSummary && <Block title="Resume" value={selected.summary.shortSummary} />}
          <div>
            <h3 className="mb-3 font-semibold">Informations cles</h3>
            <div className="grid gap-3 lg:grid-cols-2">{selected.fields.map(field => <FieldCard key={field.id} field={field} onApply={applyField} />)}</div>
            {selected.fields.length === 0 && <div className="text-sm text-muted">Aucune information structuree detectee.</div>}
          </div>
          <div>
            <div className="mb-3 flex items-center justify-between"><h3 className="font-semibold">Checklist proposee</h3>{selected.requirements.length > 0 && <button className="btn-primary" onClick={() => applyChecklist(selected.requirements)}>Appliquer checklist</button>}</div>
            <div className="grid gap-2 md:grid-cols-2">{selected.requirements.map(r => <div key={r.id} className="rounded-md border border-line p-3"><div className="font-medium">{r.label}</div><div className="text-xs text-muted">{r.category} - Page {r.sourcePage ?? '-'}</div></div>)}</div>
            {selected.requirements.length === 0 && <div className="text-sm text-muted">Aucune piece demandee detectee.</div>}
          </div>
          <div>
            <h3 className="mb-3 font-semibold">Dates importantes</h3>
            <div className="space-y-2">{selected.importantDates.map(d => <div key={d.id} className="rounded-md border border-line p-3"><div className="font-medium">{formatDate(d.date)} - {d.label}</div><div className="text-xs text-muted">{d.type} - Page {d.sourcePage ?? '-'}</div></div>)}</div>
          </div>
          <div>
            <h3 className="mb-3 font-semibold">Rechercher dans le document</h3>
            <div className="flex gap-2"><input className="input" placeholder="caution, delai, attestation..." value={search} onChange={e => setSearch(e.target.value)} /><button className="btn-secondary" onClick={searchDocument}>Rechercher</button></div>
            <div className="mt-3 space-y-2">{hits.map((hit, index) => <div key={index} className="rounded-md border border-line p-3 text-sm"><div className="font-medium">Page {hit.page}</div><div className="text-slate-700">{hit.snippet}</div></div>)}</div>
          </div>
        </div>}
      </section>
    </div>
  </div>;
}

function FieldCard({ field, onApply }: { field: ExtractedField; onApply: (field: ExtractedField, value?: string) => void }) {
  const [value, setValue] = useState(field.value ?? '');
  const confidence = Number(field.confidence ?? 0);
  const confidenceText = confidence >= 0.85 ? 'elevee' : confidence >= 0.65 ? 'moyenne' : 'a verifier';
  return <div className={`rounded-md border p-3 ${field.conflict ? 'border-amber-200 bg-amber-50' : 'border-line'}`}>
    <div className="text-sm font-semibold">{extractionFieldLabels[field.field] ?? field.field}</div>
    {field.conflict && <div className="mt-1 text-xs text-amber-800">Conflit detecte. Actuel: {field.currentValue ?? '-'}</div>}
    <input className="input mt-2" value={value} onChange={e => setValue(e.target.value)} />
    <div className="mt-2 text-xs text-muted">Confiance d'extraction: {confidenceText} ({Math.round(confidence * 100)} %) - Page {field.sourcePage ?? '-'}</div>
    {field.sourceExcerpt && <div className="mt-2 rounded bg-slate-50 p-2 text-xs text-slate-700">{field.sourceExcerpt}</div>}
    <button className="btn-primary mt-3" disabled={field.validated} onClick={() => onApply(field, value)}>{field.validated ? 'Confirme' : 'Accepter'}</button>
  </div>;
}

function Block({ title, value }: { title: string; value?: string }) {
  if (!value) return null;
  return <div><h3 className="mb-2 font-semibold">{title}</h3><div className="whitespace-pre-line rounded-md border border-line bg-slate-50 p-3 text-sm text-slate-700">{value}</div></div>;
}
