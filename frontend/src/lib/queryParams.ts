/**
 * queryParams.ts — Helper build query string cho pagination/filter/sort
 * Theo đặc tả docs/04-API.md mục 2.
 */

export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string; // VD: "createdAt,desc"
}

/**
 * Xây URLSearchParams từ object params, bỏ qua các key có value undefined/null/rỗng.
 *
 * @example
 * buildQueryParams({ page: 0, size: 20, branchId: 1, status: '' })
 * // → "page=0&size=20&branchId=1"
 */
export function buildQueryParams(
  params: Record<string, string | number | boolean | undefined | null>
): URLSearchParams {
  const sp = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      sp.set(key, String(value));
    }
  }
  return sp;
}

/**
 * Xây query string đầy đủ với pagination mặc định.
 */
export function buildPagedQuery(
  filter: Record<string, string | number | boolean | undefined | null>,
  pagination: PaginationParams = {}
): string {
  const { page = 0, size = 20, sort = 'createdAt,desc' } = pagination;
  return buildQueryParams({ page, size, sort, ...filter }).toString();
}
