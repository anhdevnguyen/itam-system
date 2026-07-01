import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, QrCode } from 'lucide-react';
import { useAssets, useAssetActions } from '../hooks/useAssets';
import { AssetForm } from '../components/AssetForm';
import { SkeletonLoader, EmptyState, Pagination, StatusBadge, Modal, ConfirmDialog } from '@/shared/components';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import type { AssetFilter, AssetStatus, Asset, CreateAssetRequest, UpdateAssetRequest } from '../types/asset.types';

const STATUS_OPTIONS: { value: AssetStatus | ''; label: string }[] = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'AVAILABLE', label: 'Sẵn sàng' },
  { value: 'ASSIGNED', label: 'Đã cấp phát' },
  { value: 'IN_MAINTENANCE', label: 'Đang bảo trì' },
  { value: 'BROKEN', label: 'Hỏng' },
  { value: 'DISPOSED', label: 'Thanh lý' },
  { value: 'LOST', label: 'Mất' },
];

interface Category { id: number; code: string; name: string; }
interface Branch { id: number; code: string; name: string; }

export default function AssetsPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isItStaffOrAdmin = user?.role === 'ADMIN' || user?.role === 'IT_STAFF';

  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<AssetFilter>({});
  const [search, setSearch] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [deleteError, setDeleteError] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<Asset | null>(null);

  // Dropdowns data — gọi API lần đầu
  const [categories, setCategories] = useState<Category[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [dropdownsLoaded, setDropdownsLoaded] = useState(false);

  const { assets, isLoading, totalPages, totalElements, refetch } = useAssets(filter, page);
  const { create, softDelete, isLoading: actionLoading } = useAssetActions();

  // Load dropdowns khi mở modal tạo mới
  const loadDropdowns = async () => {
    if (dropdownsLoaded) return;
    try {
      const [catRes, branchRes] = await Promise.all([
        apiClient.get<ApiResponse<Category[]>>('/categories?page=0&size=100'),
        apiClient.get<ApiResponse<Branch[]>>('/branches?page=0&size=100'),
      ]);
      setCategories(catRes.data.data ?? []);
      setBranches(branchRes.data.data ?? []);
      setDropdownsLoaded(true);
    } catch {
      // silent
    }
  };

  const handleOpenCreate = () => {
    loadDropdowns();
    setShowCreateModal(true);
  };

  const handleCreate = async (data: CreateAssetRequest | UpdateAssetRequest) => {
    return create(data as CreateAssetRequest);
  };

  const handleCreateSuccess = (saved: Asset) => {
    setShowCreateModal(false);
    refetch();
    navigate(`/assets/${saved.id}`);
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await softDelete(deleteTarget.id);
      setDeleteTarget(null);
      setDeleteError('');
      refetch();
    } catch {
      setDeleteError('Không thể xóa thiết bị. Thiết bị có thể đang có yêu cầu chờ xử lý.');
    }
  };

  const filtered = search
    ? assets.filter(
        (a) =>
          a.name.toLowerCase().includes(search.toLowerCase()) ||
          a.code.toLowerCase().includes(search.toLowerCase())
      )
    : assets;

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Thiết bị</h1>
          <p className="text-sm text-gray-500 mt-0.5">{totalElements} thiết bị</p>
        </div>
        {isItStaffOrAdmin && (
          <button
            onClick={handleOpenCreate}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition-colors"
          >
            <Plus size={16} aria-hidden="true" />
            Thêm thiết bị
          </button>
        )}
      </div>

      {/* Filter bar */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" aria-hidden="true" />
          <input
            type="search"
            placeholder="Tìm tên hoặc mã thiết bị..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <select
          value={filter.status ?? ''}
          onChange={(e) => {
            setFilter((f) => ({ ...f, status: (e.target.value as AssetStatus) || undefined }));
            setPage(0);
          }}
          className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
        >
          {STATUS_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="p-4">
            <SkeletonLoader rows={8} />
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState
            message="Không có thiết bị nào"
            description={search ? 'Thử tìm kiếm với từ khóa khác' : undefined}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Thiết bị
                  </th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider hidden md:table-cell">
                    Danh mục
                  </th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Trạng thái
                  </th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider hidden lg:table-cell">
                    Người giữ
                  </th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider hidden lg:table-cell">
                    Giá trị
                  </th>
                  <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Thao tác
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.map((asset) => (
                  <tr key={asset.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3">
                      <div>
                        <p className="font-medium text-gray-900">{asset.name}</p>
                        <p className="text-xs text-gray-400 font-mono">{asset.code}</p>
                      </div>
                    </td>
                    <td className="px-4 py-3 hidden md:table-cell text-gray-600">{asset.categoryName}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={asset.status} />
                    </td>
                    <td className="px-4 py-3 hidden lg:table-cell text-gray-600">
                      {asset.assignedToName ?? <span className="text-gray-300">—</span>}
                    </td>
                    <td className="px-4 py-3 hidden lg:table-cell text-gray-600">
                      {formatCurrency(asset.value)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          onClick={() => navigate(`/assets/${asset.id}`)}
                          className="px-2.5 py-1.5 text-xs text-gray-600 border border-gray-200 rounded-md hover:bg-gray-50 transition-colors"
                        >
                          Chi tiết
                        </button>
                        <button
                          onClick={() => navigate(`/assets/${asset.id}?tab=qr`)}
                          className="p-1.5 text-gray-400 border border-gray-200 rounded-md hover:bg-gray-50 transition-colors"
                          title="QR Code"
                          aria-label={`QR code cho ${asset.code}`}
                        >
                          <QrCode size={14} aria-hidden="true" />
                        </button>
                        {isItStaffOrAdmin && (
                          <button
                            onClick={() => setDeleteTarget(asset)}
                            className="px-2.5 py-1.5 text-xs text-red-600 border border-red-200 rounded-md hover:bg-red-50 transition-colors"
                          >
                            Xóa
                          </button>
                        )}
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

      {/* Modal tạo mới */}
      <Modal open={showCreateModal} onClose={() => setShowCreateModal(false)} title="Thêm thiết bị mới">
        <AssetForm
          categories={categories}
          branches={branches}
          onSubmit={handleCreate}
          onSuccess={handleCreateSuccess}
          onCancel={() => setShowCreateModal(false)}
          isLoading={actionLoading}
        />
      </Modal>

      {/* Confirm xóa */}
      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => { setDeleteTarget(null); setDeleteError(''); }}
        onConfirm={handleDelete}
        title="Xóa thiết bị"
        message={
          deleteError
            ? deleteError
            : `Bạn có chắc muốn xóa thiết bị "${deleteTarget?.name}" (${deleteTarget?.code})? Hành động này không thể hoàn tác.`
        }
        confirmLabel="Xóa"
        variant={deleteError ? 'warning' : 'danger'}
        isLoading={actionLoading}
      />
    </div>
  );
}
