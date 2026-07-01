import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/auth/hooks/useAuth.tsx';

/**
 * Route guard — chặn các route cần đăng nhập.
 * - Đang load session → hiển thị spinner
 * - Chưa đăng nhập → redirect /login
 * - mustChangePassword = true → redirect /change-password (trừ khi đang ở đó rồi)
 * - Đã đăng nhập bình thường → render children qua <Outlet>
 */
export default function ProtectedRoute() {
  const { isAuthenticated, isLoading, user } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div
          className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"
          role="status"
          aria-label="Đang tải..."
        />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // Nếu mustChangePassword = true và chưa ở trang đổi mật khẩu → redirect
  // Kiểm tra location.pathname để tránh redirect loop khi đã ở /change-password
  if (user?.mustChangePassword && location.pathname !== '/change-password') {
    return <Navigate to="/change-password" replace />;
  }

  return <Outlet />;
}
