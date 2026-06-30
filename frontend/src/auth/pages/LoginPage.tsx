import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Eye, EyeOff, LogIn } from 'lucide-react';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import type { ApiError } from '@/lib/apiResponse.types';
import axios from 'axios';

/**
 * Trang đăng nhập — /login
 *
 * Luồng:
 * 1. User nhập email + password → submit
 * 2. Gọi authContext.login() → POST /auth/login qua apiClient
 * 3. login() trả về UserInfo sau khi cập nhật AuthContext
 * 4. Nếu mustChangePassword = true → redirect /change-password
 *    Ngược lại → redirect /dashboard
 * 5. Lỗi (401, 400, network) → hiển thị message dưới form
 */
export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<ApiError[]>([]);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErrors([]);
    setIsLoading(true);

    try {
      const user = await login(email, password);

      if (user.mustChangePassword) {
        navigate('/change-password', { replace: true });
      } else {
        navigate('/dashboard', { replace: true });
      }
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        const data = error.response.data;
        if (data?.errors && Array.isArray(data.errors)) {
          setErrors(data.errors as ApiError[]);
        } else {
          setErrors([{
            code: 'UNKNOWN_ERROR',
            field: null,
            message: 'Đã có lỗi xảy ra, vui lòng thử lại'
          }]);
        }
      } else {
        setErrors([{
          code: 'NETWORK_ERROR',
          field: null,
          message: 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.'
        }]);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const fieldError = (field: string) => errors.find((e) => e.field === field)?.message;
  const globalError = errors.find((e) => !e.field)?.message;

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        {/* Logo + tiêu đề */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-blue-600 rounded-2xl mb-4 shadow-lg">
            <svg
              className="w-8 h-8 text-white"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 3H5a2 2 0 00-2 2v4m6-6h10a2 2 0 012 2v4M9 3v18m0 0h10a2 2 0 002-2V9M9 21H5a2 2 0 01-2-2V9m0 0h18"
              />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-900">ITAM System</h1>
          <p className="text-gray-500 text-sm mt-1">Hệ thống quản lý thiết bị IT</p>
        </div>

        {/* Card form */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">Đăng nhập</h2>

          <form onSubmit={handleSubmit} noValidate>
            {/* Lỗi chung (không gắn với field) */}
            {globalError && (
              <div
                role="alert"
                className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700"
              >
                {globalError}
              </div>
            )}

            {/* Email */}
            <div className="mb-4">
              <label
                htmlFor="email"
                className="block text-sm font-medium text-gray-700 mb-1.5"
              >
                Email
              </label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={isLoading}
                aria-describedby={fieldError('email') ? 'email-error' : undefined}
                aria-invalid={fieldError('email') ? true : undefined}
                className={[
                  'w-full px-3 py-2.5 rounded-lg border text-sm transition-colors',
                  'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent',
                  'disabled:opacity-50 disabled:cursor-not-allowed',
                  fieldError('email')
                    ? 'border-red-400 bg-red-50'
                    : 'border-gray-300 bg-white hover:border-gray-400',
                ].join(' ')}
                placeholder="you@company.com"
              />
              {fieldError('email') && (
                <p id="email-error" className="mt-1 text-xs text-red-600">
                  {fieldError('email')}
                </p>
              )}
            </div>

            {/* Password */}
            <div className="mb-6">
              <label
                htmlFor="password"
                className="block text-sm font-medium text-gray-700 mb-1.5"
              >
                Mật khẩu
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={isLoading}
                  aria-describedby={fieldError('password') ? 'password-error' : undefined}
                  aria-invalid={fieldError('password') ? true : undefined}
                  className={[
                    'w-full px-3 py-2.5 pr-10 rounded-lg border text-sm transition-colors',
                    'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent',
                    'disabled:opacity-50 disabled:cursor-not-allowed',
                    fieldError('password')
                      ? 'border-red-400 bg-red-50'
                      : 'border-gray-300 bg-white hover:border-gray-400',
                  ].join(' ')}
                  placeholder="••••••••"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  disabled={isLoading}
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600 disabled:opacity-50"
                >
                  {showPassword ? <EyeOff size={16} aria-hidden="true" /> : <Eye size={16} aria-hidden="true" />}
                </button>
              </div>
              {fieldError('password') && (
                <p id="password-error" className="mt-1 text-xs text-red-600">
                  {fieldError('password')}
                </p>
              )}
            </div>

            {/* Submit button */}
            <button
              type="submit"
              disabled={isLoading || !email.trim() || !password.trim()}
              className={[
                'w-full flex items-center justify-center gap-2 px-4 py-2.5',
                'bg-blue-600 text-white text-sm font-medium rounded-lg transition-colors',
                'hover:bg-blue-700 active:bg-blue-800',
                'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2',
                'disabled:opacity-50 disabled:cursor-not-allowed',
              ].join(' ')}
            >
              {isLoading ? (
                <>
                  <span
                    className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"
                    aria-hidden="true"
                  />
                  <span>Đang đăng nhập...</span>
                </>
              ) : (
                <>
                  <LogIn size={16} aria-hidden="true" />
                  <span>Đăng nhập</span>
                </>
              )}
            </button>
          </form>
        </div>

        {/* Footer note */}
        <p className="text-center text-xs text-gray-400 mt-6">
          Tài khoản được cấp bởi bộ phận IT.{' '}
          <span className="text-gray-500">Liên hệ IT Staff nếu cần hỗ trợ.</span>
        </p>
      </div>
    </div>
  );
}
