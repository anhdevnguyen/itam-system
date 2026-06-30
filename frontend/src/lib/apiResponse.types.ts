// Types cho ApiResponse wrapper — khớp với docs/01-ARCHITECTURE.md mục 7

export interface ApiError {
  code: string;
  field: string | null;
  message: string;
}

export interface Pagination {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Meta {
  timestamp: string;
  pagination?: Pagination;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  errors?: ApiError[];
  meta: Meta;
}
