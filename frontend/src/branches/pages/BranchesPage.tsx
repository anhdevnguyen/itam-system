import { useState } from 'react';
import { Plus, Search, Pencil, Trash2 } from 'lucide-react';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import { SkeletonLoader, EmptyState, Modal, ConfirmDialog } from '@/shared/components';
import { useBranches, useBranchActions } from '../hooks/useBranches';
import type { Branch, CreateBranchRequest, UpdateBranchRequest } from '../types/branch.types';

/**
 * BranchesPage — Quản lý chi nhánh.
 * Theo docs/04-API.md mục 8:
 *   GET/POST/PUT/DELETE /branches — chỉ ADMIN mới tạo/sửa/xóa.
 */
export default function BranchesPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const [search, setSearch] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editTarget, setEditTarget] = useState<Branch | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Branch | null>(null);
  const [deleteError, setDeleteError] = useState('');

  // Form state
  const [formCode, setFormCode] = useState('');
  const [formName, setFormName] = useState('');
  const [formAddress, setFormAddress] = useState('');
  const [formError, setFormError] = useState('');

  const { branches, isLoading, error, refetch } = useBranches();
  const { create, update, softDelete, isLoading: actionLoading } = useBranchActions();

  const openCreate = () => {
    setFormCode('');
    setFormName('');
    setFormAddress('');
    setFormError('');
    setShowCreateModal(true);
  };

  const openEdit = (branch: Branch) => {
    setFormCode(branch.code);
    setFormName(branch.name);
    setFormAddress(branch.address ?? '');
    setFormError('');
    setEditTarget(branch);
  };

  const handleCreate = async () => {
    if (!formCode.trim() || !formName.trim()) {
      setFormError('Mã và tên chi nhánh là bắt buộc');
      return;
    }
    setFormError('');
    try {
      const body: CreateBranchRequest = {
        code: formCode.trim().toUpperCase(),
        name: formName.trim(),
        address: formAddress.trim() || undefined,
      };
      await create(body);
      setShowCreateModal(false);
      refetch();
    } catch {
      setFormError('Không thể tạo chi nhánh. Mã có thể đã tồn tại.');
    }
  };

  const handleUpdate = async () => {
    if (!editTarget) return;
    if (!formCode.trim() || !formName.trim()) {
      setFormError('Mã và tên chi nhánh là bắt buộc');
      return;
    }
    setFormError('');
    try {
      const body: UpdateBranchRequest = {
        code: formCode.trim().toUpperCase(),
        name: formName.trim(),
        address: formAddress.trim() || undefined,
      };
      await update(editTarget.id, body);
      setEditTarget(null);
      refetch();
    } catch {
      setFormError('Không thể cập nhật chi nhánh');
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await softDelete(deleteTarget.id);
      setDeleteTarget(null);
      setDeleteError('');
      refetch();
    } catch {
      setDeleteError('Không thể xóa chi nhánh. Có thể còn nhân viên hoặc thiết bị thuộc chi nhánh này.');
    }
  };

  const filtered = search
    ? branches.filter(
        (b) =>
          b.name.toLowerCase().includes(search.toLowerCase()) ||
          b.code.toLowerCase().includes(search.toLowerCase()),
      )
    : branches;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Chi nhánh</h1>
          <p className="text-sm text-gray-500 mt-0.5">{branches.length} chi nhánh</p>
        </div>
        {isAdmin && (
          <button
            onClick={openCreate}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition-colors"
          >
            <Plus size={16} aria-hidden="true" />
            Thêm chi nhánh
          </button>
        )}
      </div>

      <div className="relative max-w-sm">
        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" aria-hidden="true" />
        <input
          type="search"
          placeholder="Tìm tên hoặc mã chi nhánh..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-9 pr-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="p-4"><SkeletonLoader rows={5} /></div>
        ) : error ? (
          <EmptyState message={error} />
        ) : filtered.length === 0 ? (
          <EmptyState message="Không có chi nhánh nào" description={search ? 'Thử tìm kiếm với từ khóa khác' : undefined} />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50">
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Mã</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Tên</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider hidden md:table-cell">Địa chỉ</th>
                {isAdmin && (
                  <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Thao tác</th>
                )}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filtered.map((b) => (
                <tr key={b.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 font-mono text-xs text-gray-600">{b.code}</td>
                  <td className="px-4 py-3 font-medium text-gray-900">{b.name}</td>
                  <td className="px-4 py-3 text-gray-500 hidden md:table-cell">{b.address ?? '—'}</td>
                  {isAdmin && (
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          onClick={() => openEdit(b)}
                          className="p-1.5 text-gray-400 border border-gray-200 rounded-md hover:bg-gray-50 transition-colors"
                          aria-label={`Chỉnh sửa ${b.name}`}
                        >
                          <Pencil size={13} aria-hidden="true" />
                        </button>
                        <button
                          onClick={() => setDeleteTarget(b)}
                          className="p-1.5 text-red-400 border border-red-200 rounded-md hover:bg-red-50 transition-colors"
                          aria-label={`Xóa ${b.name}`}
                        >
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
      <Modal open={showCreateModal} onClose={() => setShowCreateModal(false)} title="Thêm chi nhánh">
        <BranchForm
          code={formCode} name={formName} address={formAddress}
          onCodeChange={setFormCode} onNameChange={setFormName} onAddressChange={setFormAddress}
          error={formError} isLoading={actionLoading}
          onSubmit={handleCreate} onCancel={() => setShowCreateModal(false)}
          submitLabel="Tạo chi nhánh"
        />
      </Modal>

      {/* Edit modal */}
      <Modal open={!!editTarget} onClose={() => setEditTarget(null)} title="Chỉnh sửa chi nhánh">
        <BranchForm
          code={formCode} name={formName} address={formAddress}
          onCodeChange={setFormCode} onNameChange={setFormName} onAddressChange={setFormAddress}
          error={formError} isLoading={actionLoading}
          onSubmit={handleUpdate} onCancel={() => setEditTarget(null)}
          submitLabel="Lưu thay đổi"
        />
      </Modal>

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => { setDeleteTarget(null); setDeleteError(''); }}
        onConfirm={handleDelete}
        title="Xóa chi nhánh"
        message={
          deleteError
            ? deleteError
            : `Bạn có chắc muốn xóa chi nhánh "${deleteTarget?.name}"? Hành động này không thể hoàn tác.`
        }
        confirmLabel="Xóa"
        variant={deleteError ? 'warning' : 'danger'}
        isLoading={actionLoading}
      />
    </div>
  );
}

interface BranchFormProps {
  code: string; name: string; address: string;
  onCodeChange: (v: string) => void;
  onNameChange: (v: string) => void;
  onAddressChange: (v: string) => void;
  error: string; isLoading: boolean;
  onSubmit: () => void; onCancel: () => void;
  submitLabel: string;
}

function BranchForm({
  code, name, address,
  onCodeChange, onNameChange, onAddressChange,
  error, isLoading, onSubmit, onCancel, submitLabel,
}: BranchFormProps) {
  return (
    <div className="space-y-4">
      {error && (
        <div role="alert" className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">{error}</div>
      )}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Mã chi nhánh <span className="text-red-500">*</span>
        </label>
        <input
          type="text"
          value={code}
          onChange={(e) => onCodeChange(e.target.value.toUpperCase())}
          placeholder="VD: HN, HCM, DN"
          disabled={isLoading}
          className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Tên chi nhánh <span className="text-red-500">*</span>
        </label>
        <input
          type="text"
          value={name}
          onChange={(e) => onNameChange(e.target.value)}
          placeholder="VD: Hà Nội, TP. Hồ Chí Minh"
          disabled={isLoading}
          className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Địa chỉ</label>
        <input
          type="text"
          value={address}
          onChange={(e) => onAddressChange(e.target.value)}
          placeholder="Địa chỉ văn phòng (không bắt buộc)"
          disabled={isLoading}
          className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>
      <div className="flex justify-end gap-3 pt-1">
        <button type="button" onClick={onCancel} disabled={isLoading}
          className="px-4 py-2 text-sm text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50">
          Hủy
        </button>
        <button type="button" onClick={onSubmit} disabled={isLoading}
          className="px-4 py-2 text-sm text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors disabled:opacity-50">
          {isLoading ? 'Đang lưu...' : submitLabel}
        </button>
      </div>
    </div>
  );
}
