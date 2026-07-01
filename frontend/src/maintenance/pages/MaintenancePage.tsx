import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { useMaintenanceRecords, useMaintenanceActions } from '../hooks/useMaintenance';
import { MaintenanceForm } from '../components/MaintenanceForm';
import { SkeletonLoader, EmptyState, Pagination, StatusBadge, Modal } from '@/shared/components';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import type { MaintenanceFilter, MaintenanceStatus, MaintenanceType, CreateMaintenanceRequest } from '../types/maintenance.types';

const STATUS_OPTIONS: { value: MaintenanceStatus | ''; label: string }[] = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'SCHEDULED', label: 'Lên lịch' },
  { value: 'IN_PROGRESS', label: 'Đang thực hiện' },
  { value: 'COMPLETED', label: 'Hoàn thành' },
  { value: 'CANCELLED', label: 'Đã hủy' },
];

interface AssetOption { id: number; code: string; name: string; }

export default function MaintenancePage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isItStaffOrAdmin = user?.role === 'ADMIN' || user?.role === 'IT_STAFF';

  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<MaintenanceFilter>({});
  const [showCreate, setShowCreate] = useState(false);
  const [assets, setAssets] = useState<AssetOption[]>([]);

  const { records, isLoading, totalPages, totalElements, refetch } = useMaintenanceRecords(filter, page);
  const { create, isLoading: actionLoading } = useMaintenanceActions();

  // Load danh sách assets cho dropdown
  useEffect(() => {
    apiClient
      .get<ApiResponse<AssetOption[]>>('/assets?page=0&size=200')
      .then((res) => setAssets(res.data.data ?? []))
      .catch(() => setAssets([]));
  }, []);

  const handleCreate = async (data: CreateMaintenanceRequest) => {
    return create(data);
  };

  const formatDate = (dateStr: string | null) =>
    dateStr
      ? new Date(dateStr).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
      : '—';

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Bảo trì</h1>
          <p className="text-sm text-gray-500 mt-0.5">{totalElements} bản ghi</p>
        </div>
        {isItStaffOrAdmin && (
          <button
            onClick={() => setShowCreate(true)}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition-colors"
          >
            <Plus size={16} aria-hidden="true" />
            Tạo bảo trì
          </button>
        )}
      </div>

      {/* Filter */}
      <div className="flex gap-3">
        <select
          value={filter.status ?? ''}
          onChange={(e) => {
            setFilter((f) => ({ ...f, status: (e.target.value as MaintenanceStatus) || undefined }));
            setPage(0);
          }}
          className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
        >
          {STATUS_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="p-4"><SkeletonLoader rows={8} /></div>
        ) : records.length === 0 ? (
          <EmptyState message="Không có bản ghi bảo trì nào" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Thiết bị</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Loại</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Trạng thái</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider hidden md:table-cell">Ngày lên lịch</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider hidden lg:table-cell">Ngày hoàn thành</th>
                  <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {records.map((rec) => (
                  <tr key={rec.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3">
                      <div>
                        <p className="font-medium text-gray-900">{rec.assetName}</p>
                        <p className="text-xs text-gray-400 font-mono">{rec.assetCode}</p>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={rec.type} />
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={rec.status} />
                    </td>
                    <td className="px-4 py-3 hidden md:table-cell text-gray-600">{formatDate(rec.scheduledDate)}</td>
                    <td className="px-4 py-3 hidden lg:table-cell text-gray-600">{formatDate(rec.completedDate)}</td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end">
                        <button
                          onClick={() => navigate(`/maintenance/${rec.id}`)}
                          className="px-2.5 py-1.5 text-xs text-gray-600 border border-gray-200 rounded-md hover:bg-gray-50 transition-colors"
                        >
                          Chi tiết
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
        <Pagination pagination={{ page, size: 20, totalElements, totalPages }} onPageChange={setPage} />
      )}

      <Modal open={showCreate} onClose={() => setShowCreate(false)} title="Tạo bản ghi bảo trì">
        <MaintenanceForm
          assets={assets}
          onSubmit={handleCreate}
          onSuccess={(saved) => { setShowCreate(false); refetch(); navigate(`/maintenance/${saved.id}`); }}
          onCancel={() => setShowCreate(false)}
          isLoading={actionLoading}
        />
      </Modal>
    </div>
  );
}
