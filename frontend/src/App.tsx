import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from '@/auth/hooks/useAuth.tsx';
import LoginPage from '@/auth/pages/LoginPage';
import ProtectedRoute from '@/auth/components/ProtectedRoute';

/**
 * Root App component — cấu hình routing và providers.
 *
 * Route structure:
 *   /login            → LoginPage (public)
 *   /change-password  → ChangePasswordPage (semi-protected — cần đăng nhập nhưng không cần mustChangePassword=false)
 *   /dashboard        → Dashboard (protected)
 *   /                 → redirect /dashboard
 *
 * TODO: Thêm các route feature (assets, employees, requests...) sau khi module hoàn thiện
 */
function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginPage />} />

          {/* TODO: ChangePasswordPage — sẽ thêm khi implement module employee/change-password */}
          <Route
            path="/change-password"
            element={
              <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-200 text-center">
                  <h2 className="text-xl font-semibold text-gray-900 mb-2">Đổi mật khẩu</h2>
                  <p className="text-gray-500 text-sm">Trang này sẽ được triển khai ở module Employee.</p>
                </div>
              </div>
            }
          />

          {/* Protected routes */}
          <Route element={<ProtectedRoute />}>
            {/* TODO: Dashboard sẽ thêm sau */}
            <Route
              path="/dashboard"
              element={
                <div className="min-h-screen flex items-center justify-center bg-gray-50">
                  <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-200 text-center">
                    <h2 className="text-xl font-semibold text-gray-900 mb-2">Dashboard</h2>
                    <p className="text-gray-500 text-sm">Đăng nhập thành công! Module dashboard sẽ được triển khai tiếp theo.</p>
                  </div>
                </div>
              }
            />
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
