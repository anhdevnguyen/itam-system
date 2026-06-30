// Types cho module auth — khớp với API docs/04-API.md mục 4

export type RoleCode = 'ADMIN' | 'IT_STAFF' | 'MANAGER' | 'EMPLOYEE';

/** Thông tin user sau khi đăng nhập — từ LoginResponse.user */
export interface UserInfo {
  id: number;
  email: string;
  fullName: string;
  role: RoleCode;
  branchId: number;
  mustChangePassword: boolean;
}

/** Response của POST /auth/login */
export interface LoginResponse {
  accessToken: string;
  expiresIn: number; // giây
  user: UserInfo;
}

/** Response của POST /auth/refresh */
export interface TokenResponse {
  accessToken: string;
  expiresIn: number; // giây
}

/** Request body cho POST /auth/login */
export interface LoginRequest {
  email: string;
  password: string;
}

/** State của AuthContext */
export interface AuthState {
  user: UserInfo | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

export interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<UserInfo>;
  logout: () => Promise<void>;
  refreshAccessToken: () => Promise<string | null>;
}
