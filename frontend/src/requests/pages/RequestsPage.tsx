import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { useRequests } from '../hooks/useRequests';
import { CreateRequestModal } from '../components/CreateRequestModal';
import { SkeletonLoader, EmptyState, Pagination, StatusBadge } from '@/shared/components';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import type { RequestFilter, RequestStatus } from '../types/request.types';

const STATUS_OPTIONS: { value: RequestStatus | ''; label: string }[] = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'PENDING', label: 'Chờ duyệt' },
  { value: 'APPROVED', label: 'Đã duyệt' },
  { value: 'REJECTED', label: 'Từ chối' },
  { value: 'FULFILLED', label: 'Hoàn thành' },
  { value: 'CANCELLED', label: 'Đã hủy' },
];

export default function RequestsPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isEmployee = user?.role === 'EMPLOYEE';

  const [page, setPage] = useState(0);
  // EMPLOYEE chỉ thấy request của mình — tự động điền employeeId vào filter
  const [filter, setFilter] = useState<RequestFilter>(
    isEmployee && user?.id ? { employeeId: user.id } : {}
  );
  const [showCreate, setShowCreate] = useState(false);

  const { requests, isLoading, totalPages, totalElements, refetch } = useRequests(filter, page);

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Yêu cầu</h1>
          <p className="text-sm text-gray-500 mt-0.5">{totalElements} yêu cầu</p>
        </div>
        {isEmployee && (
          <button
            onClick={() => setShowCreate(true)}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition-colors"
          >
            <Plus size={16} aria-hidden="true" />
            Tạo yêu cầu
          </button>
        )}
      </div>

      {/* Filter */}
      <div className="flex gap-3">
        <select
          value={filter.status ?? ''}
          onChange={(e) => {
            setFilter((f) => ({ ...f, status: (e.target.value as RequestStatus) || undefined }));
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
        ) : requests.length === 0 ? (
          <EmptyState message="Không có yêu cầu nào" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Thiết bị
                  </th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Loại
                  </th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Trạng thái
                  </th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider hidden md:table-cell">
                    Nhân viên
                  </th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider hidden lg:table-cell">
                    Ngày tạo
                  </th>
                  <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Thao tác
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {requests.map((req) => (
                  <tr key={req.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3">
                      <div>
                        <p className="font-medium text-gray-900">{req.assetName}</p>
                        <p className="text-xs text-gray-400 font-mono">{req.assetCode}</p>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={req.type} />
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={req.status} />
                    </td>
                    <td className="px-4 py-3 hidden md:table-cell text-gray-600">
                      {req.employeeName}
                    </td>
                    <td className="px-4 py-3 hidden lg:table-cell text-gray-500">
                      {formatDate(req.createdAt)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end">
                        <button
                          onClick={() => navigate(`/requests/${req.id}`)}
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
        <Pagination
          pagination={{ page, size: 20, totalElements, totalPages }}
          onPageChange={setPage}
        />
      )}

      <CreateRequestModal
        open={showCreate}
        onClose={() => setShowCreate(false)}
        onSuccess={(req) => {
          setShowCreate(false);
          refetch();
          navigate(`/requests/${req.id}`);
        }}
      />
    </div>
  );
}
