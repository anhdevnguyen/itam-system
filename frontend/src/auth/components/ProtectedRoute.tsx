import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '@/auth/hooks/useAuth.tsx';

/**
 * Route guard — chặn các route cần đăng nhập.
 * - Đang load session → hiển thị spinner
 * - Chưa đăng nhập → redirect /login
 * - mustChangePassword = true → redirect /change-password (ngoại trừ chính trang đó)
 * - Đã đăng nhập bình thường → render children qua <Outlet>
 */
export default function ProtectedRoute() {
  const { isAuthenticated, isLoading, user } = useAuth();

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

  if (user?.mustChangePassword) {
    return <Navigate to="/change-password" replace />;
  }

  return <Outlet />;
}
