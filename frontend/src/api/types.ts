export type Role = 'ADMIN' | 'EMPLOYEE';
export type Priority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type TenderStatus = 'NEW' | 'TO_STUDY' | 'IN_PROGRESS' | 'PREPARING' | 'READY_TO_SUBMIT' | 'SUBMITTED' | 'CLOSED';
export type TenderResult = 'PENDING' | 'WON' | 'LOST' | 'CANCELLED';
export type DocumentType = 'RC' | 'CPS' | 'NOTICE' | 'PRICE_SCHEDULE' | 'TECHNICAL_DOCUMENT' | 'ADMINISTRATIVE_DOCUMENT' | 'FINANCIAL_DOCUMENT' | 'OTHER';
export type DeadlineLevel = 'NORMAL' | 'WARNING' | 'URGENT' | 'TODAY' | 'OVERDUE';
export type NotificationType = 'TENDER_ASSIGNED' | 'DEADLINE_WARNING' | 'TENDER_UPDATED' | 'DOCUMENT_ADDED' | 'STATUS_CHANGED' | 'RESULT_CHANGED' | 'TASK_ASSIGNED' | 'TASK_DEADLINE_WARNING' | 'TASK_COMPLETED' | 'TASK_BLOCKED' | 'TENDER_COMMENT_ADDED' | 'USER_MENTIONED' | 'DOCUMENT_ANALYSIS_COMPLETED' | 'DOCUMENT_ANALYSIS_FAILED';
export type TenderHistoryAction = 'CREATED' | 'UPDATED' | 'ASSIGNED' | 'REASSIGNED' | 'STATUS_CHANGED' | 'RESULT_CHANGED' | 'DOCUMENT_ADDED' | 'DOCUMENT_DELETED' | 'TASK_CREATED' | 'TASK_ASSIGNED' | 'TASK_REASSIGNED' | 'TASK_STATUS_CHANGED' | 'TASK_UPDATED' | 'TASK_DELETED' | 'DELETED';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'BLOCKED' | 'DONE';
export type TaskPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type TenderAssignmentRole = 'TENDER_MANAGER' | 'PRICING_MANAGER' | 'SUBMISSION_MANAGER' | 'CONTRIBUTOR';
export type PricingStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'READY_FOR_REVIEW' | 'VALIDATED' | 'REVISION_REQUIRED';
export type SubmissionStatus = 'NOT_READY' | 'READY' | 'IN_PROGRESS' | 'SUBMITTED' | 'CONFIRMED' | 'FAILED';
export type SubmissionPlatform = 'PMMP' | 'OTHER';
export type ParticipationType = 'SOLE_BIDDER' | 'CONSORTIUM' | 'JOINT_VENTURE' | 'SUBCONTRACTOR' | 'PARTNER';
export type ParticipationMode = 'SOLE' | 'WITH_PARTNERS';
export type SubmissionReadiness = 'NOT_READY' | 'ATTENTION' | 'READY';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  enabled: boolean;
}

export interface Employee extends User {
  assignedTenderCount: number;
}

export interface SimpleUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

export interface TenderDocument {
  id: number;
  tenderId: number;
  fileName: string;
  originalFileName: string;
  documentType: DocumentType;
  fileSize: number;
  uploadedBy?: SimpleUser;
  uploadedAt: string;
  currentVersion: number;
  latestVersionAt?: string;
}

export interface Tender {
  id: number;
  reference: string;
  marketNumber?: string;
  title: string;
  description?: string;
  organization: string;
  publicationDate?: string;
  deadline: string;
  estimatedAmount?: number;
  provisionalGuarantee?: number;
  finalAmount?: number;
  lossReason?: string;
  resultDate?: string;
  priority: Priority;
  status: TenderStatus;
  result: TenderResult;
  assignedEmployee?: SimpleUser;
  createdBy?: SimpleUser;
  createdAt: string;
  updatedAt: string;
  documents: TenderDocument[];
  daysRemaining: number | null;
  deadlineLevel: DeadlineLevel;
  checklistTotal: number;
  checklistCompleted: number;
  progressPercentage: number;
  missingRequiredItems: number;
  checklistComplete: boolean;
  favorite: boolean;
}

export interface TenderPayload {
  reference: string;
  marketNumber?: string;
  title: string;
  description?: string;
  organization: string;
  publicationDate?: string;
  deadline: string;
  estimatedAmount?: number | null;
  provisionalGuarantee?: number | null;
  priority?: Priority;
  status?: TenderStatus;
  result?: TenderResult;
  assignedEmployeeId?: number | null;
  finalAmount?: number | null;
  lossReason?: string;
}

export interface AdminStats {
  total: number;
  newCount: number;
  inProgress: number;
  submitted: number;
  won: number;
  lost: number;
  readyToSubmit: number;
  urgent: number;
  overdue: number;
  dueInSevenDays: number;
  recentTenders: Tender[];
  nextDeadlines: Tender[];
}

export interface EmployeeStats {
  myTenders: number;
  inProgress: number;
  urgent: number;
  readyToSubmit: number;
  submitted: number;
  won: number;
  lost: number;
  nextDeadlines: Tender[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface NotificationItem {
  id: number;
  tenderId?: number;
  tenderReference?: string;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  readAt?: string;
}

export interface HistoryItem {
  id: number;
  tenderId?: number;
  performedBy?: SimpleUser;
  action: TenderHistoryAction;
  fieldName?: string;
  oldValue?: string;
  newValue?: string;
  description: string;
  createdAt: string;
}

export interface StatisticsOverview {
  totalTenders: number;
  inProgress: number;
  submitted: number;
  won: number;
  lost: number;
  pending: number;
  cancelled: number;
  winRate: number;
  totalEstimatedAmount: number;
  totalWonAmount: number;
  averageWonAmount: number;
  urgent: number;
  overdue: number;
}

export interface MonthlyStats { month: number; total: number; won: number; lost: number; pending: number }
export interface ResultsStats { won: number; lost: number; pending: number; cancelled: number }
export interface EmployeePerformance {
  employeeId: number; employeeName: string; email: string; enabled: boolean; assigned: number; inProgress: number; submitted: number; won: number; lost: number; pending: number; winRate: number; wonAmount: number;
}
export interface EmployeePerformanceDetail { employee: EmployeePerformance; closed: number; tenders: Tender[]; monthly: MonthlyStats[] }
export interface OrganizationStats { organization: string; total: number; won: number; lost: number; pending: number; winRate: number; estimatedAmount: number; wonAmount: number }

export interface TenderTask {
  id: number;
  tender: Tender;
  title: string;
  description?: string;
  assignedEmployee?: SimpleUser;
  status: TaskStatus;
  priority: TaskPriority;
  deadline: string;
  daysRemaining: number | null;
  overdue: boolean;
  createdBy?: SimpleUser;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
}

export interface ChecklistItem {
  id: number;
  tenderId: number;
  label: string;
  required: boolean;
  completed: boolean;
  completedBy?: SimpleUser;
  completedAt?: string;
  position: number;
  expectedDocumentType?: DocumentType;
  documentDetected: boolean;
  createdAt: string;
}

export interface ChecklistTemplateItem {
  id?: number;
  label: string;
  position: number;
  required: boolean;
  expectedDocumentType?: DocumentType;
}

export interface ChecklistTemplate {
  id: number;
  name: string;
  description?: string;
  active: boolean;
  createdAt: string;
  items: ChecklistTemplateItem[];
}

export interface DocumentVersion {
  id: number;
  documentId: number;
  versionNumber: number;
  originalFileName: string;
  fileSize: number;
  uploadedBy?: SimpleUser;
  uploadedAt: string;
  comment?: string;
}

export interface TenderComment {
  id: number;
  tenderId: number;
  author?: SimpleUser;
  content: string;
  createdAt: string;
  updatedAt: string;
  deleted: boolean;
}

export interface ActivityItem {
  id: number;
  user?: SimpleUser;
  tenderId?: number;
  tenderReference?: string;
  entityType: string;
  entityId?: number;
  action: string;
  description: string;
  createdAt: string;
}

export interface SearchResults {
  tenders: Tender[];
  tasks: TenderTask[];
  documents: TenderDocument[];
  users: SimpleUser[];
}

export type AnalysisStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'REQUIRES_REVIEW';
export type ExtractionMethod = 'NATIVE_TEXT' | 'OCR' | 'FAILED';
export type RequirementStatus = 'DETECTED' | 'ACCEPTED' | 'REJECTED';
export type RequirementCategory = 'ADMINISTRATIVE' | 'TECHNICAL' | 'FINANCIAL' | 'LEGAL' | 'CERTIFICATE' | 'GUARANTEE' | 'OTHER';

export interface ExtractedField {
  id: number;
  field: string;
  value?: string;
  confidence: number;
  sourcePage?: number;
  sourceExcerpt?: string;
  validated: boolean;
  ignored: boolean;
  conflict: boolean;
  currentValue?: string;
}

export interface ExtractedRequirement {
  id: number;
  category: RequirementCategory;
  label: string;
  description?: string;
  required: boolean;
  sourcePage?: number;
  confidence: number;
  status: RequirementStatus;
}

export interface ExtractedImportantDate {
  id: number;
  type: string;
  date?: string;
  label: string;
  sourcePage?: number;
  confidence: number;
}

export interface DocumentAnalysisSummary {
  shortSummary?: string;
  keyPoints?: string;
  importantDates?: string;
  requiredDocuments?: string;
  financialConditions?: string;
  technicalConditions?: string;
  attentionPoints?: string;
}

export interface DocumentAnalysis {
  id: number;
  tenderId: number;
  documentId: number;
  documentVersionId: number;
  versionNumber: number;
  documentName: string;
  status: AnalysisStatus;
  extractionMethod: ExtractionMethod;
  detectedLanguage: string;
  pageCount: number;
  textLength: number;
  requiresOcr: boolean;
  analysisEngineVersion: string;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
  createdAt: string;
  fields: ExtractedField[];
  requirements: ExtractedRequirement[];
  importantDates: ExtractedImportantDate[];
  summary?: DocumentAnalysisSummary;
}

export interface DocumentSearchHit { page: number; snippet: string }

export interface TenderAssignment {
  id: number;
  employee: SimpleUser;
  assignmentRole: TenderAssignmentRole;
  assignedBy?: SimpleUser;
  assignedAt: string;
  active: boolean;
}

export interface TenderTeam { assignments: TenderAssignment[] }

export interface PricingLine {
  id: number;
  lineNumber?: string;
  reference?: string;
  designation: string;
  unit?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  notes?: string;
  position: number;
}

export interface TenderPricing {
  id: number;
  tenderId: number;
  status: PricingStatus;
  assignedPricingManager?: SimpleUser;
  estimatedTotal?: number;
  finalTotal?: number;
  currency: string;
  notes?: string;
  validatedAt?: string;
  validatedBy?: SimpleUser;
  lines: PricingLine[];
}

export interface TenderSubmission {
  id: number;
  tenderId: number;
  submissionManager?: SimpleUser;
  platform: SubmissionPlatform;
  status: SubmissionStatus;
  submissionDate?: string;
  submissionTime?: string;
  externalReference?: string;
  receiptFilePath?: string;
  notes?: string;
  submittedBy?: SimpleUser;
  readiness: SubmissionReadiness;
  pricingValidated: boolean;
  checklistComplete: boolean;
  submissionManagerAssigned: boolean;
}

export interface PartnerCompany {
  id: number;
  name: string;
  tradeName?: string;
  ice?: string;
  rc?: string;
  ifNumber?: string;
  address?: string;
  city?: string;
  country?: string;
  email?: string;
  phone?: string;
  contactPerson?: string;
  active: boolean;
  notes?: string;
  sharedTenderCount: number;
}

export interface CompanySettings {
  id: number;
  companyName?: string | null;
  applicationName: string;
  companyLogo?: string;
  companyLogoDark?: string;
  favicon?: string;
  primaryColor: string;
  secondaryColor?: string;
  accentColor?: string;
  dangerColor?: string;
  warningColor?: string;
  successColor?: string;
  sidebarColor?: string;
  headerColor?: string;
  address?: string;
  city?: string;
  postalCode?: string;
  country?: string;
  phone?: string;
  mobile?: string;
  email?: string;
  website?: string;
  ice?: string;
  ifNumber?: string;
  rc?: string;
  patente?: string;
  taxIdentifier?: string;
  currency?: string;
  timezone?: string;
  language?: string;
  linkedin?: string;
  facebook?: string;
  instagram?: string;
  youtube?: string;
  updatedAt?: string;
}

export interface UserPreferences {
  theme: 'LIGHT' | 'DARK' | 'SYSTEM' | string;
  language: string;
  emailNotifications: boolean;
  browserNotifications: boolean;
  timezone: string;
  dateFormat: string;
  timeFormat: string;
  pageSize: number;
}

export interface Profile {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  photo?: string;
  signature?: string;
  jobTitle?: string;
  department?: string;
  phone?: string;
  language?: string;
  timezone?: string;
  preferences: UserPreferences;
}

export interface AboutInfo {
  company: CompanySettings;
}

export interface BackupItem {
  name: string;
  size: number;
  createdAt: string;
  status: string;
  checksumSha256?: string;
}
