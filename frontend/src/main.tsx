import React from 'react';
import ReactDOM from 'react-dom/client';
import { Navigate, RouterProvider, createBrowserRouter } from 'react-router-dom';
import { AuthProvider } from './state/AuthContext';
import { SettingsProvider } from './state/SettingsContext';
import { ProtectedRoute } from './routes/ProtectedRoute';
import { AdminLayout } from './ui/AdminLayout';
import { EmployeeLayout } from './ui/EmployeeLayout';
import { LoginPage } from './pages/LoginPage';
import { AdminDashboard } from './pages/admin/AdminDashboard';
import { EmployeesPage } from './pages/admin/EmployeesPage';
import { EmployeeFormPage } from './pages/admin/EmployeeFormPage';
import { TendersPage } from './pages/admin/TendersPage';
import { TenderFormPage } from './pages/admin/TenderFormPage';
import { TenderDetailPage } from './pages/TenderDetailPage';
import { EmployeeDashboard } from './pages/employee/EmployeeDashboard';
import { EmployeeTendersPage } from './pages/employee/EmployeeTendersPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { DeadlinesPage } from './pages/admin/DeadlinesPage';
import { HistoryPage } from './pages/admin/HistoryPage';
import { EmployeePerformancePage } from './pages/admin/EmployeePerformancePage';
import { EmployeePerformanceDetailPage } from './pages/admin/EmployeePerformanceDetailPage';
import { OrganizationsAnalyticsPage } from './pages/admin/OrganizationsAnalyticsPage';
import { ReportsPage } from './pages/admin/ReportsPage';
import { ActivityPage } from './pages/admin/ActivityPage';
import { AdminTasksPage, EmployeeTasksPage } from './pages/TasksPage';
import { KanbanPage } from './pages/KanbanPage';
import { FavoritesPage } from './pages/FavoritesPage';
import { TenderAnalysisPage } from './pages/TenderAnalysisPage';
import { PricingPage } from './pages/PricingPage';
import { CompanySettingsPage } from './pages/admin/CompanySettingsPage';
import { BackupsPage } from './pages/admin/BackupsPage';
import { ProfilePage } from './pages/ProfilePage';
import { AboutPage } from './pages/AboutPage';
import { PartnersPage } from './pages/admin/PartnersPage';
import { ChecklistTemplatesPage } from './pages/admin/ChecklistTemplatesPage';
import { EmployeePricingPage } from './pages/employee/EmployeePricingPage';
import { EmployeeSubmissionsPage } from './pages/employee/EmployeeSubmissionsPage';
import { ForbiddenPage } from './pages/ForbiddenPage';
import { GlobalErrorPage } from './pages/GlobalErrorPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { TenderSubmissionPage } from './pages/TenderSubmissionPage';
import { TenderTeamPage } from './pages/TenderTeamPage';
import { LegacyTenderRedirect, RoleHomeRedirect } from './routes/RoleRedirect';
import { ROUTES } from './routes/paths';
import './styles.css';
import { applyStoredTheme } from './state/theme';

applyStoredTheme();

const router = createBrowserRouter([
  { path: '/', element: <RoleHomeRedirect />, errorElement: <GlobalErrorPage /> },
  { path: ROUTES.LOGIN, element: <LoginPage />, errorElement: <GlobalErrorPage /> },
  { path: ROUTES.ABOUT, element: <AboutPage />, errorElement: <GlobalErrorPage /> },
  { path: '/403', element: <ForbiddenPage />, errorElement: <GlobalErrorPage /> },
  { path: '/dashboard', element: <RoleHomeRedirect />, errorElement: <GlobalErrorPage /> },
  { path: '/tenders/:id/*', element: <LegacyTenderRedirect />, errorElement: <GlobalErrorPage /> },
  { path: '/tenders/:id', element: <LegacyTenderRedirect />, errorElement: <GlobalErrorPage /> },
  {
    path: ROUTES.ADMIN.ROOT,
    element: <ProtectedRoute role="ADMIN"><AdminLayout /></ProtectedRoute>,
    errorElement: <GlobalErrorPage />,
    children: [
      { index: true, element: <Navigate to="dashboard" replace /> },
      { path: 'dashboard', element: <AdminDashboard /> },
      { path: 'employees', element: <EmployeesPage /> },
      { path: 'deadlines', element: <DeadlinesPage /> },
      { path: 'notifications', element: <NotificationsPage /> },
      { path: 'history', element: <HistoryPage /> },
      { path: 'performance/employees', element: <EmployeePerformancePage /> },
      { path: 'performance/employees/:id', element: <EmployeePerformanceDetailPage /> },
      { path: 'analytics/organizations', element: <OrganizationsAnalyticsPage /> },
      { path: 'reports', element: <ReportsPage /> },
      { path: 'activity', element: <ActivityPage /> },
      { path: 'tasks', element: <AdminTasksPage /> },
      { path: 'tasks/kanban', element: <KanbanPage /> },
      { path: 'favorites', element: <FavoritesPage /> },
      { path: 'partners', element: <PartnersPage /> },
      { path: 'checklists', element: <ChecklistTemplatesPage /> },
      { path: 'company', element: <CompanySettingsPage /> },
      { path: 'backups', element: <BackupsPage /> },
      { path: 'profile', element: <ProfilePage /> },
      { path: 'employees/new', element: <EmployeeFormPage /> },
      { path: 'employees/:id', element: <EmployeeFormPage /> },
      { path: 'tenders', element: <TendersPage /> },
      { path: 'tenders/new', element: <TenderFormPage /> },
      { path: 'tenders/:id/edit', element: <TenderFormPage /> },
      { path: 'tenders/:id', element: <TenderDetailPage /> },
      { path: 'tenders/:id/checklist', element: <TenderDetailPage /> },
      { path: 'tenders/:id/tasks', element: <TenderDetailPage /> },
      { path: 'tenders/:id/documents', element: <TenderDetailPage /> },
      { path: 'tenders/:id/comments', element: <TenderDetailPage /> },
      { path: 'tenders/:id/history', element: <TenderDetailPage /> },
      { path: 'tenders/:id/analysis', element: <TenderAnalysisPage /> },
      { path: 'tenders/:id/pricing', element: <PricingPage /> },
      { path: 'tenders/:id/team', element: <TenderTeamPage /> },
      { path: 'tenders/:id/submission', element: <TenderSubmissionPage /> }
    ]
  },
  {
    path: ROUTES.EMPLOYEE.ROOT,
    element: <ProtectedRoute role="EMPLOYEE"><EmployeeLayout /></ProtectedRoute>,
    errorElement: <GlobalErrorPage />,
    children: [
      { index: true, element: <Navigate to="dashboard" replace /> },
      { path: 'dashboard', element: <EmployeeDashboard /> },
      { path: 'tenders', element: <EmployeeTendersPage /> },
      { path: 'tasks', element: <EmployeeTasksPage /> },
      { path: 'pricing', element: <EmployeePricingPage /> },
      { path: 'submissions', element: <EmployeeSubmissionsPage /> },
      { path: 'tasks/kanban', element: <KanbanPage /> },
      { path: 'favorites', element: <FavoritesPage /> },
      { path: 'tenders/:id', element: <TenderDetailPage /> },
      { path: 'tenders/:id/checklist', element: <TenderDetailPage /> },
      { path: 'tenders/:id/tasks', element: <TenderDetailPage /> },
      { path: 'tenders/:id/documents', element: <TenderDetailPage /> },
      { path: 'tenders/:id/comments', element: <TenderDetailPage /> },
      { path: 'tenders/:id/history', element: <TenderDetailPage /> },
      { path: 'tenders/:id/analysis', element: <TenderAnalysisPage /> },
      { path: 'tenders/:id/pricing', element: <PricingPage /> },
      { path: 'tenders/:id/team', element: <TenderTeamPage /> },
      { path: 'tenders/:id/submission', element: <TenderSubmissionPage /> },
      { path: 'notifications', element: <NotificationsPage /> },
      { path: 'profile', element: <ProfilePage /> }
    ]
  },
  { path: '*', element: <NotFoundPage />, errorElement: <GlobalErrorPage /> }
]);

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <SettingsProvider>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </SettingsProvider>
  </React.StrictMode>
);
