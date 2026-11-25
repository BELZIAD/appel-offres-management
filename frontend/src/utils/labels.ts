import { DeadlineLevel, PricingStatus, Priority, SubmissionStatus, TaskPriority, TaskStatus, TenderAssignmentRole, TenderResult, TenderStatus } from '../api/types';

export const statusLabels: Record<TenderStatus, string> = {
  NEW: 'Nouveau',
  TO_STUDY: 'A etudier',
  IN_PROGRESS: 'En cours',
  PREPARING: 'Preparation',
  READY_TO_SUBMIT: 'Pret a deposer',
  SUBMITTED: 'Depose',
  CLOSED: 'Cloture'
};

export const resultLabels: Record<TenderResult, string> = {
  PENDING: 'En attente',
  WON: 'Gagne',
  LOST: 'Perdu',
  CANCELLED: 'Annule'
};

export const priorityLabels: Record<Priority, string> = {
  LOW: 'Faible',
  NORMAL: 'Normale',
  HIGH: 'Haute',
  URGENT: 'Urgente'
};

export const taskStatusLabels: Record<TaskStatus, string> = {
  TODO: 'A faire',
  IN_PROGRESS: 'En cours',
  BLOCKED: 'Bloquee',
  DONE: 'Terminee'
};

export const taskPriorityLabels: Record<TaskPriority, string> = {
  LOW: 'Faible',
  NORMAL: 'Normale',
  HIGH: 'Haute',
  URGENT: 'Urgente'
};

export const assignmentRoleLabels: Record<TenderAssignmentRole, string> = {
  TENDER_MANAGER: 'Responsable AO',
  PRICING_MANAGER: 'Responsable chiffrage',
  SUBMISSION_MANAGER: 'Responsable depot',
  CONTRIBUTOR: 'Collaborateur'
};

export const pricingStatusLabels: Record<PricingStatus, string> = {
  NOT_STARTED: 'Non commence',
  IN_PROGRESS: 'En cours',
  READY_FOR_REVIEW: 'A verifier',
  VALIDATED: 'Valide',
  REVISION_REQUIRED: 'A corriger'
};

export const submissionStatusLabels: Record<SubmissionStatus, string> = {
  NOT_READY: 'Pas pret',
  READY: 'Pret a deposer',
  IN_PROGRESS: 'Depot en cours',
  SUBMITTED: 'Depose',
  CONFIRMED: 'Confirme',
  FAILED: 'Echec'
};

export const analysisStatusLabels: Record<string, string> = {
  PENDING: 'En attente',
  PROCESSING: 'Analyse en cours',
  COMPLETED: 'Analyse',
  FAILED: 'Erreur',
  REQUIRES_REVIEW: 'A verifier'
};

export const extractionFieldLabels: Record<string, string> = {
  submissionDeadline: 'Date limite detectee',
  publicationDate: 'Date publication detectee',
  openingDate: 'Ouverture des plis',
  estimatedAmount: 'Montant detecte',
  provisionalGuarantee: 'Caution detectee',
  organization: 'Organisme detecte',
  title: 'Objet detecte',
  marketNumber: 'Numero marche',
  reference: 'Reference detectee',
  executionPeriod: 'Delai execution'
};

export function formatDate(value?: string) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('fr-FR').format(new Date(`${value}T00:00:00`));
}

export function formatMoney(value?: number) {
  if (value === undefined || value === null) return '-';
  return `${new Intl.NumberFormat('fr-MA', { maximumFractionDigits: 0 }).format(value)} DH`;
}

export function fullName(user?: { firstName: string; lastName: string }) {
  return user ? `${user.firstName} ${user.lastName}` : 'Non affecte';
}

export function deadlineText(days: number | null) {
  if (days === null || days === undefined) return '-';
  if (days < 0) return `Depasse de ${Math.abs(days)} jour${Math.abs(days) > 1 ? 's' : ''}`;
  if (days === 0) return "Aujourd'hui";
  return `${days} jour${days > 1 ? 's' : ''}`;
}

export const deadlineLabels: Record<DeadlineLevel, string> = {
  NORMAL: 'Normal',
  WARNING: 'Cette semaine',
  URGENT: 'Urgent',
  TODAY: "Aujourd'hui",
  OVERDUE: 'En retard'
};

export function formatFileSize(size: number) {
  if (size < 1024) return `${size} o`;
  if (size < 1024 * 1024) return `${Math.round(size / 1024)} Ko`;
  return `${(size / 1024 / 1024).toFixed(1)} Mo`;
}
