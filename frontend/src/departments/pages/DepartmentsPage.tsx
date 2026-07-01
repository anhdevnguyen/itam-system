import { useState, useCallback, useEffect } from 'react';
import { Plus, Search, Pencil, Trash2 } from 'lucide-react';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import { SkeletonLoader, EmptyState, Modal, ConfirmDialog } from '@/shared/components';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';

interface Department {
  id: number;
  name: string;
  branchId: number;
  branchName: string;
  managerId: number | null;
  managerName: string | null;
  createdAt: string;
}

interface CreateDepartmentRequest {
  name: string;
  branchId: number;
}

/**
 * DepartmentsPage — Quản lý phòng ban.
 * Chỉ ADMIN mới có quyền tạo/sửa/xóa.
 */
export default function DepartmentsPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const [departments, setDepartments] = useState<Department[]>([]);
  const [branches, setBranches] = useState<{ id: number; name: string }[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState('');

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editTarget, setEditTarget] = useState<Department | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Department | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [formError, setFormError] = useState('');

  const [formName, setFormName] = useState('');
  const [formBranchId, setFormBranchId] = useState<number | ''>('');

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    try {
      const [deptRes, branchRes] = await Promise.all([
        apiClient.get<ApiResponse<Department[]>>('/departments?page=0&size=200'),
        apiClient.get<ApiResponse<{ id: number; name: string }[]>>('/branches?page=0&size=100'),
      ]);
      setDepartments(deptRes.data.data ?? []);
      setBranches(branchRes.data.data ?? []);
    } catch {
      setDepartments([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const openCreate = () => {
    setFormName('');
    setFormBranchId(user?.branchId ?? '');
    setFormError('');
    setShowCreateModal(true);
  };

  const openEdit = (dept: Department) => {
    setFormName(dept.name);
    setFormBranchId(dept.branchId);
    setFormError('');
    setEditTarget(dept);
  };

  const handleCreate = async () => {
    if (!formName.trim() || !formBranchId) {
      setFormError('Tên phòng ban và chi nhánh là bắt buộc');
      return;
    }
    setFormError('');
    setActionLoading(true);
    try {
      const body: CreateDepartmentRequest = { name: formName.trim(), branchId: Number(formBranchId) };
      await apiClient.post('/departments', body);
      setShowCreateModal(false);
      fetchData();
    } catch {
      setFormError('Không thể tạo phòng ban');
    } finally {
      setActionLoading(false);
    }
  };

  const handleUpdate = async () => {
    if (!editTarget) return;
    if (!formName.trim()) {
      setFormError('Tên phòng ban là bắt buộc');
      return;
    }
    setFormError('');
    setActionLoading(true);
    try {
      await apiClient.put(`/departments/${editTarget.id}`, { name: formName.trim() });
      setEditTarget(null);
      fetchData();
    } catch {
      setFormError('Không thể cập nhật phòng ban');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setActionLoading(true);
    try {
      await apiClient.delete(`/departments/${deleteTarget.id}`);
      setDeleteTarget(null);
      fetchData();
    } catch {
      // silent
    } finally {
      setActionLoading(false);
    }
  };

  const filtered = search
    ? departments.filter(
        (d) =>
          d.name.toLowerCase().includes(search.toLowerCase()) ||
          d.branchName.toLowerCase().includes(search.toLowerCase()),
      )
    : departments;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Phòng ban</h1>
          <p className="text-sm text-gray-500 mt-0.5">{departments.length} phòng ban</p>
        </div>
        {isAdmin && (
          <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition-colors">
            <Plus size={16} aria-hidden="true" />
            Thêm phòng ban
          </button>
        )}
      </div>

      <div className="relative max-w-sm">
        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" aria-hidden="true" />
        <input
          type="search"
          placeholder="Tìm tên phòng ban..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-9 pr-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="p-4"><SkeletonLoader rows={5} /></div>
        ) : filtered.length === 0 ? (
          <EmptyState message="Không có phòng ban nào" description={search ? 'Thử tìm kiếm với từ khóa khác' : undefined} />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50">
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Tên phòng ban</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider hidden md:table-cell">Chi nhánh</th>
                {isAdmin && <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Thao tác</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filtered.map((d) => (
                <tr key={d.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 font-medium text-gray-900">{d.name}</td>
                  <td className="px-4 py-3 text-gray-500 hidden md:table-cell">{d.branchName}</td>
                  {isAdmin && (
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => openEdit(d)} className="p-1.5 text-gray-400 border border-gray-200 rounded-md hover:bg-gray-50 transition-colors" aria-label={`Chỉnh sửa ${d.name}`}>
                          <Pencil size={13} aria-hidden="true" />
                        </button>
                        <button onClick={() => setDeleteTarget(d)} className="p-1.5 text-red-400 border border-red-200 rounded-md hover:bg-red-50 transition-colors" aria-label={`Xóa ${d.name}`}>
                          <Trash2 size={13} aria-hidden="true" />
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Create modal */}
      <Modal open={showCreateModal} onClose={() => setShowCreateModal(false)} title="Thêm phòng ban">
        <div className="space-y-4">
          {formError && <div role="alert" className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">{formError}</div>}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Tên phòng ban <span className="text-red-500">*</span></label>
            <input type="text" value={formName} onChange={(e) => setFormName(e.target.value)} placeholder="VD: Phòng Kỹ thuật" disabled={actionLoading} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Chi nhánh <span className="text-red-500">*</span></label>
            <select value={formBranchId} onChange={(e) => setFormBranchId(e.target.value ? Number(e.target.value) : '')} disabled={actionLoading} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500">
              <option value="">— Chọn chi nhánh —</option>
              {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
            </select>
          </div>
          <div className="flex justify-end gap-3 pt-1">
            <button type="button" onClick={() => setShowCreateModal(false)} disabled={actionLoading} className="px-4 py-2 text-sm text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-50">Hủy</button>
            <button type="button" onClick={handleCreate} disabled={actionLoading} className="px-4 py-2 text-sm text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg disabled:opacity-50">{actionLoading ? 'Đang lưu...' : 'Tạo phòng ban'}</button>
          </div>
        </div>
      </Modal>

      {/* Edit modal */}
      <Modal open={!!editTarget} onClose={() => setEditTarget(null)} title="Chỉnh sửa phòng ban">
        <div className="space-y-4">
          {formError && <div role="alert" className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">{formError}</div>}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Tên phòng ban <span className="text-red-500">*</span></label>
            <input type="text" value={formName} onChange={(e) => setFormName(e.target.value)} disabled={actionLoading} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
          <div className="flex justify-end gap-3 pt-1">
            <button type="button" onClick={() => setEditTarget(null)} disabled={actionLoading} className="px-4 py-2 text-sm text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-50">Hủy</button>
            <button type="button" onClick={handleUpdate} disabled={actionLoading} className="px-4 py-2 text-sm text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg disabled:opacity-50">{actionLoading ? 'Đang lưu...' : 'Lưu thay đổi'}</button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Xóa phòng ban"
        message={`Bạn có chắc muốn xóa phòng ban "${deleteTarget?.name}"?`}
        confirmLabel="Xóa"
        variant="danger"
        isLoading={actionLoading}
      />
    </div>
  );
}
