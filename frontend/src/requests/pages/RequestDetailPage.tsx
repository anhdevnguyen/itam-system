import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, CheckCircle, XCircle, PlayCircle, Ban } from 'lucide-react';
import { useRequest, useRequestActions } from '../hooks/useRequests';
import { RejectRequestModal } from '../components/RejectRequestModal';
import { SkeletonLoader, EmptyState, StatusBadge, ConfirmDialog } from '@/shared/components';
import { useAuth } from '@/auth/hooks/useAuth.tsx';

export default function RequestDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const requestId = id ? Number(id) : null;
  const { request, isLoading, error, refetch } = useRequest(requestId);
  const { approve, reject, fulfill, cancel, isLoading: actionLoading } = useRequestActions();

  const [showReject, setShowReject] = useState(false);
  const [showApproveDialog, setShowApproveDialog] = useState(false);
  const [showFulfillDialog, setShowFulfillDialog] = useState(false);
  const [showCancelDialog, setShowCancelDialog] = useState(false);

  const role = user?.role;
  const isManager = role === 'MANAGER';
  const isItStaffOrAdmin = role === 'IT_STAFF' || role === 'ADMIN';
  const isEmployee = role === 'EMPLOYEE';
  const isOwner = request?.employeeId === user?.id;

  const handleApprove = async () => {
    await approve(requestId!);
    setShowApproveDialog(false);
    refetch();
  };

  const handleReject = async (reason: string) => {
    await reject(requestId!, { rejectionReason: reason });
    setShowReject(false);
    refetch();
  };

  const handleFulfill = async () => {
    await fulfill(requestId!);
    setShowFulfillDialog(false);
    refetch();
  };

  const handleCancel = async () => {
    await cancel(requestId!);
    setShowCancelDialog(false);
    refetch();
  };

  const formatDate = (dateStr: string | null) =>
    dateStr
      ? new Date(dateStr).toLocaleDateString('vi-VN', {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
        })
      : '—';

  if (isLoading) {
    return (
      <div className="p-6">
        <SkeletonLoader rows={8} />
      </div>
    );
  }

  if (error || !request) {
    return (
      <EmptyState
        message={error || 'Không tìm thấy yêu cầu'}
        action={
          <button
            onClick={() => navigate('/requests')}
            className="px-4 py-2 text-sm text-indigo-600 border border-indigo-200 rounded-lg hover:bg-indigo-50 transition-colors"
          >
            Quay lại danh sách
          </button>
        }
      />
    );
  }

  const isPending = request.status === 'PENDING';
  const isApproved = request.status === 'APPROVED';

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <button
            onClick={() => navigate('/requests')}
            className="mt-0.5 p-1.5 rounded-lg text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors"
            aria-label="Quay lại danh sách"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <div className="flex items-center gap-2 mb-1">
              <h1 className="text-2xl font-semibold text-gray-900">
                Yêu cầu #{request.id}
              </h1>
              <StatusBadge status={request.type} />
              <StatusBadge status={request.status} />
            </div>
            <p className="text-sm text-gray-400">{formatDate(request.createdAt)}</p>
          </div>
        </div>

        {/* Action buttons theo role và trạng thái */}
        <div className="flex items-center gap-2 shrink-0 flex-wrap justify-end">
          {/* Manager: duyệt / từ chối khi PENDING */}
          {isManager && isPending && (
            <>
              <button
                onClick={() => setShowApproveDialog(true)}
                className="flex items-center gap-1.5 px-3 py-2 text-sm text-green-700 border border-green-200 rounded-lg hover:bg-green-50 transition-colors"
              >
                <CheckCircle size={15} aria-hidden="true" />
                Duyệt
              </button>
              <button
                onClick={() => setShowReject(true)}
                className="flex items-center gap-1.5 px-3 py-2 text-sm text-red-600 border border-red-200 rounded-lg hover:bg-red-50 transition-colors"
              >
                <XCircle size={15} aria-hidden="true" />
                Từ chối
              </button>
            </>
          )}

          {/* IT Staff / Admin: fulfill khi APPROVED */}
          {isItStaffOrAdmin && isApproved && (
            <button
              onClick={() => setShowFulfillDialog(true)}
              className="flex items-center gap-1.5 px-3 py-2 text-sm text-blue-700 border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors"
            >
              <PlayCircle size={15} aria-hidden="true" />
              Hoàn tất
            </button>
          )}

          {/* Employee: hủy khi PENDING và là chủ request */}
          {isEmployee && isOwner && isPending && (
            <button
              onClick={() => setShowCancelDialog(true)}
              className="flex items-center gap-1.5 px-3 py-2 text-sm text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <Ban size={15} aria-hidden="true" />
              Hủy yêu cầu
            </button>
          )}
        </div>
      </div>

      {/* Detail card */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-4">
          <InfoRow label="Thiết bị" value={
            <span>
              <span className="font-mono text-xs text-gray-400 mr-1">{request.assetCode}</span>
              {request.assetName}
            </span>
          } />
          <InfoRow label="Nhân viên yêu cầu" value={request.employeeName} />
          <InfoRow label="Loại yêu cầu" value={<StatusBadge status={request.type} />} />
          <InfoRow label="Trạng thái" value={<StatusBadge status={request.status} />} />
          <InfoRow label="Ngày tạo" value={formatDate(request.createdAt)} />
          <InfoRow
            label="Người duyệt"
            value={request.approvedByName ? `${request.approvedByName} (${formatDate(request.approvedAt)})` : '—'}
          />
          <InfoRow
            label="Người hoàn tất"
            value={request.fulfilledByName ? `${request.fulfilledByName} (${formatDate(request.fulfilledAt)})` : '—'}
          />
          {request.note && <InfoRow label="Ghi chú" value={request.note} />}
          {request.rejectionReason && (
            <div className="sm:col-span-2">
              <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-0.5">Lý do từ chối</dt>
              <dd className="text-sm text-red-700 bg-red-50 px-3 py-2 rounded-lg">{request.rejectionReason}</dd>
            </div>
          )}
        </dl>
      </div>

      {/* Dialogs */}
      <ConfirmDialog
        open={showApproveDialog}
        onClose={() => setShowApproveDialog(false)}
        onConfirm={handleApprove}
        title="Duyệt yêu cầu"
        message={`Duyệt yêu cầu ${request.type === 'ASSIGN' ? 'cấp phát' : 'trả'} thiết bị "${request.assetName}" cho ${request.employeeName}?`}
        confirmLabel="Duyệt"
        variant="default"
        isLoading={actionLoading}
      />

      <ConfirmDialog
        open={showFulfillDialog}
        onClose={() => setShowFulfillDialog(false)}
        onConfirm={handleFulfill}
        title="Hoàn tất yêu cầu"
        message={`Xác nhận hoàn tất ${request.type === 'ASSIGN' ? 'cấp phát' : 'thu hồi'} thiết bị "${request.assetName}"?`}
        confirmLabel="Hoàn tất"
        variant="default"
        isLoading={actionLoading}
      />

      <ConfirmDialog
        open={showCancelDialog}
        onClose={() => setShowCancelDialog(false)}
        onConfirm={handleCancel}
        title="Hủy yêu cầu"
        message="Bạn có chắc muốn hủy yêu cầu này?"
        confirmLabel="Hủy yêu cầu"
        variant="warning"
        isLoading={actionLoading}
      />

      <RejectRequestModal
        open={showReject}
        onClose={() => setShowReject(false)}
        onConfirm={handleReject}
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
