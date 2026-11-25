import { DeadlineLevel, Priority, TaskPriority, TaskStatus, TenderResult, TenderStatus } from '../api/types';
import { deadlineLabels, priorityLabels, resultLabels, statusLabels, taskPriorityLabels, taskStatusLabels } from '../utils/labels';

const tone: Record<string, string> = {
  NEW: 'bg-sky-50 text-sky-700 border-sky-200',
  IN_PROGRESS: 'bg-amber-50 text-amber-700 border-amber-200',
  SUBMITTED: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  WON: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  LOST: 'bg-red-50 text-red-700 border-red-200',
  URGENT: 'bg-red-50 text-red-700 border-red-200',
  OVERDUE: 'bg-red-50 text-red-800 border-red-200',
  TODAY: 'bg-rose-50 text-rose-700 border-rose-200',
  WARNING: 'bg-amber-50 text-amber-700 border-amber-200',
  HIGH: 'bg-orange-50 text-orange-700 border-orange-200'
};

export function StatusBadge({ value }: { value: TenderStatus }) {
  return <span className={`rounded-full border px-2 py-1 text-xs font-medium ${tone[value] ?? 'border-slate-200 bg-slate-50 text-slate-700'}`}>{statusLabels[value]}</span>;
}

export function ResultBadge({ value }: { value: TenderResult }) {
  return <span className={`rounded-full border px-2 py-1 text-xs font-medium ${tone[value] ?? 'border-slate-200 bg-slate-50 text-slate-700'}`}>{resultLabels[value]}</span>;
}

export function PriorityBadge({ value }: { value: Priority }) {
  return <span className={`rounded-full border px-2 py-1 text-xs font-medium ${tone[value] ?? 'border-slate-200 bg-slate-50 text-slate-700'}`}>{priorityLabels[value]}</span>;
}

export function DeadlineBadge({ value, text }: { value: DeadlineLevel; text: string }) {
  return <span className={`rounded-full border px-2 py-1 text-xs font-medium ${tone[value] ?? 'border-slate-200 bg-slate-50 text-slate-700'}`}>{text || deadlineLabels[value]}</span>;
}

export function TaskStatusBadge({ value }: { value: TaskStatus }) {
  return <span className={`rounded-full border px-2 py-1 text-xs font-medium ${tone[value] ?? 'border-slate-200 bg-slate-50 text-slate-700'}`}>{taskStatusLabels[value]}</span>;
}

export function TaskPriorityBadge({ value }: { value: TaskPriority }) {
  return <span className={`rounded-full border px-2 py-1 text-xs font-medium ${tone[value] ?? 'border-slate-200 bg-slate-50 text-slate-700'}`}>{taskPriorityLabels[value]}</span>;
}

export function ChecklistBadge({ complete }: { complete: boolean }) {
  return <span className={`rounded-full border px-2 py-1 text-xs font-medium ${complete ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-amber-200 bg-amber-50 text-amber-700'}`}>{complete ? 'Complet' : 'Incomplet'}</span>;
}

export function AnalysisBadge({ status }: { status: string }) {
  const label = status === 'PROCESSING' ? 'Analyse en cours' : status === 'COMPLETED' ? 'Analyse' : status === 'FAILED' ? 'Erreur' : status === 'REQUIRES_REVIEW' ? 'A verifier' : 'Non analyse';
  return <span className={`rounded-full border px-2 py-1 text-xs font-medium ${tone[status] ?? (status === 'COMPLETED' ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : status === 'FAILED' ? 'border-red-200 bg-red-50 text-red-700' : 'border-slate-200 bg-slate-50 text-slate-700')}`}>{label}</span>;
}
