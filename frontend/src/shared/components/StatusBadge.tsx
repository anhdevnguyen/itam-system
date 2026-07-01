import { cn } from '@/lib/utils';

/** Mapping enum value → Tailwind color classes (bg + text) */
const STATUS_COLOR_MAP: Record<string, string> = {
  // AssetStatus
  AVAILABLE: 'bg-green-100 text-green-700',
  ASSIGNED: 'bg-blue-100 text-blue-700',
  IN_MAINTENANCE: 'bg-amber-100 text-amber-700',
  BROKEN: 'bg-red-100 text-red-700',
  DISPOSED: 'bg-red-50 text-red-400',
  LOST: 'bg-red-100 text-red-700',
  // RequestStatus
  PENDING: 'bg-amber-100 text-amber-700',
  APPROVED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
  FULFILLED: 'bg-blue-100 text-blue-700',
  CANCELLED: 'bg-gray-100 text-gray-600',
  // MaintenanceStatus
  SCHEDULED: 'bg-amber-100 text-amber-700',
  IN_PROGRESS: 'bg-blue-100 text-blue-700',
  COMPLETED: 'bg-green-100 text-green-700',
  // MaintenanceType
  WARRANTY: 'bg-purple-100 text-purple-700',
  REPAIR: 'bg-orange-100 text-orange-700',
  PERIODIC: 'bg-teal-100 text-teal-700',
  // DiscrepancyStatus
  OPEN: 'bg-amber-100 text-amber-700',
  RESOLVED: 'bg-green-100 text-green-700',
  // DiscrepancyType
  LOCATION_MISMATCH: 'bg-orange-100 text-orange-700',
  MISSING: 'bg-red-100 text-red-700',
  UNEXPECTED_FOUND: 'bg-purple-100 text-purple-700',
  // AuditSessionStatus (IN_PROGRESS / COMPLETED handled above)
  // RequestType
  ASSIGN: 'bg-blue-100 text-blue-700',
  RETURN: 'bg-gray-100 text-gray-700',
  // RoleCode
  ADMIN: 'bg-indigo-100 text-indigo-700',
  IT_STAFF: 'bg-cyan-100 text-cyan-700',
  MANAGER: 'bg-violet-100 text-violet-700',
  EMPLOYEE: 'bg-gray-100 text-gray-600',
};

/** Mapping enum value → label Tiếng Việt */
export const STATUS_LABEL_MAP: Record<string, string> = {
  // AssetStatus
  AVAILABLE: 'Sẵn sàng',
  ASSIGNED: 'Đã cấp phát',
  IN_MAINTENANCE: 'Đang bảo trì',
  BROKEN: 'Hỏng',
  DISPOSED: 'Đã thanh lý',
  LOST: 'Mất',
  // RequestStatus
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
  FULFILLED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
  // MaintenanceStatus
  SCHEDULED: 'Lên lịch',
  IN_PROGRESS: 'Đang thực hiện',
  COMPLETED: 'Hoàn thành',
  // MaintenanceType
  WARRANTY: 'Bảo hành',
  REPAIR: 'Sửa chữa',
  PERIODIC: 'Định kỳ',
  // DiscrepancyStatus
  OPEN: 'Chưa xử lý',
  RESOLVED: 'Đã xử lý',
  // DiscrepancyType
  LOCATION_MISMATCH: 'Sai vị trí',
  MISSING: 'Mất tích',
  UNEXPECTED_FOUND: 'Phát sinh ngoài dự kiến',
  // AuditSessionStatus
  // IN_PROGRESS already handled above
  // RequestType
  ASSIGN: 'Cấp phát',
  RETURN: 'Thu hồi',
  // RoleCode (dùng trong StatusBadge ở EmployeeDetailPage, ProfilePage, v.v.)
  ADMIN: 'Quản trị viên',
  IT_STAFF: 'Nhân viên IT',
  MANAGER: 'Trưởng phòng',
  EMPLOYEE: 'Nhân viên',
};

interface StatusBadgeProps {
  status: string;
  /** Override label — nếu không truyền thì dùng STATUS_LABEL_MAP */
  label?: string;
  className?: string;
}

export function StatusBadge({ status, label, className }: StatusBadgeProps) {
  const colorClass = STATUS_COLOR_MAP[status] ?? 'bg-gray-100 text-gray-600';
  const displayLabel = label ?? STATUS_LABEL_MAP[status] ?? status;
  return (
    <span className={cn('inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium', colorClass, className)}>
      {displayLabel}
    </span>
  );
}
