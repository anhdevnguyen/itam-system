import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider } from '@/auth/hooks/useAuth.tsx';
import LoginPage from '@/auth/pages/LoginPage';
import ChangePasswordPage from '@/auth/pages/ChangePasswordPage';
import ProtectedRoute from '@/auth/components/ProtectedRoute';
import { AppLayout } from '@/shared/layout';
import { ErrorBoundary } from '@/shared/components';

// Lazy imports cho từng module
import { lazy, Suspense, type ReactNode } from 'react';
import { SkeletonLoader } from '@/shared/components';

/**
 * Wrapper tự động truyền pathname làm resetKey cho ErrorBoundary.
 * Đặt bên trong BrowserRouter để dùng được useLocation.
 * Khi navigate sang route mới, ErrorBoundary tự reset — không còn lỗi "lây" giữa các trang.
 */
function RouteErrorBoundary({ children, pageName }: { children: ReactNode; pageName?: string }) {
  const { pathname } = useLocation();
  return <ErrorBoundary resetKey={pathname} pageName={pageName}>{children}</ErrorBoundary>;
}

const DashboardPage = lazy(() => import('@/dashboard/pages/DashboardPage'));
const AssetsPage = lazy(() => import('@/assets/pages/AssetsPage'));
const AssetDetailPage = lazy(() => import('@/assets/pages/AssetDetailPage'));
const RequestsPage = lazy(() => import('@/requests/pages/RequestsPage'));
const RequestDetailPage = lazy(() => import('@/requests/pages/RequestDetailPage'));
const MaintenancePage = lazy(() => import('@/maintenance/pages/MaintenancePage'));
const MaintenanceDetailPage = lazy(() => import('@/maintenance/pages/MaintenanceDetailPage'));
const AuditPage = lazy(() => import('@/audit/pages/AuditPage'));
const AuditDetailPage = lazy(() => import('@/audit/pages/AuditDetailPage'));
const AuditScanPage = lazy(() => import('@/audit/pages/AuditScanPage'));
const EmployeesPage = lazy(() => import('@/employees/pages/EmployeesPage'));
const EmployeeDetailPage = lazy(() => import('@/employees/pages/EmployeeDetailPage'));
const BranchesPage = lazy(() => import('@/branches/pages/BranchesPage'));
const DepartmentsPage = lazy(() => import('@/departments/pages/DepartmentsPage'));
const CategoriesPage = lazy(() => import('@/categories/pages/CategoriesPage'));
const NotificationsPage = lazy(() => import('@/notifications/pages/NotificationsPage'));
const ProfilePage = lazy(() => import('@/employees/pages/ProfilePage'));

/** Fallback loading khi lazy-load component */
function PageLoader() {
  return (
    <div className="p-6">
      <SkeletonLoader rows={6} />
    </div>
  );
}

/**
 * Root App component — cấu hình routing và providers.
 *
 * Route structure:
 *   /login                       → LoginPage (public)
 *   /change-password             → ChangePasswordPage (semi-protected)
 *   / (AppLayout)
 *     /dashboard                 → DashboardPage
 *     /assets                    → AssetsPage
 *     /assets/:id                → AssetDetailPage
 *     /requests                  → RequestsPage
 *     /requests/:id              → RequestDetailPage
 *     /maintenance               → MaintenancePage
 *     /maintenance/:id           → MaintenanceDetailPage
 *     /audit                     → AuditPage
 *     /audit/:id                 → AuditDetailPage
 *     /audit/:id/scan            → AuditScanPage
 *     /employees                 → EmployeesPage
 *     /employees/:id             → EmployeeDetailPage
 *     /branches                  → BranchesPage
 *     /departments               → DepartmentsPage
 *     /categories                → CategoriesPage
 *     /notifications             → NotificationsPage
 *     /profile                   → ProfilePage
 */
function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginPage />} />

          {/* Protected routes — bọc trong AppLayout */}
          <Route element={<ProtectedRoute />}>
            {/*
              /change-password nằm trong ProtectedRoute nhưng NGOÀI AppLayout —
              user phải đăng nhập mới vào được, kể cả khi mustChangePassword=true
              (ProtectedRoute cho phép mustChangePassword đi tới /change-password).
            */}
            <Route path="/change-password" element={
              <RouteErrorBoundary pageName="Đổi mật khẩu">
                <ChangePasswordPage />
              </RouteErrorBoundary>
            } />
            <Route element={<AppLayout />}>
              <Route
                path="/dashboard"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <DashboardPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />

              {/* Assets */}
              <Route
                path="/assets"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <AssetsPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />
              <Route
                path="/assets/:id"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <AssetDetailPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />

              {/* Requests */}
              <Route
                path="/requests"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <RequestsPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />
              <Route
                path="/requests/:id"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <RequestDetailPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />

              {/* Maintenance */}
              <Route
                path="/maintenance"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <MaintenancePage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />
              <Route
                path="/maintenance/:id"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <MaintenanceDetailPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />

              {/* Audit */}
              <Route
                path="/audit"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <AuditPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />
              <Route
                path="/audit/:id"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <AuditDetailPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />
              <Route
                path="/audit/:id/scan"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <AuditScanPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />

              {/* Employees */}
              <Route
                path="/employees"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <EmployeesPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />
              <Route
                path="/employees/:id"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <EmployeeDetailPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />

              {/* Config / Settings */}
              <Route
                path="/branches"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <BranchesPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />
              <Route
                path="/departments"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <DepartmentsPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />
              <Route
                path="/categories"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <CategoriesPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />

              {/* Notifications */}
              <Route
                path="/notifications"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <NotificationsPage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />

              {/* Profile */}
              <Route
                path="/profile"
                element={
                  <RouteErrorBoundary>
                    <Suspense fallback={<PageLoader />}>
                      <ProfilePage />
                    </Suspense>
                  </RouteErrorBoundary>
                }
              />
            </Route>
          </Route>

          {/* Fallback */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
