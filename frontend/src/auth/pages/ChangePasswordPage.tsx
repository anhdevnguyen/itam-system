import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Eye, EyeOff, KeyRound } from 'lucide-react';
import apiClient from '@/lib/apiClient';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import type { ApiError } from '@/lib/apiResponse.types';
import axios from 'axios';

/**
 * Trang đổi mật khẩu — /change-password
 * Dùng cho cả:
 *  - Lần đăng nhập đầu (mustChangePassword = true) — bắt buộc trước khi vào hệ thống
 *  - Đổi mật khẩu chủ động từ menu user
 */
export default function ChangePasswordPage() {
  const navigate = useNavigate();
  const { user, refreshAccessToken } = useAuth();

  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showOld, setShowOld] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [globalError, setGlobalError] = useState('');
  const [success, setSuccess] = useState(false);

  const validate = () => {
    const errs: Record<string, string> = {};
    if (!oldPassword) errs.oldPassword = 'Vui lòng nhập mật khẩu hiện tại';
    if (!newPassword) errs.newPassword = 'Vui lòng nhập mật khẩu mới';
    else if (!/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).{8,}$/.test(newPassword)) {
      errs.newPassword = 'Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số';
    }
    if (newPassword !== confirmPassword) errs.confirmPassword = 'Mật khẩu xác nhận không khớp';
    return errs;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }
    setErrors({});
    setGlobalError('');
    setIsLoading(true);

    try {
      await apiClient.put('/employees/me/change-password', { oldPassword, newPassword });
      setSuccess(true);
      // Refresh token để lấy user mới (mustChangePassword = false)
      await refreshAccessToken();
      setTimeout(() => navigate('/dashboard', { replace: true }), 1500);
    } catch (err) {
      if (axios.isAxiosError(err) && err.response) {
        const apiErrors = err.response.data?.errors as ApiError[] | undefined;
        if (apiErrors) {
          const fieldErrs: Record<string, string> = {};
          let global = '';
          apiErrors.forEach((e) => {
            if (e.field) fieldErrs[e.field] = e.message;
            else global = e.message;
          });
          setErrors(fieldErrs);
          setGlobalError(global);
        } else {
          setGlobalError('Đã có lỗi xảy ra, vui lòng thử lại');
        }
      } else {
        setGlobalError('Không thể kết nối đến máy chủ');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const isMustChange = user?.mustChangePassword;

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-indigo-600 rounded-2xl mb-4 shadow-lg">
            <KeyRound size={28} className="text-white" aria-hidden="true" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">
            {isMustChange ? 'Đặt mật khẩu mới' : 'Đổi mật khẩu'}
          </h1>
          {isMustChange && (
            <p className="text-sm text-amber-600 mt-1 font-medium">
              Bạn cần đặt mật khẩu mới trước khi sử dụng hệ thống
            </p>
          )}
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
          {success ? (
            <div className="flex flex-col items-center py-4 gap-3">
              <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
                <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <p className="text-sm font-medium text-gray-900">Đổi mật khẩu thành công!</p>
              <p className="text-xs text-gray-400">Đang chuyển hướng...</p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} noValidate className="space-y-4">
              {globalError && (
                <div role="alert" className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
                  {globalError}
                </div>
              )}

              <PasswordField
                id="oldPassword"
                label="Mật khẩu hiện tại"
                value={oldPassword}
                onChange={setOldPassword}
                show={showOld}
                onToggle={() => setShowOld((v) => !v)}
                error={errors.oldPassword}
                disabled={isLoading}
                autoComplete="current-password"
              />
              <PasswordField
                id="newPassword"
                label="Mật khẩu mới"
                value={newPassword}
                onChange={setNewPassword}
                show={showNew}
                onToggle={() => setShowNew((v) => !v)}
                error={errors.newPassword}
                disabled={isLoading}
                autoComplete="new-password"
                hint="Tối thiểu 8 ký tự, có chữ hoa, chữ thường và số"
              />
              <PasswordField
                id="confirmPassword"
                label="Xác nhận mật khẩu mới"
                value={confirmPassword}
                onChange={setConfirmPassword}
                show={showConfirm}
                onToggle={() => setShowConfirm((v) => !v)}
                error={errors.confirmPassword}
                disabled={isLoading}
                autoComplete="new-password"
              />

              <button
                type="submit"
                disabled={isLoading}
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed mt-2"
              >
                {isLoading ? 'Đang xử lý...' : 'Xác nhận đổi mật khẩu'}
              </button>

              {!isMustChange && (
                <button
                  type="button"
                  onClick={() => navigate(-1)}
                  className="w-full py-2 text-sm text-gray-500 hover:text-gray-700 transition-colors"
                >
                  Hủy
                </button>
              )}
            </form>
          )}
        </div>
      </div>
    </div>
  );
}

// ── PasswordField helper ─────────────────────────────────────────────────────
interface PasswordFieldProps {
  id: string;
  label: string;
  value: string;
  onChange: (v: string) => void;
  show: boolean;
  onToggle: () => void;
  error?: string;
  disabled?: boolean;
  autoComplete?: string;
  hint?: string;
}

function PasswordField({ id, label, value, onChange, show, onToggle, error, disabled, autoComplete, hint }: PasswordFieldProps) {
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-gray-700 mb-1.5">
        {label}
      </label>
      <div className="relative">
        <input
          id={id}
          type={show ? 'text' : 'password'}
          autoComplete={autoComplete}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled}
          aria-invalid={error ? true : undefined}
          className={`w-full px-3 py-2.5 pr-10 rounded-lg border text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent disabled:opacity-50 ${error ? 'border-red-400 bg-red-50' : 'border-gray-300'}`}
        />
        <button
          type="button"
          onClick={onToggle}
          disabled={disabled}
          aria-label={show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
          className="absolute right-2.5 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600"
        >
          {show ? <EyeOff size={16} aria-hidden="true" /> : <Eye size={16} aria-hidden="true" />}
        </button>
      </div>
      {hint && !error && <p className="mt-1 text-xs text-gray-400">{hint}</p>}
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}
