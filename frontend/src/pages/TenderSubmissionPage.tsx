import { CheckCircle2, Download } from 'lucide-react';
import { FormEvent, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api, errorMessage } from '../api/client';
import { SubmissionPlatform, SubmissionStatus, TenderSubmission } from '../api/types';
import { Toast } from '../ui/Toast';
import { formatDate, fullName, submissionStatusLabels } from '../utils/labels';

const readinessLabels: Record<string, string> = { NOT_READY: 'Dossier incomplet', ATTENTION: 'A verifier', READY: 'Pret a deposer' };

const statuses: SubmissionStatus[] = ['NOT_READY', 'READY', 'IN_PROGRESS', 'SUBMITTED', 'CONFIRMED', 'FAILED'];

export function TenderSubmissionPage() {
  const { id } = useParams();
  const [submission, setSubmission] = useState<TenderSubmission | null>(null);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [confirmForm, setConfirmForm] = useState({ platform: 'PMMP' as SubmissionPlatform, submissionDate: new Date().toISOString().slice(0, 10), submissionTime: '', externalReference: '', notes: '' });
  const [receipt, setReceipt] = useState<File | null>(null);
  const [saving, setSaving] = useState(false);
  const load = () => api.get<TenderSubmission>(`/tenders/${id}/submission`).then(r => setSubmission(r.data)).catch(e => setToast({ type: 'error', message: errorMessage(e) }));
  useEffect(() => { load(); }, [id]);

  async function updateStatus(status: SubmissionStatus) {
    try {
      await api.patch(`/tenders/${id}/submission/status`, { status });
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }

  async function confirmSubmission(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    try {
      const data = new FormData();
      const payload = { ...confirmForm, submissionTime: confirmForm.submissionTime ? `${confirmForm.submissionTime}:00` : null, status: 'SUBMITTED' };
      data.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
      if (receipt) data.append('receipt', receipt);
      await api.post(`/tenders/${id}/submission/confirm`, data);
      setReceipt(null);
      setToast({ type: 'success', message: 'Depot confirme' });
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
    finally { setSaving(false); }
  }

  async function downloadReceipt() {
    try {
      const response = await api.get(`/tenders/${id}/submission/receipt`, { responseType: 'blob' });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = `recu-depot-${id}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }

  if (!submission) return <div className="panel">Chargement...</div>;
  return (
    <div className="space-y-5">
      {toast && <Toast {...toast} />}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div><h1 className="text-2xl font-semibold">Depot AO</h1><p className="text-sm text-muted">{submission.submissionManager ? fullName(submission.submissionManager) : 'Responsable depot non affecte'}</p></div>
        <select className="input max-w-xs" value={submission.status} onChange={e => updateStatus(e.target.value as SubmissionStatus)}>
          {statuses.map(status => <option key={status} value={status}>{submissionStatusLabels[status]}</option>)}
        </select>
      </div>
      <section className="grid gap-4 md:grid-cols-3">
        <div className="panel"><div className="text-sm text-muted">Etat depot</div><div className="text-xl font-semibold">{submissionStatusLabels[submission.status]}</div></div>
        <div className="panel"><div className="text-sm text-muted">Plateforme</div><div className="text-xl font-semibold">{submission.platform}</div></div>
        <div className="panel"><div className="text-sm text-muted">Preparation</div><div className="text-xl font-semibold">{readinessLabels[submission.readiness] ?? submission.readiness}</div></div>
      </section>
      <section className="panel">
        <h2 className="mb-4 text-lg font-semibold">Informations depot</h2>
        <dl className="grid gap-4 md:grid-cols-2">
          <Info label="Date depot" value={formatDate(submission.submissionDate)} />
          <Info label="Heure depot" value={submission.submissionTime ?? '-'} />
          <Info label="Reference externe" value={submission.externalReference ?? '-'} />
          <Info label="Depose par" value={submission.submittedBy ? fullName(submission.submittedBy) : '-'} />
          <Info label="Chiffrage valide" value={submission.pricingValidated ? 'Oui' : 'Non'} />
          <Info label="Checklist complete" value={submission.checklistComplete ? 'Oui' : 'Non'} />
        </dl>
        {submission.notes && <p className="mt-5 whitespace-pre-line text-sm text-slate-700">{submission.notes}</p>}
        {submission.receiptFilePath && <button className="btn-secondary mt-5" onClick={downloadReceipt}><Download size={16} /> Telecharger le recu</button>}
      </section>
      {submission.status !== 'SUBMITTED' && submission.status !== 'CONFIRMED' && (
        <form className="panel space-y-4" onSubmit={confirmSubmission}>
          <div>
            <h2 className="text-lg font-semibold">Confirmer le depot</h2>
            {submission.readiness !== 'READY' && <p className="mt-1 text-sm text-amber-700">Attention : le dossier n'est pas complet ({submission.pricingValidated ? '' : 'chiffrage non valide, '}{submission.checklistComplete ? '' : 'checklist incomplete'}).</p>}
          </div>
          <div className="grid gap-3 md:grid-cols-4">
            <label className="block"><span className="label">Plateforme</span><select className="input mt-1" value={confirmForm.platform} onChange={e => setConfirmForm({ ...confirmForm, platform: e.target.value as SubmissionPlatform })}><option value="PMMP">PMMP (marchespublics.gov.ma)</option><option value="OTHER">Autre / depot papier</option></select></label>
            <label className="block"><span className="label">Date de depot</span><input className="input mt-1" type="date" required value={confirmForm.submissionDate} onChange={e => setConfirmForm({ ...confirmForm, submissionDate: e.target.value })} /></label>
            <label className="block"><span className="label">Heure</span><input className="input mt-1" type="time" value={confirmForm.submissionTime} onChange={e => setConfirmForm({ ...confirmForm, submissionTime: e.target.value })} /></label>
            <label className="block"><span className="label">Reference / N d'accuse</span><input className="input mt-1" value={confirmForm.externalReference} onChange={e => setConfirmForm({ ...confirmForm, externalReference: e.target.value })} /></label>
            <label className="block md:col-span-3"><span className="label">Notes</span><input className="input mt-1" value={confirmForm.notes} onChange={e => setConfirmForm({ ...confirmForm, notes: e.target.value })} /></label>
            <label className="block"><span className="label">Accuse de reception (PDF)</span><input className="input mt-1" type="file" accept=".pdf,application/pdf" onChange={e => setReceipt(e.target.files?.[0] ?? null)} /></label>
          </div>
          <button className="btn-primary" disabled={saving}><CheckCircle2 size={16} /> {saving ? 'Enregistrement...' : 'Confirmer le depot'}</button>
        </form>
      )}
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return <div><dt className="text-xs uppercase text-muted">{label}</dt><dd className="mt-1 font-medium">{value}</dd></div>;
}
