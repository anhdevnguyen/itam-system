import { useState } from 'react';
import { CheckCheck } from 'lucide-react';
import { useNotifications } from '../hooks/useNotifications';
import { SkeletonLoader, EmptyState, Pagination } from '@/shared/components';
import apiClient from '@/lib/apiClient';

/**
 * NotificationsPage — Trang xem toàn bộ thông báo.
 */
export default function NotificationsPage() {
  const [page, setPage] = useState(0);
  const { notifications, isLoading, totalPages, totalElements, markRead, refetch } = useNotifications(page);
  const [markingAll, setMarkingAll] = useState(false);

  const handleMarkAllRead = async () => {
    setMarkingAll(true);
    try {
      await apiClient.post('/notifications/read-all');
      refetch();
    } catch {
      // silent
    } finally {
      setMarkingAll(false);
    }
  };

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Thông báo</h1>
          <p className="text-sm text-gray-500 mt-0.5">{totalElements} thông báo</p>
        </div>
        {unreadCount > 0 && (
          <button
            onClick={handleMarkAllRead}
            disabled={markingAll}
            className="flex items-center gap-2 px-3 py-2 text-sm text-indigo-600 border border-indigo-200 rounded-lg hover:bg-indigo-50 transition-colors disabled:opacity-50"
          >
            <CheckCheck size={15} aria-hidden="true" />
            Đánh dấu tất cả đã đọc
          </button>
        )}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="p-4"><SkeletonLoader rows={6} /></div>
        ) : notifications.length === 0 ? (
          <EmptyState
            message="Không có thông báo nào"
            description="Bạn đã đọc hết tất cả thông báo"
          />
        ) : (
          <ul className="divide-y divide-gray-100">
            {notifications.map((n) => (
              <li
                key={n.id}
                className={`flex items-start gap-3 px-5 py-4 transition-colors ${
                  !n.isRead ? 'bg-indigo-50/50 hover:bg-indigo-50' : 'hover:bg-gray-50'
                }`}
              >
                <div className={`mt-1 w-2 h-2 rounded-full shrink-0 ${!n.isRead ? 'bg-indigo-500' : 'bg-transparent'}`} aria-hidden="true" />
                <div className="flex-1 min-w-0">
                  <p className={`text-sm ${!n.isRead ? 'font-medium text-gray-900' : 'text-gray-600'}`}>
                    {n.message}
                  </p>
                  <p className="text-xs text-gray-400 mt-0.5">{formatDate(n.createdAt)}</p>
                </div>
                {!n.isRead && (
                  <button
                    onClick={() => markRead(n.id)}
                    className="shrink-0 text-xs text-indigo-600 hover:text-indigo-800 transition-colors"
                  >
                    Đánh dấu đọc
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      {totalPages > 1 && (
        <Pagination
          pagination={{ page, size: 20, totalElements, totalPages }}
          onPageChange={setPage}
        />
      )}
    </div>
  );
}
