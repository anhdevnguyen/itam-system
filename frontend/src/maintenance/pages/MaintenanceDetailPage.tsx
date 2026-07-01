import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Edit2, Trash2 } from 'lucide-react';
import { useMaintenanceRecord, useMaintenanceActions } from '../hooks/useMaintenance';
import { MaintenanceForm } from '../components/MaintenanceForm';
import { SkeletonLoader, EmptyState, StatusBadge, Modal, ConfirmDialog } from '@/shared/components';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import type { UpdateMaintenanceRequest } from '../types/maintenance.types';

interface AssetOption { id: number; code: string; name: string; }

export default function MaintenanceDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const recordId = id ? Number(id) : null;
  const { record, isLoading, error, refetch } = useMaintenanceRecord(recordId);
  const { update, softDelete, isLoading: actionLoading } = useMaintenanceActions();

  const isItStaffOrAdmin = user?.role === 'ADMIN' || user?.role === 'IT_STAFF';
  const [showEdit, setShowEdit] = useState(false);
  const [showDelete, setShowDelete] = useState(false);
  const [assets, setAssets] = useState<AssetOption[]>([]);

  useEffect(() => {
    apiClient
      .get<ApiResponse<AssetOption[]>>('/assets?page=0&size=200')
      .then((res) => setAssets(res.data.data ?? []))
      .catch(() => setAssets([]));
  }, []);

  const handleUpdate = async (data: UpdateMaintenanceRequest) => {
    return update(recordId!, data);
  };

  const handleDelete = async () => {
    await softDelete(recordId!);
    navigate('/maintenance', { replace: true });
  };

  const formatDate = (dateStr: string | null) =>
    dateStr
      ? new Date(dateStr).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
      : '—';

  if (isLoading) return <div className="p-6"><SkeletonLoader rows={8} /></div>;

  if (error || !record) {
    return (
      <EmptyState
        message={error || 'Không tìm thấy bản ghi bảo trì'}
        action={
          <button
            onClick={() => navigate('/maintenance')}
            className="px-4 py-2 text-sm text-indigo-600 border border-indigo-200 rounded-lg hover:bg-indigo-50 transition-colors"
          >
            Quay lại
          </button>
        }
      />
    );
  }

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <button
            onClick={() => navigate('/maintenance')}
            className="mt-0.5 p-1.5 rounded-lg text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors"
            aria-label="Quay lại"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <div className="flex items-center gap-2 mb-1">
              <h1 className="text-2xl font-semibold text-gray-900">Bảo trì #{record.id}</h1>
              <StatusBadge status={record.type} />
              <StatusBadge status={record.status} />
            </div>
            <p className="text-sm text-gray-400 font-mono">{record.assetCode} — {record.assetName}</p>
          </div>
        </div>

        {isItStaffOrAdmin && (
          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={() => setShowEdit(true)}
              className="flex items-center gap-1.5 px-3 py-2 text-sm text-gray-700 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <Edit2 size={15} aria-hidden="true" />
              Sửa
            </button>
            <button
              onClick={() => setShowDelete(true)}
              className="flex items-center gap-1.5 px-3 py-2 text-sm text-red-600 border border-red-200 rounded-lg hover:bg-red-50 transition-colors"
            >
              <Trash2 size={15} aria-hidden="true" />
              Xóa
            </button>
          </div>
        )}
      </div>

      {/* Detail card */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-4">
          <InfoRow label="Thiết bị" value={
            <button
              onClick={() => navigate(`/assets/${record.assetId}`)}
              className="text-indigo-600 hover:underline text-sm"
            >
              {record.assetCode} — {record.assetName}
            </button>
          } />
          <InfoRow label="Loại bảo trì" value={<StatusBadge status={record.type} />} />
          <InfoRow label="Trạng thái" value={<StatusBadge status={record.status} />} />
          <InfoRow label="Ngày lên lịch" value={formatDate(record.scheduledDate)} />
          <InfoRow label="Ngày hoàn thành" value={formatDate(record.completedDate)} />
          <InfoRow label="Ngày tạo" value={formatDate(record.createdAt)} />
          {record.description && (
            <div className="sm:col-span-2">
              <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-0.5">Mô tả</dt>
              <dd className="text-sm text-gray-700 bg-gray-50 px-3 py-2 rounded-lg">{record.description}</dd>
            </div>
          )}
        </dl>
      </div>

      {/* Thông báo đồng bộ trạng thái asset */}
      {(record.status === 'IN_PROGRESS' || record.status === 'COMPLETED') && (
        <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg text-sm text-blue-800">
          {record.status === 'IN_PROGRESS'
            ? '⚠️ Thiết bị đang ở trạng thái BẢO TRÌ — trạng thái asset đã được đồng bộ sang IN_MAINTENANCE.'
            : '✅ Bảo trì hoàn thành — trạng thái asset đã được khôi phục.'}
        </div>
      )}

      <Modal open={showEdit} onClose={() => setShowEdit(false)} title="Cập nhật bảo trì">
        <MaintenanceForm
          record={record}
          assets={assets}
          onSubmit={handleUpdate}
          onSuccess={() => { setShowEdit(false); refetch(); }}
          onCancel={() => setShowEdit(false)}
          isLoading={actionLoading}
        />
      </Modal>

      <ConfirmDialog
        open={showDelete}
        onClose={() => setShowDelete(false)}
        onConfirm={handleDelete}
        title="Xóa bản ghi bảo trì"
        message="Bạn có chắc muốn xóa bản ghi bảo trì này?"
        confirmLabel="Xóa"
        variant="danger"
        isLoading={actionLoading}
      />
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
