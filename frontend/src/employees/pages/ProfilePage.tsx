import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { KeyRound } from 'lucide-react';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import { StatusBadge } from '@/shared/components';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Quản trị viên',
  IT_STAFF: 'Nhân viên IT',
  MANAGER: 'Trưởng phòng',
  EMPLOYEE: 'Nhân viên',
};

/**
 * ProfilePage — Thông tin tài khoản của người dùng đang đăng nhập.
 */
export default function ProfilePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [copied, setCopied] = useState(false);

  const copyEmail = () => {
    if (!user?.email) return;
    navigator.clipboard.writeText(user.email).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  if (!user) return null;

  return (
    <div className="space-y-5 max-w-xl">
      <div>
        <h1 className="text-2xl font-semibold text-gray-900">Hồ sơ cá nhân</h1>
        <p className="text-sm text-gray-500 mt-0.5">Thông tin tài khoản của bạn</p>
      </div>

      {/* Avatar + name */}
      <div className="bg-white rounded-xl border border-gray-200 p-5 flex items-center gap-4">
        <div
          className="w-14 h-14 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-600 text-xl font-semibold shrink-0"
          aria-hidden="true"
        >
          {user.fullName.charAt(0).toUpperCase()}
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-lg font-semibold text-gray-900 truncate">{user.fullName}</p>
          <button
            onClick={copyEmail}
            className="text-sm text-gray-400 hover:text-indigo-600 transition-colors"
            title="Sao chép email"
          >
            {copied ? 'Đã sao chép!' : user.email}
          </button>
        </div>
        <StatusBadge status={user.role} label={ROLE_LABELS[user.role] ?? user.role} />
      </div>

      {/* Info detail */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <dl className="space-y-3">
          <InfoRow label="Họ tên" value={user.fullName} />
          <InfoRow label="Email" value={user.email} />
          <InfoRow label="Vai trò" value={ROLE_LABELS[user.role] ?? user.role} />
          <InfoRow label="Mã chi nhánh" value={`#${user.branchId}`} />
        </dl>
      </div>

      {/* Actions */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <h2 className="text-sm font-semibold text-gray-700 mb-3">Bảo mật</h2>
        <button
          onClick={() => navigate('/change-password')}
          className="flex items-center gap-2 px-4 py-2 text-sm text-indigo-600 border border-indigo-200 rounded-lg hover:bg-indigo-50 transition-colors"
        >
          <KeyRound size={15} aria-hidden="true" />
          Đổi mật khẩu
        </button>
      </div>
    </div>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <dt className="text-sm text-gray-500 shrink-0">{label}</dt>
      <dd className="text-sm text-gray-900 font-medium text-right truncate">{value}</dd>
    </div>
  );
}
