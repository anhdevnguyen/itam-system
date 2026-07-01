import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, ScanLine, CheckCircle } from 'lucide-react';
import { useAuditSession, useDiscrepancies, useAuditActions } from '../hooks/useAudit';
import { SkeletonLoader, EmptyState, StatusBadge, ConfirmDialog } from '@/shared/components';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import type { Discrepancy, ResolutionAction } from '../types/audit.types';

export default function AuditDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const sessionId = id ? Number(id) : null;
  const { session, isLoading, error, refetch } = useAuditSession(sessionId);
  const { discrepancies, isLoading: discLoading, refetch: refetchDisc } = useDiscrepancies(sessionId);
  const { completeSession, resolveDiscrepancy, isLoading: actionLoading } = useAuditActions();

  const isItStaffOrAdmin = user?.role === 'ADMIN' || user?.role === 'IT_STAFF';
  const [showComplete, setShowComplete] = useState(false);
  const [resolveTarget, setResolveTarget] = useState<{ disc: Discrepancy; action: ResolutionAction } | null>(null);

  const handleComplete = async () => {
    await completeSession(sessionId!);
    setShowComplete(false);
    refetch();
  };

  const handleResolve = async () => {
    if (!resolveTarget) return;
    await resolveDiscrepancy(resolveTarget.disc.id, { action: resolveTarget.action });
    setResolveTarget(null);
    refetchDisc();
  };

  const formatDate = (dateStr: string | null) =>
    dateStr
      ? new Date(dateStr).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
      : '—';

  const isExpired = session?.status === 'IN_PROGRESS' && new Date(session.expiresAt) < new Date();

  if (isLoading) return <div className="p-6"><SkeletonLoader rows={8} /></div>;

  if (error || !session) {
    return (
      <EmptyState
        message={error || 'Không tìm thấy phiên kiểm kê'}
        action={
          <button onClick={() => navigate('/audit')} className="px-4 py-2 text-sm text-indigo-600 border border-indigo-200 rounded-lg hover:bg-indigo-50 transition-colors">
            Quay lại
          </button>
        }
      />
    );
  }

  const openDiscrepancies = discrepancies.filter((d) => d.status === 'OPEN');
  const resolvedDiscrepancies = discrepancies.filter((d) => d.status === 'RESOLVED');

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <button
            onClick={() => navigate('/audit')}
            className="mt-0.5 p-1.5 rounded-lg text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors"
            aria-label="Quay lại"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <div className="flex items-center gap-2 mb-1">
              <h1 className="text-2xl font-semibold text-gray-900">Kiểm kê #{session.id}</h1>
              <StatusBadge status={session.status} />
              {isExpired && <span className="text-xs text-red-500 font-medium bg-red-50 px-2 py-0.5 rounded-full">Quá hạn</span>}
            </div>
            <p className="text-sm text-gray-400">{session.branchName}</p>
          </div>
        </div>

        {isItStaffOrAdmin && session.status === 'IN_PROGRESS' && (
          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={() => navigate(`/audit/${session.id}/scan`)}
              className="flex items-center gap-1.5 px-3 py-2 text-sm text-indigo-700 border border-indigo-200 rounded-lg hover:bg-indigo-50 transition-colors"
            >
              <ScanLine size={15} aria-hidden="true" />
              Quét QR
            </button>
            <button
              onClick={() => setShowComplete(true)}
              className="flex items-center gap-1.5 px-3 py-2 text-sm text-green-700 border border-green-200 rounded-lg hover:bg-green-50 transition-colors"
            >
              <CheckCircle size={15} aria-hidden="true" />
              Hoàn tất
            </button>
          </div>
        )}
      </div>

      {/* Session info */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <dl className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <InfoRow label="Chi nhánh" value={session.branchName} />
          <InfoRow label="Người tạo" value={session.createdByName} />
          <InfoRow label="Bắt đầu" value={formatDate(session.startedAt)} />
          <InfoRow label="Hết hạn" value={formatDate(session.expiresAt)} />
          {session.completedAt && <InfoRow label="Hoàn tất" value={formatDate(session.completedAt)} />}
          {session.note && <InfoRow label="Ghi chú" value={session.note} />}
        </dl>

        {/* Progress bar nếu có scannedCount */}
        {session.scannedCount !== undefined && session.totalAssetCount !== undefined && session.totalAssetCount > 0 && (
          <div className="mt-4">
            <div className="flex justify-between text-xs text-gray-500 mb-1">
              <span>Tiến độ quét</span>
              <span>{session.scannedCount}/{session.totalAssetCount} thiết bị</span>
            </div>
            <div className="w-full bg-gray-100 rounded-full h-2">
              <div
                className="bg-indigo-600 h-2 rounded-full transition-all"
                style={{ width: `${Math.min((session.scannedCount / session.totalAssetCount) * 100, 100)}%` }}
              />
            </div>
          </div>
        )}
      </div>

      {/* Discrepancies */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="px-5 py-3 border-b border-gray-200 flex items-center justify-between">
          <h2 className="text-base font-semibold text-gray-900">Sai lệch</h2>
          <div className="flex gap-3 text-sm">
            <span className="text-amber-600 font-medium">{openDiscrepancies.length} chưa xử lý</span>
            <span className="text-green-600 font-medium">{resolvedDiscrepancies.length} đã xử lý</span>
          </div>
        </div>

        {discLoading ? (
          <div className="p-4"><SkeletonLoader rows={4} /></div>
        ) : discrepancies.length === 0 ? (
          <EmptyState message="Không có sai lệch nào" description="Tất cả thiết bị đã được quét đầy đủ" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Thiết bị</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Loại</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Trạng thái</th>
                  <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {discrepancies.map((disc) => (
                  <tr key={disc.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3">
                      <p className="font-medium text-gray-900">{disc.assetName}</p>
                      <p className="text-xs text-gray-400 font-mono">{disc.assetCode}</p>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={disc.type} />
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={disc.status} />
                    </td>
                    <td className="px-4 py-3">
                      {isItStaffOrAdmin && disc.status === 'OPEN' && (
                        <div className="flex justify-end gap-1">
                          {disc.type === 'MISSING' && (
                            <>
                              <button
                                onClick={() => setResolveTarget({ disc, action: 'FOUND' })}
                                className="px-2.5 py-1.5 text-xs text-green-700 border border-green-200 rounded-md hover:bg-green-50 transition-colors"
                              >
                                Đã tìm thấy
                              </button>
                              <button
                                onClick={() => setResolveTarget({ disc, action: 'CONFIRM_LOST' })}
                                className="px-2.5 py-1.5 text-xs text-red-600 border border-red-200 rounded-md hover:bg-red-50 transition-colors"
                              >
                                Xác nhận mất
                              </button>
                            </>
                          )}
                          {(disc.type === 'LOCATION_MISMATCH' || disc.type === 'UNEXPECTED_FOUND') && (
                            <button
                              onClick={() => setResolveTarget({ disc, action: 'FOUND' })}
                              className="px-2.5 py-1.5 text-xs text-indigo-700 border border-indigo-200 rounded-md hover:bg-indigo-50 transition-colors"
                            >
                              Đánh dấu đã xử lý
                            </button>
                          )}
                        </div>
                      )}
                      {disc.status === 'RESOLVED' && disc.resolvedByName && (
                        <span className="text-xs text-gray-400 text-right block">
                          {disc.resolvedByName}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Dialogs */}
      <ConfirmDialog
        open={showComplete}
        onClose={() => setShowComplete(false)}
        onConfirm={handleComplete}
        title="Hoàn tất kiểm kê"
        message="Hoàn tất phiên kiểm kê? Hệ thống tự động tạo báo cáo sai lệch cho các thiết bị chưa quét."
        confirmLabel="Hoàn tất"
        variant="default"
        isLoading={actionLoading}
      />

      <ConfirmDialog
        open={!!resolveTarget}
        onClose={() => setResolveTarget(null)}
        onConfirm={handleResolve}
        title={resolveTarget?.action === 'CONFIRM_LOST' ? 'Xác nhận mất thiết bị' : 'Xác nhận đã xử lý'}
        message={
          resolveTarget?.action === 'CONFIRM_LOST'
            ? `Xác nhận thiết bị "${resolveTarget?.disc.assetName}" bị mất? Trạng thái thiết bị sẽ chuyển sang LOST.`
            : resolveTarget?.disc.type === 'MISSING'
            ? `Xác nhận đã tìm thấy thiết bị "${resolveTarget?.disc.assetName}"? Trạng thái thiết bị giữ nguyên.`
            : `Xác nhận đã xử lý sai lệch cho thiết bị "${resolveTarget?.disc.assetName}"? Trạng thái thiết bị giữ nguyên.`
        }
        confirmLabel={resolveTarget?.action === 'CONFIRM_LOST' ? 'Xác nhận mất' : 'Xác nhận'}
        variant={resolveTarget?.action === 'CONFIRM_LOST' ? 'danger' : 'default'}
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
