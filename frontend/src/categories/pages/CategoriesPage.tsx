import { useState, useCallback, useEffect } from 'react';
import { Plus, Search, Pencil, Trash2 } from 'lucide-react';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import { SkeletonLoader, EmptyState, Modal, ConfirmDialog } from '@/shared/components';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';

interface Category {
  id: number;
  code: string;
  name: string;
  createdAt: string;
}

/**
 * CategoriesPage — Quản lý danh mục thiết bị.
 * Chỉ ADMIN và IT_STAFF mới có quyền tạo/sửa/xóa.
 */
export default function CategoriesPage() {
  const { user } = useAuth();
  const isItStaffOrAdmin = user?.role === 'ADMIN' || user?.role === 'IT_STAFF';

  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState('');

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editTarget, setEditTarget] = useState<Category | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Category | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [formError, setFormError] = useState('');

  const [formCode, setFormCode] = useState('');
  const [formName, setFormName] = useState('');

  const fetchCategories = useCallback(async () => {
    setIsLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<Category[]>>('/categories?page=0&size=200');
      setCategories(res.data.data ?? []);
    } catch {
      setCategories([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => { fetchCategories(); }, [fetchCategories]);

  const openCreate = () => {
    setFormCode('');
    setFormName('');
    setFormError('');
    setShowCreateModal(true);
  };

  const openEdit = (cat: Category) => {
    setFormCode(cat.code);
    setFormName(cat.name);
    setFormError('');
    setEditTarget(cat);
  };

  const handleCreate = async () => {
    if (!formCode.trim() || !formName.trim()) {
      setFormError('Mã và tên danh mục là bắt buộc');
      return;
    }
    setFormError('');
    setActionLoading(true);
    try {
      await apiClient.post('/categories', { code: formCode.trim().toUpperCase(), name: formName.trim() });
      setShowCreateModal(false);
      fetchCategories();
    } catch {
      setFormError('Không thể tạo danh mục. Mã có thể đã tồn tại.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleUpdate = async () => {
    if (!editTarget) return;
    if (!formCode.trim() || !formName.trim()) {
      setFormError('Mã và tên danh mục là bắt buộc');
      return;
    }
    setFormError('');
    setActionLoading(true);
    try {
      await apiClient.put(`/categories/${editTarget.id}`, { code: formCode.trim().toUpperCase(), name: formName.trim() });
      setEditTarget(null);
      fetchCategories();
    } catch {
      setFormError('Không thể cập nhật danh mục');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setActionLoading(true);
    try {
      await apiClient.delete(`/categories/${deleteTarget.id}`);
      setDeleteTarget(null);
      fetchCategories();
    } catch {
      // silent
    } finally {
      setActionLoading(false);
    }
  };

  const filtered = search
    ? categories.filter(
        (c) =>
          c.name.toLowerCase().includes(search.toLowerCase()) ||
          c.code.toLowerCase().includes(search.toLowerCase()),
      )
    : categories;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Danh mục thiết bị</h1>
          <p className="text-sm text-gray-500 mt-0.5">{categories.length} danh mục</p>
        </div>
        {isItStaffOrAdmin && (
          <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition-colors">
            <Plus size={16} aria-hidden="true" />
            Thêm danh mục
          </button>
        )}
      </div>

      <div className="relative max-w-sm">
        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" aria-hidden="true" />
        <input
          type="search"
          placeholder="Tìm tên hoặc mã danh mục..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-9 pr-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="p-4"><SkeletonLoader rows={5} /></div>
        ) : filtered.length === 0 ? (
          <EmptyState message="Không có danh mục nào" description={search ? 'Thử tìm kiếm với từ khóa khác' : undefined} />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50">
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Mã</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Tên danh mục</th>
                {isItStaffOrAdmin && <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Thao tác</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filtered.map((c) => (
                <tr key={c.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 font-mono text-xs text-gray-600">{c.code}</td>
                  <td className="px-4 py-3 font-medium text-gray-900">{c.name}</td>
                  {isItStaffOrAdmin && (
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => openEdit(c)} className="p-1.5 text-gray-400 border border-gray-200 rounded-md hover:bg-gray-50 transition-colors" aria-label={`Chỉnh sửa ${c.name}`}>
                          <Pencil size={13} aria-hidden="true" />
                        </button>
                        <button onClick={() => setDeleteTarget(c)} className="p-1.5 text-red-400 border border-red-200 rounded-md hover:bg-red-50 transition-colors" aria-label={`Xóa ${c.name}`}>
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

      {/* Create / Edit shared form */}
      {[showCreateModal, !!editTarget].includes(true) && (
        <>
          <Modal open={showCreateModal} onClose={() => setShowCreateModal(false)} title="Thêm danh mục">
            <CategoryForm code={formCode} name={formName} onCodeChange={setFormCode} onNameChange={setFormName} error={formError} isLoading={actionLoading} onSubmit={handleCreate} onCancel={() => setShowCreateModal(false)} submitLabel="Tạo danh mục" />
          </Modal>
          <Modal open={!!editTarget} onClose={() => setEditTarget(null)} title="Chỉnh sửa danh mục">
            <CategoryForm code={formCode} name={formName} onCodeChange={setFormCode} onNameChange={setFormName} error={formError} isLoading={actionLoading} onSubmit={handleUpdate} onCancel={() => setEditTarget(null)} submitLabel="Lưu thay đổi" />
          </Modal>
        </>
      )}

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Xóa danh mục"
        message={`Bạn có chắc muốn xóa danh mục "${deleteTarget?.name}"?`}
        confirmLabel="Xóa"
        variant="danger"
        isLoading={actionLoading}
      />
    </div>
  );
}

interface CategoryFormProps {
  code: string; name: string;
  onCodeChange: (v: string) => void;
  onNameChange: (v: string) => void;
  error: string; isLoading: boolean;
  onSubmit: () => void; onCancel: () => void;
  submitLabel: string;
}

function CategoryForm({ code, name, onCodeChange, onNameChange, error, isLoading, onSubmit, onCancel, submitLabel }: CategoryFormProps) {
  return (
    <div className="space-y-4">
      {error && <div role="alert" className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">{error}</div>}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Mã danh mục <span className="text-red-500">*</span></label>
        <input type="text" value={code} onChange={(e) => onCodeChange(e.target.value.toUpperCase())} placeholder="VD: LAPTOP, MONITOR" disabled={isLoading} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500" />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Tên danh mục <span className="text-red-500">*</span></label>
        <input type="text" value={name} onChange={(e) => onNameChange(e.target.value)} placeholder="VD: Laptop, Màn hình" disabled={isLoading} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
      </div>
      <div className="flex justify-end gap-3 pt-1">
        <button type="button" onClick={onCancel} disabled={isLoading} className="px-4 py-2 text-sm text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-50">Hủy</button>
        <button type="button" onClick={onSubmit} disabled={isLoading} className="px-4 py-2 text-sm text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg disabled:opacity-50">{isLoading ? 'Đang lưu...' : submitLabel}</button>
      </div>
    </div>
  );
}
