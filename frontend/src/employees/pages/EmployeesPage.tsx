import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, RotateCcw } from 'lucide-react';
import { useEmployees, useEmployeeActions } from '../hooks/useEmployees';
import { SkeletonLoader, EmptyState, Pagination, StatusBadge, ConfirmDialog } from '@/shared/components';
import type { RoleCode } from '../types/employee.types';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Quản trị viên',
  IT_STAFF: 'Nhân viên IT',
  MANAGER: 'Trưởng phòng',
  EMPLOYEE: 'Nhân viên',
};

const ROLE_OPTIONS: { value: RoleCode | ''; label: string }[] = [
  { value: '', label: 'Tất cả vai trò' },
  { value: 'ADMIN', label: 'Quản trị viên' },
  { value: 'IT_STAFF', label: 'Nhân viên IT' },
  { value: 'MANAGER', label: 'Trưởng phòng' },
  { value: 'EMPLOYEE', label: 'Nhân viên' },
];

export default function EmployeesPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [roleCode, setRoleCode] = useState<RoleCode | ''>('');
  const [search, setSearch] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);
  const [resetTarget, setResetTarget] = useState<{ id: number; name: string } | null>(null);
  const [tempPassword, setTempPassword] = useState('');

  const { employees, isLoading, totalPages, totalElements, refetch } = useEmployees(
    { roleCode: roleCode || undefined },
    page
  );
  const { softDelete, resetPassword, isLoading: actionLoading } = useEmployeeActions();

  const filtered = search
    ? employees.filter(
        (e) =>
          e.fullName.toLowerCase().includes(search.toLowerCase()) ||
          e.email.toLowerCase().includes(search.toLowerCase())
      )
    : employees;

  const handleDelete = async () => {
    if (!deleteTarget) return;
    await softDelete(deleteTarget);
    setDeleteTarget(null);
    refetch();
  };

  const handleResetPassword = async () => {
    if (!resetTarget) return;
    const pwd = await resetPassword(resetTarget.id);
    setTempPassword(pwd);
    setResetTarget(null);
  };

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Nhân viên</h1>
          <p className="text-sm text-gray-500 mt-0.5">{totalElements} nhân viên</p>
        </div>
        <button
          onClick={() => navigate('/employees/new')}
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition-colors"
        >
          <Plus size={16} aria-hidden="true" />
          Thêm nhân viên
        </button>
      </div>

      {/* Filter bar */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" aria-hidden="true" />
          <input
            type="search"
            placeholder="Tìm tên hoặc email..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <select
          value={roleCode}
          onChange={(e) => { setRoleCode(e.target.value as RoleCode | ''); setPage(0); }}
          className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
        >
          {ROLE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      </div>

      {/* Tạm mật khẩu sau reset */}
      {tempPassword && (
        <div className="p-3 bg-green-50 border border-green-200 rounded-lg flex items-center justify-between">
          <span className="text-sm text-green-800">
            Mật khẩu tạm thời: <strong className="font-mono">{tempPassword}</strong>
          </span>
          <button onClick={() => setTempPassword('')} className="text-green-600 text-xs hover:underline">Đóng</button>
        </div>
      )}

      {/* Table */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="p-4"><SkeletonLoader rows={8} /></div>
        ) : filtered.length === 0 ? (
          <EmptyState
            message="Không có nhân viên nào"
            description={search ? 'Thử tìm kiếm với từ khóa khác' : undefined}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Nhân viên</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Vai trò</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider hidden md:table-cell">Chi nhánh</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider hidden lg:table-cell">Phòng ban</th>
                  <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.map((emp) => (
                  <tr key={emp.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3">
                      <div>
                        <p className="font-medium text-gray-900">{emp.fullName}</p>
                        <p className="text-xs text-gray-400">{emp.email}</p>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={emp.roleCode} label={ROLE_LABELS[emp.roleCode] ?? emp.roleCode} />
                      {emp.mustChangePassword && (
                        <span className="ml-1 text-xs text-amber-600">(chờ đổi mật khẩu)</span>
                      )}
                    </td>
                    <td className="px-4 py-3 hidden md:table-cell text-gray-600">{emp.branchName}</td>
                    <td className="px-4 py-3 hidden lg:table-cell text-gray-600">{emp.departmentName ?? '—'}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          onClick={() => navigate(`/employees/${emp.id}`)}
                          className="px-2.5 py-1.5 text-xs text-gray-600 border border-gray-200 rounded-md hover:bg-gray-50 transition-colors"
                        >
                          Chi tiết
                        </button>
                        <button
                          onClick={() => setResetTarget({ id: emp.id, name: emp.fullName })}
                          className="px-2.5 py-1.5 text-xs text-amber-600 border border-amber-200 rounded-md hover:bg-amber-50 transition-colors"
                          title="Reset mật khẩu"
                        >
                          <RotateCcw size={13} aria-hidden="true" />
                        </button>
                        <button
                          onClick={() => setDeleteTarget(emp.id)}
                          className="px-2.5 py-1.5 text-xs text-red-600 border border-red-200 rounded-md hover:bg-red-50 transition-colors"
                        >
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {totalPages > 1 && (
        <Pagination
          pagination={{ page, size: 20, totalElements, totalPages }}
          onPageChange={setPage}
        />
      )}

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Xóa nhân viên"
        message="Bạn có chắc muốn xóa nhân viên này? Hành động này không thể hoàn tác."
        confirmLabel="Xóa"
        variant="danger"
        isLoading={actionLoading}
      />

      <ConfirmDialog
        open={!!resetTarget}
        onClose={() => setResetTarget(null)}
        onConfirm={handleResetPassword}
        title="Reset mật khẩu"
        message={`Reset mật khẩu cho ${resetTarget?.name}? Mật khẩu tạm thời mới sẽ được hiển thị sau khi xác nhận.`}
        confirmLabel="Reset"
        variant="warning"
        isLoading={actionLoading}
      />
    </div>
  );
}
