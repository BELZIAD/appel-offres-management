export const ROUTES = {
  LOGIN: '/login',
  ABOUT: '/about',
  ADMIN: {
    ROOT: '/admin',
    DASHBOARD: '/admin/dashboard',
    TENDERS: '/admin/tenders',
    EMPLOYEES: '/admin/employees',
    DEADLINES: '/admin/deadlines',
    TASKS: '/admin/tasks',
    TASKS_KANBAN: '/admin/tasks/kanban',
    FAVORITES: '/admin/favorites',
    PARTNERS: '/admin/partners',
    CHECKLISTS: '/admin/checklists',
    PERFORMANCE_EMPLOYEES: '/admin/performance/employees',
    ORGANIZATIONS: '/admin/analytics/organizations',
    REPORTS: '/admin/reports',
    ACTIVITY: '/admin/activity',
    NOTIFICATIONS: '/admin/notifications',
    HISTORY: '/admin/history',
    COMPANY: '/admin/company',
    BACKUPS: '/admin/backups',
    PROFILE: '/admin/profile'
  },
  EMPLOYEE: {
    ROOT: '/employee',
    DASHBOARD: '/employee/dashboard',
    TENDERS: '/employee/tenders',
    TASKS: '/employee/tasks',
    TASKS_KANBAN: '/employee/tasks/kanban',
    PRICING: '/employee/pricing',
    SUBMISSIONS: '/employee/submissions',
    FAVORITES: '/employee/favorites',
    NOTIFICATIONS: '/employee/notifications',
    PROFILE: '/employee/profile'
  }
} as const;

export function adminTenderRoute(id: number | string) {
  return `/admin/tenders/${id}`;
}

export function employeeTenderRoute(id: number | string) {
  return `/employee/tenders/${id}`;
}

export function tenderPricingRoute(id: number | string, role: 'ADMIN' | 'EMPLOYEE') {
  return `${role === 'ADMIN' ? adminTenderRoute(id) : employeeTenderRoute(id)}/pricing`;
}
