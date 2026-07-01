import { useState, useRef, useEffect } from 'react';
import { Menu, Bell, ChevronDown, User, KeyRound, LogOut } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import { useUnreadCount } from '@/notifications/hooks/useNotifications';

interface NavbarProps {
  onMenuClick: () => void;
}

export function Navbar({ onMenuClick }: NavbarProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { unreadCount } = useUnreadCount();

  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);
  const notifRef = useRef<HTMLDivElement>(null);

  // Đóng dropdown khi click ngoài
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        setUserMenuOpen(false);
      }
      if (notifRef.current && !notifRef.current.contains(e.target as Node)) {
        setNotifOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleLogout = async () => {
    setUserMenuOpen(false);
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <header className="sticky top-0 z-20 h-16 bg-white border-b border-gray-200 flex items-center justify-between px-4 shrink-0">
      {/* Hamburger — chỉ hiện trên mobile/tablet */}
      <button
        onClick={onMenuClick}
        className="lg:hidden p-2 rounded-md text-gray-500 hover:bg-gray-100 transition-colors"
        aria-label="Mở menu"
      >
        <Menu size={20} />
      </button>

      {/* Spacer trên desktop */}
      <div className="flex-1" />

      <div className="flex items-center gap-1">
        {/* Notification bell */}
        <div ref={notifRef} className="relative">
          <button
            onClick={() => { setNotifOpen((v) => !v); setUserMenuOpen(false); }}
            aria-label={`Thông báo${unreadCount > 0 ? `, ${unreadCount} chưa đọc` : ''}`}
            className="relative p-2 rounded-md text-gray-500 hover:bg-gray-100 transition-colors"
          >
            <Bell size={20} />
            {unreadCount > 0 && (
              <span className="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </button>

          {/* Notification panel dropdown */}
          {notifOpen && (
            <div className="absolute right-0 mt-1 w-80 bg-white rounded-xl shadow-lg border border-gray-200 py-1 z-50">
              <NotificationPanel onClose={() => setNotifOpen(false)} />
            </div>
          )}
        </div>

        {/* User menu */}
        <div ref={userMenuRef} className="relative">
          <button
            onClick={() => { setUserMenuOpen((v) => !v); setNotifOpen(false); }}
            aria-expanded={userMenuOpen}
            aria-haspopup="true"
            className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-gray-700 hover:bg-gray-100 transition-colors"
          >
            <div className="w-7 h-7 bg-indigo-100 rounded-full flex items-center justify-center">
              <User size={14} className="text-indigo-600" aria-hidden="true" />
            </div>
            <span className="hidden sm:block max-w-[120px] truncate font-medium">
              {user?.fullName ?? 'Người dùng'}
            </span>
            <ChevronDown size={14} className="text-gray-400" aria-hidden="true" />
          </button>

          {userMenuOpen && (
            <div className="absolute right-0 mt-1 w-48 bg-white rounded-xl shadow-lg border border-gray-200 py-1 z-50">
              <div className="px-3 py-2 border-b border-gray-100">
                <p className="text-sm font-medium text-gray-900 truncate">{user?.fullName}</p>
                <p className="text-xs text-gray-400 truncate">{user?.email}</p>
              </div>
              <button
                onClick={() => { setUserMenuOpen(false); navigate('/profile'); }}
                className="w-full flex items-center gap-2.5 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
              >
                <User size={15} aria-hidden="true" />
                Hồ sơ cá nhân
              </button>
              <button
                onClick={() => { setUserMenuOpen(false); navigate('/change-password'); }}
                className="w-full flex items-center gap-2.5 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
              >
                <KeyRound size={15} aria-hidden="true" />
                Đổi mật khẩu
              </button>
              <div className="my-1 border-t border-gray-100" />
              <button
                onClick={handleLogout}
                className="w-full flex items-center gap-2.5 px-3 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors"
              >
                <LogOut size={15} aria-hidden="true" />
                Đăng xuất
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

// ── Inline NotificationPanel (mini) — panel đầy đủ ở notifications/components ──
function NotificationPanel({ onClose }: { onClose: () => void }) {
  const navigate = useNavigate();
  return (
    <div>
      <div className="flex items-center justify-between px-3 py-2 border-b border-gray-100">
        <p className="text-sm font-semibold text-gray-900">Thông báo</p>
        <button
          onClick={() => { onClose(); navigate('/notifications'); }}
          className="text-xs text-indigo-600 hover:underline"
        >
          Xem tất cả
        </button>
      </div>
      <NotificationList onClose={onClose} />
    </div>
  );
}

// Lazy import tránh circular dependency — render inline danh sách 5 notif gần nhất
function NotificationList({ onClose }: { onClose: () => void }) {
  const navigate = useNavigate();
  const { notifications, markRead } = useRecentNotifications();

  if (notifications.length === 0) {
    return <p className="px-3 py-6 text-sm text-center text-gray-400">Không có thông báo mới</p>;
  }

  /** Điều hướng đến entity liên quan dựa trên type + relatedEntityId */
  function getNotifLink(type: string, relatedEntityId: number | null): string | null {
    if (!relatedEntityId) return null;
    if (type.startsWith('REQUEST_')) return `/requests/${relatedEntityId}`;
    if (type.startsWith('MAINTENANCE_')) return `/maintenance/${relatedEntityId}`;
    if (type.startsWith('AUDIT_')) return `/audit/${relatedEntityId}`;
    return null;
  }

  return (
    <ul className="max-h-72 overflow-y-auto">
      {notifications.map((n) => {
        const link = getNotifLink(n.type, n.relatedEntityId);
        return (
          <li key={n.id}>
            <button
              onClick={() => {
                markRead(n.id);
                onClose();
                if (link) navigate(link);
              }}
              className={`w-full text-left px-3 py-2.5 hover:bg-gray-50 transition-colors border-b border-gray-50 last:border-0 ${!n.isRead ? 'bg-indigo-50/40' : ''}`}
            >
              <p className={`text-sm leading-snug ${!n.isRead ? 'font-medium text-gray-900' : 'text-gray-700'}`}>
                {n.message}
              </p>
              <p className="text-xs text-gray-400 mt-0.5">
                {new Date(n.createdAt).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })}
              </p>
            </button>
          </li>
        );
      })}
    </ul>
  );
}

// Hook tạm dùng cho dropdown — sẽ dùng lại từ notifications module
import { useRecentNotifications } from '@/notifications/hooks/useNotifications';
