import { useState, useEffect, useCallback, useRef } from 'react';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import type { Notification } from '../types/notification.types';

const POLL_INTERVAL = 30_000; // 30 giây

/** Hook polling số lượng thông báo chưa đọc — dùng cho badge chuông */
export function useUnreadCount() {
  const [unreadCount, setUnreadCount] = useState(0);

  const fetchCount = useCallback(async () => {
    try {
      const res = await apiClient.get<ApiResponse<{ count: number }>>('/notifications/unread-count');
      setUnreadCount(res.data.data?.count ?? 0);
    } catch {
      // Lỗi không làm gián đoạn UI
    }
  }, []);

  useEffect(() => {
    fetchCount();
    const interval = setInterval(fetchCount, POLL_INTERVAL);
    return () => clearInterval(interval);
  }, [fetchCount]);

  return { unreadCount };
}

/** Hook lấy 5 thông báo gần nhất — dùng cho dropdown trên Navbar */
export function useRecentNotifications() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => { mountedRef.current = false; };
  }, []);

  const fetchRecent = useCallback(async () => {
    try {
      const res = await apiClient.get<ApiResponse<Notification[]>>('/notifications?page=0&size=5');
      if (mountedRef.current) setNotifications(res.data.data ?? []);
    } catch {
      // no-op
    }
  }, []);

  useEffect(() => {
    fetchRecent();
  }, [fetchRecent]);

  const markRead = useCallback(async (id: number) => {
    try {
      await apiClient.post(`/notifications/${id}/read`);
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)));
    } catch {
      // no-op
    }
  }, []);

  return { notifications, markRead, refetch: fetchRecent };
}

/** Hook đầy đủ cho trang Notifications */
export function useNotifications(page = 0) {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);

  const fetchPage = useCallback(async () => {
    setIsLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<Notification[]>>(`/notifications?page=${page}&size=20`);
      setNotifications(res.data.data ?? []);
      const p = res.data.meta.pagination;
      if (p) { setTotalPages(p.totalPages); setTotalElements(p.totalElements); }
    } catch {
      // no-op
    } finally {
      setIsLoading(false);
    }
  }, [page]);

  useEffect(() => { fetchPage(); }, [fetchPage]);

  const markRead = useCallback(async (id: number) => {
    try {
      await apiClient.post(`/notifications/${id}/read`);
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)));
    } catch {
      // no-op
    }
  }, []);

  return { notifications, isLoading, totalPages, totalElements, markRead, refetch: fetchPage };
}
