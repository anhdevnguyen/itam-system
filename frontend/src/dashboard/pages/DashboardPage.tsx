import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Monitor, ClipboardList, Wrench, ScanLine } from 'lucide-react';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import { SkeletonLoader } from '@/shared/components';

interface DashboardStats {
  totalAssets: number;
  pendingRequests: number;
  inProgressMaintenance: number;
  openAuditSessions: number;
}

/**
 * DashboardPage — Trang tổng quan hệ thống.
 * Gọi 4 API song song để lấy số liệu tóm tắt:
 * - Tổng thiết bị (GET /assets?page=0&size=1)
 * - Yêu cầu chờ duyệt (GET /requests?status=PENDING&page=0&size=1)
 * - Bảo trì đang thực hiện (GET /maintenance?status=IN_PROGRESS&page=0&size=1)
 * - Phiên kiểm kê đang mở (GET /audits?page=0&size=1 → lọc IN_PROGRESS)
 */
export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      setIsLoading(true);
      try {
        const [assetsRes, requestsRes, maintenanceRes, auditsRes] = await Promise.all([
          apiClient.get<ApiResponse<unknown[]>>('/assets?page=0&size=1'),
          apiClient.get<ApiResponse<unknown[]>>('/requests?status=PENDING&page=0&size=1'),
          apiClient.get<ApiResponse<unknown[]>>('/maintenance?status=IN_PROGRESS&page=0&size=1'),
          // Kiểm kê: lấy danh sách, đếm các phiên IN_PROGRESS từ meta
          apiClient.get<ApiResponse<{ status: string }[]>>('/audits?page=0&size=100'),
        ]);

        const totalAssets = assetsRes.data.meta.pagination?.totalElements ?? 0;
        const pendingRequests = requestsRes.data.meta.pagination?.totalElements ?? 0;
        const inProgressMaintenance = maintenanceRes.data.meta.pagination?.totalElements ?? 0;

        // Đếm phiên audit IN_PROGRESS trong kết quả trả về
        const auditData = auditsRes.data.data ?? [];
        const openAuditSessions = auditData.filter((s) => s.status === 'IN_PROGRESS').length;

        setStats({ totalAssets, pendingRequests, inProgressMaintenance, openAuditSessions });
      } catch {
        // Nếu API lỗi, dùng giá trị 0 để không crash trang
        setStats({ totalAssets: 0, pendingRequests: 0, inProgressMaintenance: 0, openAuditSessions: 0 });
      } finally {
        setIsLoading(false);
      }
    };
    load();
  }, []);

  const cards = [
    {
      label: 'Tổng thiết bị',
      value: stats?.totalAssets,
      icon: Monitor,
      color: 'text-blue-600 bg-blue-50',
      to: '/assets',
    },
    {
      label: 'Yêu cầu chờ duyệt',
      value: stats?.pendingRequests,
      icon: ClipboardList,
      color: 'text-amber-600 bg-amber-50',
      to: '/requests?status=PENDING',
      highlight: (stats?.pendingRequests ?? 0) > 0,
    },
    {
      label: 'Đang bảo trì',
      value: stats?.inProgressMaintenance,
      icon: Wrench,
      color: 'text-orange-600 bg-orange-50',
      to: '/maintenance?status=IN_PROGRESS',
    },
    {
      label: 'Kiểm kê đang mở',
      value: stats?.openAuditSessions,
      icon: ScanLine,
      color: 'text-indigo-600 bg-indigo-50',
      to: '/audit',
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-semibold text-gray-900">Tổng quan</h1>
        <p className="text-sm text-gray-500 mt-0.5">
          Xin chào, <span className="font-medium text-gray-700">{user?.fullName}</span>
        </p>
      </div>

      {/* Stat cards */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="bg-white rounded-xl border border-gray-200 p-5">
              <SkeletonLoader rows={2} />
            </div>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {cards.map((card) => (
            <button
              key={card.label}
              onClick={() => navigate(card.to)}
              className={`bg-white rounded-xl border text-left p-5 hover:shadow-sm transition-shadow ${
                card.highlight ? 'border-amber-300 bg-amber-50/30' : 'border-gray-200'
              }`}
            >
              <div className="flex items-start justify-between gap-3 mb-3">
                <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">{card.label}</p>
                <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${card.color}`}>
                  <card.icon size={16} aria-hidden="true" />
                </div>
              </div>
              <p className={`text-3xl font-semibold ${card.highlight ? 'text-amber-700' : 'text-gray-900'}`}>
                {card.value ?? '—'}
              </p>
            </button>
          ))}
        </div>
      )}

      {/* Quick links */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <h2 className="text-sm font-semibold text-gray-700 mb-3">Truy cập nhanh</h2>
        <div className="flex flex-wrap gap-2">
          {[
            { label: 'Danh sách thiết bị', to: '/assets' },
            { label: 'Yêu cầu của tôi', to: '/requests' },
            { label: 'Lịch bảo trì', to: '/maintenance' },
            { label: 'Phiên kiểm kê', to: '/audit' },
          ].map((link) => (
            <button
              key={link.to}
              onClick={() => navigate(link.to)}
              className="px-3 py-1.5 text-sm text-indigo-600 border border-indigo-200 rounded-lg hover:bg-indigo-50 transition-colors"
            >
              {link.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
