import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, ScanLine } from 'lucide-react';
import { useAuditSessions, useAuditActions } from '../hooks/useAudit';
import { SkeletonLoader, EmptyState, Pagination, StatusBadge, Modal, ConfirmDialog } from '@/shared/components';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import type { AuditSession, CreateAuditSessionRequest } from '../types/audit.types';

interface Branch { id: number; code: string; name: string; }

export default function AuditPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isItStaffOrAdmin = user?.role === 'ADMIN' || user?.role === 'IT_STAFF';

  const [page, setPage] = useState(0);
  const [showCreate, setShowCreate] = useState(false);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [branchId, setBranchId] = useState<number | ''>('');
  const [note, setNote] = useState('');
  const [createError, setCreateError] = useState('');
  const [completeTarget, setCompleteTarget] = useState<AuditSession | null>(null);

  const { sessions, isLoading, totalPages, totalElements, refetch } = useAuditSessions(page);
  const { createSession, completeSession, isLoading: actionLoading } = useAuditActions();

  useEffect(() => {
    apiClient
      .get<ApiResponse<Branch[]>>('/branches?page=0&size=100')
      .then((res) => {
        const list = res.data.data ?? [];
        setBranches(list);
        // IT_STAFF chỉ thấy chi nhánh của mình — pre-select
        if (user?.role === 'IT_STAFF' && user.branchId) {
          setBranchId(user.branchId);
        }
      })
      .catch(() => setBranches([]));
  }, [user]);

  const handleCreate = async () => {
    if (!branchId) { setCreateError('Vui lòng chọn chi nhánh'); return; }
    setCreateError('');
    try {
      const session = await createSession({
        branchId: Number(branchId),
        note: note.trim() || undefined,
      } as CreateAuditSessionRequest);
      setShowCreate(false);
      setNote('');
      refetch();
      navigate(`/audit/${session.id}`);
    } catch {
      setCreateError('Không thể tạo phiên kiểm kê, vui lòng thử lại');
    }
  };

  const handleComplete = async () => {
    if (!completeTarget) return;
    await completeSession(completeTarget.id);
    setCompleteTarget(null);
    refetch();
  };

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });

  const isExpired = (session: AuditSession) =>
    session.status === 'IN_PROGRESS' && new Date(session.expiresAt) < new Date();

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Kiểm kê</h1>
          <p className="text-sm text-gray-500 mt-0.5">{totalElements} phiên kiểm kê</p>
        </div>
        {isItStaffOrAdmin && (
          <button
            onClick={() => setShowCreate(true)}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition-colors"
          >
            <Plus size={16} aria-hidden="true" />
            Tạo phiên kiểm kê
          </button>
        )}
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="p-4"><SkeletonLoader rows={6} /></div>
        ) : sessions.length === 0 ? (
          <EmptyState message="Chưa có phiên kiểm kê nào" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Chi nhánh</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Trạng thái</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider hidden md:table-cell">Bắt đầu</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider hidden lg:table-cell">Hết hạn</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider hidden lg:table-cell">Người tạo</th>
                  <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {sessions.map((s) => (
                  <tr key={s.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3">
                      <div>
                        <p className="font-medium text-gray-900">{s.branchName}</p>
                        {s.note && <p className="text-xs text-gray-400 truncate max-w-xs">{s.note}</p>}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1.5">
                        <StatusBadge status={s.status} />
                        {isExpired(s) && (
                          <span className="text-xs text-red-500 font-medium">Quá hạn</span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3 hidden md:table-cell text-gray-600">{formatDate(s.startedAt)}</td>
                    <td className="px-4 py-3 hidden lg:table-cell text-gray-600">{formatDate(s.expiresAt)}</td>
                    <td className="px-4 py-3 hidden lg:table-cell text-gray-600">{s.createdByName}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          onClick={() => navigate(`/audit/${s.id}`)}
                          className="px-2.5 py-1.5 text-xs text-gray-600 border border-gray-200 rounded-md hover:bg-gray-50 transition-colors"
                        >
                          Chi tiết
                        </button>
                        {isItStaffOrAdmin && s.status === 'IN_PROGRESS' && (
                          <>
                            <button
                              onClick={() => navigate(`/audit/${s.id}/scan`)}
                              className="flex items-center gap-1 px-2.5 py-1.5 text-xs text-indigo-600 border border-indigo-200 rounded-md hover:bg-indigo-50 transition-colors"
                            >
                              <ScanLine size={12} aria-hidden="true" />
                              Quét
                            </button>
                            <button
                              onClick={() => setCompleteTarget(s)}
                              className="px-2.5 py-1.5 text-xs text-green-700 border border-green-200 rounded-md hover:bg-green-50 transition-colors"
                            >
                              Hoàn tất
                            </button>
                          </>
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
        <Pagination pagination={{ page, size: 20, totalElements, totalPages }} onPageChange={setPage} />
      )}

      {/* Modal tạo session */}
      <Modal open={showCreate} onClose={() => { setShowCreate(false); setCreateError(''); }} title="Tạo phiên kiểm kê mới">
        <div className="space-y-4">
          {createError && (
            <div role="alert" className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
              {createError}
            </div>
          )}
          <div>
            <label htmlFor="audit-branch" className="block text-sm font-medium text-gray-700 mb-1">
              Chi nhánh <span className="text-red-500">*</span>
            </label>
            <select
              id="audit-branch"
              value={branchId}
              onChange={(e) => setBranchId(e.target.value ? Number(e.target.value) : '')}
              disabled={user?.role === 'IT_STAFF' || actionLoading}
              className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="">— Chọn chi nhánh —</option>
              {branches.map((b) => (
                <option key={b.id} value={b.id}>{b.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="audit-note" className="block text-sm font-medium text-gray-700 mb-1">Ghi chú</label>
            <input
              id="audit-note"
              type="text"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="VD: Kiểm kê quý 3/2026"
              disabled={actionLoading}
              className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg text-sm text-amber-800">
            ⚠️ Phiên kiểm kê tự động hết hạn sau <strong>3 ngày</strong>.
          </div>
          <div className="flex justify-end gap-3">
            <button
              onClick={() => { setShowCreate(false); setCreateError(''); }}
              disabled={actionLoading}
              className="px-4 py-2 text-sm text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              Hủy
            </button>
            <button
              onClick={handleCreate}
              disabled={actionLoading}
              className="px-4 py-2 text-sm text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors disabled:opacity-50"
            >
              {actionLoading ? 'Đang tạo...' : 'Tạo phiên'}
            </button>
          </div>
        </div>
      </Modal>

      {/* Confirm hoàn tất */}
      <ConfirmDialog
        open={!!completeTarget}
        onClose={() => setCompleteTarget(null)}
        onConfirm={handleComplete}
        title="Hoàn tất phiên kiểm kê"
        message={`Hoàn tất phiên kiểm kê chi nhánh "${completeTarget?.branchName}"? Hệ thống sẽ tự động tạo báo cáo sai lệch cho các thiết bị chưa quét.`}
        confirmLabel="Hoàn tất"
        variant="default"
        isLoading={actionLoading}
      />
    </div>
  );
}
