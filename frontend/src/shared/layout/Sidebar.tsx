import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Monitor,
  ClipboardList,
  Wrench,
  ScanLine,
  Users,
  Building2,
  FolderKanban,
  X,
  Package,
  Bell,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import type { RoleCode } from '@/auth/types/auth.types';

interface NavItem {
  label: string;
  to: string;
  icon: React.ElementType;
  roles?: RoleCode[]; // undefined = mọi role đều thấy
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', to: '/dashboard', icon: LayoutDashboard },
  { label: 'Thiết bị', to: '/assets', icon: Monitor, roles: ['ADMIN', 'IT_STAFF', 'MANAGER', 'EMPLOYEE'] },
  { label: 'Yêu cầu', to: '/requests', icon: ClipboardList },
  { label: 'Bảo trì', to: '/maintenance', icon: Wrench, roles: ['ADMIN', 'IT_STAFF', 'MANAGER'] },
  { label: 'Kiểm kê', to: '/audit', icon: ScanLine, roles: ['ADMIN', 'IT_STAFF'] },
  { label: 'Nhân viên', to: '/employees', icon: Users, roles: ['ADMIN', 'IT_STAFF'] },
  { label: 'Thông báo', to: '/notifications', icon: Bell },
];

const SETTINGS_ITEMS: NavItem[] = [
  { label: 'Chi nhánh', to: '/branches', icon: Building2, roles: ['ADMIN'] },
  { label: 'Phòng ban', to: '/departments', icon: FolderKanban, roles: ['ADMIN', 'IT_STAFF'] },
  { label: 'Danh mục', to: '/categories', icon: Package, roles: ['ADMIN'] },
];

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { user } = useAuth();
  const role = user?.role as RoleCode | undefined;

  const isVisible = (item: NavItem) =>
    !item.roles || !role || item.roles.includes(role);

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    cn(
      'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors',
      isActive
        ? 'bg-indigo-50 text-indigo-700 font-medium'
        : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
    );

  return (
    <>
      {/* Mobile overlay backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/40 lg:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      {/* Sidebar panel */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-40 flex flex-col w-64 bg-white border-r border-gray-200',
          'transition-transform duration-200',
          'lg:translate-x-0 lg:static lg:z-auto',
          isOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        {/* Header */}
        <div className="flex items-center justify-between h-16 px-4 border-b border-gray-200 shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center">
              <Monitor size={16} className="text-white" aria-hidden="true" />
            </div>
            <span className="font-bold text-gray-900 text-base">ITAM</span>
          </div>
          {/* Nút đóng trên mobile */}
          <button
            onClick={onClose}
            className="lg:hidden p-1.5 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100"
            aria-label="Đóng menu"
          >
            <X size={18} />
          </button>
        </div>

        {/* Nav chính */}
        <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-0.5">
          {NAV_ITEMS.filter(isVisible).map((item) => (
            <NavLink key={item.to} to={item.to} className={navLinkClass} onClick={onClose}>
              <item.icon size={18} aria-hidden="true" />
              {item.label}
            </NavLink>
          ))}

          {/* Phần cài đặt cấu hình — chỉ role có quyền */}
          {SETTINGS_ITEMS.some(isVisible) && (
            <>
              <div className="pt-4 pb-1 px-3">
                <p className="text-xs font-medium text-gray-400 uppercase tracking-wider">Cấu hình</p>
              </div>
              {SETTINGS_ITEMS.filter(isVisible).map((item) => (
                <NavLink key={item.to} to={item.to} className={navLinkClass} onClick={onClose}>
                  <item.icon size={18} aria-hidden="true" />
                  {item.label}
                </NavLink>
              ))}
            </>
          )}
        </nav>

        {/* Footer — thông tin user nhỏ */}
        {user && (
          <div className="px-4 py-3 border-t border-gray-200 shrink-0">
            <p className="text-sm font-medium text-gray-900 truncate">{user.fullName}</p>
            <p className="text-xs text-gray-400 truncate">{user.email}</p>
          </div>
        )}
      </aside>
    </>
  );
}
