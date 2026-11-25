import { CalendarPlus, Copy, Download, Edit, Save, Star, Trash2, Upload, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { api, errorMessage } from '../api/client';
import { ChecklistItem, DocumentVersion, HistoryItem, Page, Tender, TenderComment, TenderResult, TenderStatus, TenderTask } from '../api/types';
import { useAuth } from '../state/AuthContext';
import { ChecklistBadge, DeadlineBadge, PriorityBadge, ResultBadge, StatusBadge, TaskPriorityBadge, TaskStatusBadge } from '../ui/Badge';
import { Toast } from '../ui/Toast';
import { deadlineText, formatDate, formatFileSize, formatMoney, fullName, resultLabels, statusLabels } from '../utils/labels';

export function TenderDetailPage() {
  const { id } = useParams();
  const location = useLocation();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [tender, setTender] = useState<Tender | null>(null);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [checklist, setChecklist] = useState<ChecklistItem[]>([]);
  const [tasks, setTasks] = useState<TenderTask[]>([]);
  const [comments, setComments] = useState<TenderComment[]>([]);
  const [versions, setVersions] = useState<Record<number, DocumentVersion[]>>({});
  const [tab, setTab] = useState<'info' | 'checklist' | 'tasks' | 'documents' | 'comments' | 'history'>(() => tabFromPath(location.pathname));
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [wonAmount, setWonAmount] = useState('');
  const [lossReason, setLossReason] = useState('');
  const [comment, setComment] = useState('');
  const [editingComment, setEditingComment] = useState<{ id: number; content: string } | null>(null);
  const [uploadingVersion, setUploadingVersion] = useState<number | null>(null);
  const load = () => {
    api.get<Tender>(`/tenders/${id}`).then(r => setTender(r.data)).catch(e => setToast({ type: 'error', message: errorMessage(e) }));
    api.get<Page<HistoryItem>>(`/tenders/${id}/history?size=50`).then(r => setHistory(r.data.content)).catch(() => undefined);
    api.get<ChecklistItem[]>(`/tenders/${id}/checklist`).then(r => setChecklist(r.data)).catch(() => undefined);
    api.get<Page<TenderTask>>(`${user?.role === 'ADMIN' ? '/admin' : '/employee'}/tasks?tenderId=${id}&size=50`).then(r => setTasks(r.data.content)).catch(() => undefined);
    api.get<TenderComment[]>(`/tenders/${id}/comments`).then(r => setComments(r.data)).catch(() => undefined);
  };
  useEffect(() => { load(); }, [id, user?.role]);
  useEffect(() => { setTab(tabFromPath(location.pathname)); }, [location.pathname]);

  async function removeDocument(documentId: number) {
    try { await api.delete(`/documents/${documentId}`); setToast({ type: 'success', message: 'Document supprime' }); load(); } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function downloadDocument(documentId: number, name: string) {
    try {
      const response = await api.get(`/documents/${documentId}/download`, { responseType: 'blob' });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = name;
      link.click();
      URL.revokeObjectURL(url);
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function downloadVersion(versionId: number, name: string) {
    try {
      const response = await api.get(`/document-versions/${versionId}/download`, { responseType: 'blob' });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = name;
      link.click();
      URL.revokeObjectURL(url);
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function changeStatus(status: TenderStatus) {
    try {
      const url = user?.role === 'ADMIN' ? `/admin/tenders/${id}/status` : `/employee/tenders/${id}/status`;
      await api.patch(url, { status });
      setToast({ type: 'success', message: 'Statut mis a jour' });
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function changeResult(result: TenderResult) {
    try {
      await api.patch(`/admin/tenders/${id}/result`, { result });
      setToast({ type: 'success', message: 'Resultat mis a jour' });
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function declareWon() {
    try {
      await api.patch(`/admin/tenders/${id}/result`, { result: 'WON', finalAmount: wonAmount ? Number(wonAmount) : null });
      setToast({ type: 'success', message: 'Marche declare gagne' });
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function declareLost() {
    try {
      await api.patch(`/admin/tenders/${id}/result`, { result: 'LOST', lossReason: lossReason || null });
      setToast({ type: 'success', message: 'Marche declare perdu' });
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function toggleChecklist(item: ChecklistItem) {
    try {
      await api.patch(`/tenders/${id}/checklist/${item.id}`, { completed: !item.completed });
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function createChecklistFromDefault() {
    try {
      const templates = await api.get('/admin/checklist-templates');
      const templateId = templates.data[0]?.id;
      if (!templateId) return;
      await api.post(`/admin/tenders/${id}/checklist/from-template/${templateId}`);
      setToast({ type: 'success', message: 'Checklist creee' });
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function addComment() {
    if (!comment.trim()) return;
    try {
      await api.post(`/tenders/${id}/comments`, { content: comment });
      setComment('');
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function updateComment() {
    if (!editingComment || !editingComment.content.trim()) return;
    try {
      await api.put(`/tenders/${id}/comments/${editingComment.id}`, { content: editingComment.content });
      setEditingComment(null);
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function deleteComment(commentId: number) {
    if (!window.confirm('Supprimer ce commentaire ?')) return;
    try {
      await api.delete(`/tenders/${id}/comments/${commentId}`);
      if (editingComment?.id === commentId) setEditingComment(null);
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function duplicateTender() {
    if (!window.confirm('Dupliquer cet appel d\'offres (informations, montants et checklist) ?')) return;
    try {
      const response = await api.post<Tender>(`/admin/tenders/${id}/duplicate`);
      navigate(`/admin/tenders/${response.data.id}/edit`);
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }

  function exportCalendar() {
    if (!tender) return;
    const ics = buildIcs(tender);
    const url = URL.createObjectURL(new Blob([ics], { type: 'text/calendar;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `echeance-${tender.reference.replace(/[^A-Za-z0-9-_]/g, '_')}.ics`;
    link.click();
    URL.revokeObjectURL(url);
  }

  async function toggleFavorite() {
    try {
      if (tender?.favorite) await api.delete(`/tenders/${id}/favorite`);
      else await api.post(`/tenders/${id}/favorite`);
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }
  async function loadVersions(documentId: number) {
    const response = await api.get<DocumentVersion[]>(`/tenders/${id}/documents/${documentId}/versions`);
    setVersions(v => ({ ...v, [documentId]: response.data }));
  }
  async function addVersion(documentId: number, file?: File) {
    if (!file) return;
    setUploadingVersion(documentId);
    try {
      const data = new FormData();
      data.append('file', file);
      await api.post(`/tenders/${id}/documents/${documentId}/versions`, data);
      await loadVersions(documentId);
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); } finally { setUploadingVersion(null); }
  }
  async function deleteVersion(documentId: number, versionId: number) {
    if (!window.confirm('Supprimer cette version ?')) return;
    try {
      await api.delete(`/document-versions/${versionId}`);
      await loadVersions(documentId);
      load();
    } catch (e) { setToast({ type: 'error', message: errorMessage(e) }); }
  }

  if (!tender) return <div>Chargement...</div>;
  const tenderBase = `${user?.role === 'ADMIN' ? '/admin' : '/employee'}/tenders/${id}`;
  return (
    <div className="space-y-5">
      {toast && <Toast {...toast} />}
      <div className="flex items-start justify-between gap-3"><div><h1 className="text-2xl font-semibold">{tender.reference} - {tender.title}</h1><p className="text-sm text-muted">{tender.organization}</p></div><div className="flex flex-wrap gap-2"><Link className="btn-secondary" to={`${tenderBase}/analysis`}>Analyse</Link><Link className="btn-secondary" to={`${tenderBase}/pricing`}>Chiffrage</Link><Link className="btn-secondary" to={`${tenderBase}/team`}>Equipe</Link><Link className="btn-secondary" to={`${tenderBase}/submission`}>Depot</Link><button className="btn-secondary" onClick={exportCalendar} title="Ajouter la date limite a Outlook / Google Agenda"><CalendarPlus size={16} /> Agenda</button>{user?.role === 'ADMIN' && <button className="btn-secondary" onClick={duplicateTender}><Copy size={16} /> Dupliquer</button>}<button className="btn-secondary" onClick={toggleFavorite}><Star size={16} className={tender.favorite ? 'fill-amber-400 text-amber-500' : ''} /> {tender.favorite ? 'Favori' : 'Ajouter favori'}</button></div></div>
      <div className="flex flex-wrap gap-2 border-b border-line">
        <button className={`px-3 py-2 text-sm font-medium ${tab === 'info' ? 'border-b-2 border-ink' : 'text-muted'}`} onClick={() => setTab('info')}>Informations</button>
        <button className={`px-3 py-2 text-sm font-medium ${tab === 'checklist' ? 'border-b-2 border-ink' : 'text-muted'}`} onClick={() => setTab('checklist')}>Checklist</button>
        <button className={`px-3 py-2 text-sm font-medium ${tab === 'tasks' ? 'border-b-2 border-ink' : 'text-muted'}`} onClick={() => setTab('tasks')}>Taches</button>
        <button className={`px-3 py-2 text-sm font-medium ${tab === 'documents' ? 'border-b-2 border-ink' : 'text-muted'}`} onClick={() => setTab('documents')}>Documents</button>
        <button className={`px-3 py-2 text-sm font-medium ${tab === 'comments' ? 'border-b-2 border-ink' : 'text-muted'}`} onClick={() => setTab('comments')}>Commentaires</button>
        <button className={`px-3 py-2 text-sm font-medium ${tab === 'history' ? 'border-b-2 border-ink' : 'text-muted'}`} onClick={() => setTab('history')}>Historique</button>
      </div>

      {tab === 'info' && (
        <div className="grid gap-5 lg:grid-cols-3">
          <section className="panel lg:col-span-2"><h2 className="mb-4 text-lg font-semibold">Informations</h2><dl className="grid gap-4 sm:grid-cols-2"><Info label="Numero marche" value={tender.marketNumber ?? '-'} /><Info label="Organisme" value={tender.organization} /><Info label="Publication" value={formatDate(tender.publicationDate)} /><Info label="Date limite" value={formatDate(tender.deadline)} /><Info label="Montant estime" value={formatMoney(tender.estimatedAmount)} /><Info label="Caution provisoire" value={formatMoney(tender.provisionalGuarantee)} /></dl><p className="mt-5 whitespace-pre-line text-sm text-slate-700">{tender.description || 'Aucune description.'}</p></section>
          <section className="panel"><h2 className="mb-4 text-lg font-semibold">Avancement</h2><div className="space-y-4"><div><div className="label mb-1">Responsable</div>{fullName(tender.assignedEmployee)}</div><div><div className="label mb-1">Echeance</div><DeadlineBadge value={tender.deadlineLevel} text={deadlineText(tender.daysRemaining)} /></div><div><div className="label mb-1">Statut</div><StatusBadge value={tender.status} /><select className="input mt-2" value={tender.status} onChange={e => changeStatus(e.target.value as TenderStatus)}>{Object.entries(statusLabels).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></div><div><div className="label mb-1">Resultat</div><ResultBadge value={tender.result} />{user?.role === 'ADMIN' && <button className="btn-secondary mt-2" onClick={() => changeResult('PENDING')}>Remettre en attente</button>}</div><div><div className="label mb-1">Priorite</div><PriorityBadge value={tender.priority} /></div></div></section>
          <section className="panel lg:col-span-3"><div className="mb-2 flex items-center justify-between"><h2 className="text-lg font-semibold">Preparation du dossier</h2><ChecklistBadge complete={tender.checklistComplete} /></div><div className="h-2 rounded-full bg-slate-100"><div className="h-2 rounded-full bg-emerald-500" style={{ width: `${tender.progressPercentage}%` }} /></div><div className="mt-2 text-sm text-muted">{tender.checklistCompleted}/{tender.checklistTotal} elements termines - {tender.missingRequiredItems} requis manquants</div></section>
          <section className="panel lg:col-span-3"><h2 className="mb-4 text-lg font-semibold">Resume financier</h2><div className="grid gap-4 md:grid-cols-4"><Info label="Montant estime" value={formatMoney(tender.estimatedAmount)} /><Info label="Montant remporte" value={formatMoney(tender.finalAmount)} /><Info label="Date resultat" value={formatDate(tender.resultDate)} /><Info label="Motif perte" value={tender.lossReason ?? '-'} /></div>{user?.role === 'ADMIN' && <div className="mt-5 grid gap-3 md:grid-cols-2"><div className="rounded-md border border-line p-3"><div className="font-medium">Declarer gagne</div><p className="mb-3 text-xs text-muted">Cette action sera enregistree dans l'historique.</p><input className="input" type="number" placeholder="Montant remporte optionnel" value={wonAmount} onChange={e => setWonAmount(e.target.value)} /><button className="btn-primary mt-3" onClick={declareWon}>Declarer gagne</button></div><div className="rounded-md border border-line p-3"><div className="font-medium">Declarer perdu</div><p className="mb-3 text-xs text-muted">Cette action sera enregistree dans l'historique.</p><textarea className="input" placeholder="Motif de perte optionnel" value={lossReason} onChange={e => setLossReason(e.target.value)} /><button className="btn-secondary mt-3" onClick={declareLost}>Declarer perdu</button></div></div>}</section>
        </div>
      )}

      {tab === 'checklist' && (
        <section className="panel">
          <div className="mb-4 flex items-center justify-between"><h2 className="text-lg font-semibold">Checklist</h2>{user?.role === 'ADMIN' && checklist.length === 0 && <button className="btn-primary" onClick={createChecklistFromDefault}>Creer checklist standard</button>}</div>
          <div className="mb-4"><div className="h-2 rounded-full bg-slate-100"><div className="h-2 rounded-full bg-emerald-500" style={{ width: `${tender.progressPercentage}%` }} /></div></div>
          <div className="space-y-2">{checklist.map(item => <label key={item.id} className="flex items-center justify-between gap-3 rounded-md border border-line p-3"><span className="flex items-center gap-3"><input type="checkbox" checked={item.completed} onChange={() => toggleChecklist(item)} /><span className="font-medium">{item.label}</span>{item.required && <span className="text-xs text-red-700">Requis</span>}{item.documentDetected && <span className="text-xs text-emerald-700">Document detecte</span>}</span><span className="text-xs text-muted">{item.expectedDocumentType ?? ''}</span></label>)}</div>
          {checklist.length === 0 && <div className="py-8 text-center text-sm text-muted">Aucune checklist.</div>}
        </section>
      )}

      {tab === 'tasks' && (
        <section className="panel overflow-x-auto">
          <h2 className="mb-4 text-lg font-semibold">Taches liees</h2>
          <table className="w-full text-left text-sm"><thead><tr className="border-b border-line text-xs uppercase text-muted"><th className="py-2">Titre</th><th>Responsable</th><th>Priorite</th><th>Statut</th><th>Deadline</th></tr></thead><tbody>{tasks.map(task => <tr key={task.id} className="border-b border-line last:border-0"><td className="py-3 font-medium">{task.title}</td><td>{fullName(task.assignedEmployee)}</td><td><TaskPriorityBadge value={task.priority} /></td><td><TaskStatusBadge value={task.status} /></td><td>{formatDate(task.deadline)}</td></tr>)}</tbody></table>
          {tasks.length === 0 && <div className="py-8 text-center text-sm text-muted">Aucune tache.</div>}
        </section>
      )}

      {tab === 'documents' && (
        <section className="panel">
          <h2 className="mb-4 text-lg font-semibold">Documents</h2>
          <div className="space-y-2">{tender.documents.map(d => <div key={d.id} className="rounded-md border border-line p-3"><div className="flex flex-wrap items-center justify-between gap-3"><div><div className="font-medium">{d.originalFileName}</div><div className="text-xs text-muted">{d.documentType} - v{d.currentVersion} - {formatFileSize(d.fileSize)} - {d.uploadedBy ? fullName(d.uploadedBy) : 'Systeme'} - {formatDate(d.uploadedAt.slice(0, 10))}</div></div><div className="flex gap-2"><button className="btn-secondary" onClick={() => downloadDocument(d.id, d.originalFileName)}><Download size={16} /> Telecharger</button><button className="btn-secondary" onClick={() => loadVersions(d.id)}>Historique versions</button><label className="btn-secondary cursor-pointer"><Upload size={16} /> {uploadingVersion === d.id ? 'Upload...' : 'Ajouter version'}<input className="hidden" type="file" accept="application/pdf" onChange={e => addVersion(d.id, e.target.files?.[0])} /></label>{user?.role === 'ADMIN' && <button className="btn-secondary text-red-700" onClick={() => removeDocument(d.id)}><Trash2 size={16} /></button>}</div></div>{versions[d.id] && <div className="mt-3 space-y-2 border-t border-line pt-3">{versions[d.id].map(v => <div key={v.id} className="flex items-center justify-between gap-3 text-sm"><span>Version {v.versionNumber} - {new Date(v.uploadedAt).toLocaleDateString('fr-FR')} - {v.uploadedBy ? fullName(v.uploadedBy) : 'Systeme'}</span><div className="flex gap-2"><button className="btn-secondary" onClick={() => downloadVersion(v.id, v.originalFileName)}>Telecharger</button>{user?.role === 'ADMIN' && <button className="btn-secondary text-red-700" onClick={() => deleteVersion(d.id, v.id)}><Trash2 size={16} /></button>}</div></div>)}</div>}</div>)}</div>
          {tender.documents.length === 0 && <div className="py-6 text-center text-sm text-muted">Aucun document PDF depose.</div>}
        </section>
      )}

      {tab === 'comments' && (
        <section className="panel">
          <h2 className="mb-4 text-lg font-semibold">Commentaires</h2>
          <div className="space-y-4">{comments.map(c => <div key={c.id} className="flex gap-3"><div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-slate-900 text-xs font-semibold text-white">{c.author ? `${c.author.firstName[0]}${c.author.lastName[0]}` : 'SY'}</div><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center justify-between gap-2"><div className="text-sm font-medium">{c.author ? fullName(c.author) : 'Systeme'} <span className="font-normal text-muted">{new Date(c.createdAt).toLocaleString('fr-FR')}</span></div>{user?.role === 'ADMIN' && !c.deleted && <div className="flex gap-2"><button className="btn-secondary" onClick={() => setEditingComment({ id: c.id, content: c.content })}><Edit size={16} /></button><button className="btn-secondary text-red-700" onClick={() => deleteComment(c.id)}><Trash2 size={16} /></button></div>}</div>{editingComment?.id === c.id ? <div className="mt-2 space-y-2"><textarea className="input min-h-24" value={editingComment.content} onChange={e => setEditingComment({ ...editingComment, content: e.target.value })} /><div className="flex gap-2"><button className="btn-primary" onClick={updateComment}><Save size={16} /> Enregistrer</button><button className="btn-secondary" onClick={() => setEditingComment(null)}><X size={16} /> Annuler</button></div></div> : <div className="whitespace-pre-line text-sm text-slate-700">{c.deleted ? 'Commentaire supprime' : c.content}</div>}</div></div>)}</div>
          <div className="mt-5 flex gap-3"><textarea className="input min-h-24 flex-1" placeholder="Ajouter un commentaire..." value={comment} onChange={e => setComment(e.target.value)} /><button className="btn-primary self-end" onClick={addComment}>Envoyer</button></div>
        </section>
      )}

      {tab === 'history' && (
        <section className="panel">
          <h2 className="mb-4 text-lg font-semibold">Historique</h2>
          <div className="space-y-4">{history.map(h => <div key={h.id} className="border-l-2 border-slate-300 pl-4"><div className="text-xs text-muted">{new Date(h.createdAt).toLocaleString('fr-FR')}</div><div className="font-medium">{h.performedBy ? fullName(h.performedBy) : 'Systeme'}</div><div className="text-sm text-slate-700">{h.description}</div>{h.fieldName && <div className="mt-1 text-xs text-muted">{h.oldValue ?? '-'} {'->'} {h.newValue ?? '-'}</div>}</div>)}</div>
          {history.length === 0 && <div className="text-sm text-muted">Aucun historique.</div>}
        </section>
      )}
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return <div><dt className="text-xs uppercase text-muted">{label}</dt><dd className="mt-1 font-medium">{value}</dd></div>;
}

function tabFromPath(pathname: string): 'info' | 'checklist' | 'tasks' | 'documents' | 'comments' | 'history' {
  if (pathname.endsWith('/checklist')) return 'checklist';
  if (pathname.endsWith('/tasks')) return 'tasks';
  if (pathname.endsWith('/documents')) return 'documents';
  if (pathname.endsWith('/comments')) return 'comments';
  if (pathname.endsWith('/history')) return 'history';
  return 'info';
}

/** Fichier iCalendar (.ics) : evenement le jour de la date limite + rappels J-3 et J-1. */
function buildIcs(tender: Tender) {
  const esc = (v: string) => v.replace(/\\/g, '\\\\').replace(/;/g, '\\;').replace(/,/g, '\\,').replace(/\r?\n/g, '\\n');
  const day = tender.deadline.replace(/-/g, '');
  const next = new Date(`${tender.deadline}T00:00:00`);
  next.setDate(next.getDate() + 1);
  const end = `${next.getFullYear()}${String(next.getMonth() + 1).padStart(2, '0')}${String(next.getDate()).padStart(2, '0')}`;
  const stamp = new Date().toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '');
  return [
    'BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//Gestion AO//FR', 'CALSCALE:GREGORIAN', 'BEGIN:VEVENT',
    `UID:ao-${tender.id}-${day}@gestion-ao`, `DTSTAMP:${stamp}`, `DTSTART;VALUE=DATE:${day}`, `DTEND;VALUE=DATE:${end}`,
    `SUMMARY:${esc(`Date limite AO ${tender.reference} - ${tender.title}`)}`,
    `DESCRIPTION:${esc(`Organisme : ${tender.organization}${tender.description ? `\n${tender.description}` : ''}`)}`,
    'BEGIN:VALARM', 'ACTION:DISPLAY', 'DESCRIPTION:Echeance AO dans 3 jours', 'TRIGGER:-P3D', 'END:VALARM',
    'BEGIN:VALARM', 'ACTION:DISPLAY', 'DESCRIPTION:Echeance AO demain', 'TRIGGER:-P1D', 'END:VALARM',
    'END:VEVENT', 'END:VCALENDAR'
  ].join('\r\n');
}
