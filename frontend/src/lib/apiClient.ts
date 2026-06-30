import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse } from './apiResponse.types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

/**
 * Axios instance dùng chung toàn hệ thống.
 * - Tự động đính kèm Authorization header từ in-memory token
 * - Tự động refresh khi nhận 401 (access token hết hạn)
 * - withCredentials = true để browser gửi httpOnly cookie (refresh token)
 */
const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  withCredentials: true, // Gửi cookie (refresh token) theo mỗi request
  headers: {
    'Content-Type': 'application/json',
  },
});

// ── In-memory token storage ───────────────────────────────────────────────────
// Access Token lưu in-memory (React state thông qua AuthContext).
// Module này cung cấp getter/setter để interceptor có thể đọc token hiện tại
// mà không cần import AuthContext (tránh circular dependency).

let _accessToken: string | null = null;

export function setAccessToken(token: string | null): void {
  _accessToken = token;
}

export function getAccessToken(): string | null {
  return _accessToken;
}

// ── Refresh lock — tránh nhiều request đồng thời đều trigger refresh ──────────
let _isRefreshing = false;
let _refreshSubscribers: Array<(token: string) => void> = [];

function subscribeTokenRefresh(cb: (token: string) => void): void {
  _refreshSubscribers.push(cb);
}

function onTokenRefreshed(newToken: string): void {
  _refreshSubscribers.forEach((cb) => cb(newToken));
  _refreshSubscribers = [];
}

function onRefreshFailed(): void {
  _refreshSubscribers = [];
}

// ── Request interceptor — đính kèm Authorization header ──────────────────────
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ── Response interceptor — tự động refresh khi 401 ───────────────────────────
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Chỉ xử lý 401 và chưa retry lần nào
    if (error.response?.status === 401 && !originalRequest._retry) {
      // Không retry các endpoint auth — tránh vòng lặp vô tận
      const url = originalRequest.url ?? '';
      if (url.includes('/auth/login') || url.includes('/auth/refresh')) {
        return Promise.reject(error);
      }

      if (_isRefreshing) {
        // Có request khác đang refresh — đợi token mới rồi retry
        return new Promise((resolve, reject) => {
          subscribeTokenRefresh((newToken: string) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            resolve(apiClient(originalRequest));
          });
          // Timeout: nếu refresh fail thì reject
          setTimeout(() => reject(error), 10000);
        });
      }

      originalRequest._retry = true;
      _isRefreshing = true;

      try {
        const refreshResponse = await axios.post<ApiResponse<{ accessToken: string; expiresIn: number }>>(
          `${BASE_URL}/auth/refresh`,
          {},
          { withCredentials: true }
        );

        const newToken = refreshResponse.data.data!.accessToken;
        setAccessToken(newToken);
        onTokenRefreshed(newToken);

        // Retry original request với token mới
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Refresh thất bại — xóa token và redirect về login
        setAccessToken(null);
        onRefreshFailed();
        // Dispatch event để AuthContext bắt và redirect
        window.dispatchEvent(new CustomEvent('auth:unauthorized'));
        return Promise.reject(refreshError);
      } finally {
        _isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
