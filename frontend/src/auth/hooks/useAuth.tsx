import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  type ReactNode,
} from 'react';
import apiClient, { setAccessToken } from '@/lib/apiClient';
import type { AuthContextValue, AuthState, UserInfo, LoginResponse, TokenResponse } from '@/auth/types/auth.types';
import type { ApiResponse } from '@/lib/apiResponse.types';

// ── Context ───────────────────────────────────────────────────────────────────

const AuthContext = createContext<AuthContextValue | null>(null);

// ── Provider ──────────────────────────────────────────────────────────────────

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [state, setState] = useState<AuthState>({
    user: null,
    accessToken: null,
    isAuthenticated: false,
    isLoading: true, // true khi app khởi động — đang kiểm tra session
  });

  // ── Khởi tạo: thử refresh để khôi phục session khi load lại trang ────────
  useEffect(() => {
    const restoreSession = async () => {
      try {
        // Gọi /auth/refresh — nếu có Refresh Token cookie hợp lệ sẽ trả Access Token mới
        const response = await apiClient.post<ApiResponse<TokenResponse>>('/auth/refresh');
        const { accessToken } = response.data.data!;

        setAccessToken(accessToken);

        // Lấy thông tin user từ /employees/me
        const meResponse = await apiClient.get<ApiResponse<UserInfo>>('/employees/me', {
          headers: { Authorization: `Bearer ${accessToken}` },
        });
        const user = meResponse.data.data!;

        setState({
          user,
          accessToken,
          isAuthenticated: true,
          isLoading: false,
        });
      } catch {
        // Không có session hợp lệ — không phải lỗi, user chưa đăng nhập
        setAccessToken(null);
        setState({ user: null, accessToken: null, isAuthenticated: false, isLoading: false });
      }
    };

    restoreSession();
  }, []);

  // ── Lắng nghe event auth:unauthorized (từ apiClient interceptor) ──────────
  useEffect(() => {
    const handleUnauthorized = () => {
      setAccessToken(null);
      setState({ user: null, accessToken: null, isAuthenticated: false, isLoading: false });
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  // ── Login ─────────────────────────────────────────────────────────────────
  const login = useCallback(async (email: string, password: string): Promise<UserInfo> => {
    const response = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', {
      email,
      password,
    });

    const { accessToken, user } = response.data.data!;

    setAccessToken(accessToken);
    setState({
      user,
      accessToken,
      isAuthenticated: true,
      isLoading: false,
    });

    return user;
  }, []);

  // ── Logout ────────────────────────────────────────────────────────────────
  const logout = useCallback(async (): Promise<void> => {
    try {
      await apiClient.post('/auth/logout');
    } catch {
      // Logout graceful — dù API fail vẫn xóa state local
    } finally {
      setAccessToken(null);
      setState({ user: null, accessToken: null, isAuthenticated: false, isLoading: false });
    }
  }, []);

  // ── Refresh Access Token ──────────────────────────────────────────────────
  const refreshAccessToken = useCallback(async (): Promise<string | null> => {
    try {
      const response = await apiClient.post<ApiResponse<TokenResponse>>('/auth/refresh');
      const { accessToken } = response.data.data!;
      setAccessToken(accessToken);
      setState((prev) => ({ ...prev, accessToken }));
      return accessToken;
    } catch {
      setAccessToken(null);
      setState({ user: null, accessToken: null, isAuthenticated: false, isLoading: false });
      return null;
    }
  }, []);

  const value: AuthContextValue = {
    ...state,
    login,
    logout,
    refreshAccessToken,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// ── Hook ──────────────────────────────────────────────────────────────────────

/**
 * Hook để truy cập AuthContext trong bất kỳ component nào.
 * Phải được dùng bên trong <AuthProvider>.
 */
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within <AuthProvider>');
  }
  return ctx;
}
