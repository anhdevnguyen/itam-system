import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Pencil } from 'lucide-react';
import { useEmployee, useEmployeeActions } from '../hooks/useEmployees';
import { SkeletonLoader, EmptyState, Modal, StatusBadge } from '@/shared/components';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import type { UpdateEmployeeRequest } from '../types/employee.types';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Quản trị viên',
  IT_STAFF: 'Nhân viên IT',
  MANAGER: 'Trưởng phòng',
  EMPLOYEE: 'Nhân viên',
};

/**
 * EmployeeDetailPage — Xem và chỉnh sửa thông tin nhân viên.
 * Chỉ ADMIN mới có nút chỉnh sửa.
 */
export default function EmployeeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const employeeId = id ? Number(id) : null;
  const { employee, isLoading } = useEmployee(employeeId);
  const { update, isLoading: actionLoading } = useEmployeeActions();

  const [showEdit, setShowEdit] = useState(false);
  const [formName, setFormName] = useState('');
  const [formError, setFormError] = useState('');

  const openEdit = () => {
    setFormName(employee?.fullName ?? '');
    setFormError('');
    setShowEdit(true);
  };

  const handleUpdate = async () => {
    if (!employeeId) return;
    if (!formName.trim()) {
      setFormError('Họ tên là bắt buộc');
      return;
    }
    setFormError('');
    try {
      const body: UpdateEmployeeRequest = { fullName: formName.trim() };
      await update(employeeId, body);
      setShowEdit(false);
      // Reload by navigating to same page
      navigate(0);
    } catch {
      setFormError('Không thể cập nhật thông tin nhân viên');
    }
  };

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });

  if (isLoading) return <div className="p-6"><SkeletonLoader rows={8} /></div>;

  if (!employee) {
    return (
      <EmptyState
        message="Không tìm thấy nhân viên"
        action={
          <button onClick={() => navigate('/employees')} className="px-4 py-2 text-sm text-indigo-600 border border-indigo-200 rounded-lg hover:bg-indigo-50 transition-colors">
            Quay lại danh sách
          </button>
        }
      />
    );
  }

  return (
    <div className="space-y-5 max-w-2xl">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <button
            onClick={() => navigate('/employees')}
            className="mt-0.5 p-1.5 rounded-lg text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors"
            aria-label="Quay lại"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <h1 className="text-2xl font-semibold text-gray-900">{employee.fullName}</h1>
            <p className="text-sm text-gray-400">{employee.email}</p>
          </div>
        </div>
        {isAdmin && (
          <button
            onClick={openEdit}
            className="flex items-center gap-1.5 px-3 py-2 text-sm text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
          >
            <Pencil size={14} aria-hidden="true" />
            Chỉnh sửa
          </button>
        )}
      </div>

      {/* Info card */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <InfoRow label="Họ tên" value={employee.fullName} />
          <InfoRow label="Email" value={employee.email} />
          <InfoRow label="Vai trò" value={<StatusBadge status={employee.roleCode} label={ROLE_LABELS[employee.roleCode] ?? employee.roleCode} />} />
          <InfoRow label="Chi nhánh" value={employee.branchName} />
          <InfoRow label="Phòng ban" value={employee.departmentName ?? '—'} />
          <InfoRow label="Ngày tạo" value={formatDate(employee.createdAt)} />
          {employee.mustChangePassword && (
            <div className="col-span-full">
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-amber-50 border border-amber-200 rounded-full text-xs text-amber-700">
                Chờ đổi mật khẩu lần đầu
              </span>
            </div>
          )}
        </dl>
      </div>

      {/* Edit modal */}
      <Modal open={showEdit} onClose={() => setShowEdit(false)} title="Chỉnh sửa nhân viên">
        <div className="space-y-4">
          {formError && (
            <div role="alert" className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">{formError}</div>
          )}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Họ tên <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              disabled={actionLoading}
              className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div className="flex justify-end gap-3 pt-1">
            <button type="button" onClick={() => setShowEdit(false)} disabled={actionLoading} className="px-4 py-2 text-sm text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-50">Hủy</button>
            <button type="button" onClick={handleUpdate} disabled={actionLoading} className="px-4 py-2 text-sm text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg disabled:opacity-50">
              {actionLoading ? 'Đang lưu...' : 'Lưu thay đổi'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

function InfoRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-0.5">{label}</dt>
      <dd className="text-sm text-gray-900">{value}</dd>
    </div>
  );
}
