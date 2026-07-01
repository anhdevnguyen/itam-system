/**
 * asset.types.ts — Types cho module Assets
 * Khớp với docs/04-API.md mục 11, docs/05-DATABASE.md mục 5.7
 */

export type AssetStatus =
  | 'AVAILABLE'
  | 'ASSIGNED'
  | 'IN_MAINTENANCE'
  | 'BROKEN'
  | 'DISPOSED'
  | 'LOST';

export interface Asset {
  id: number;
  code: string;
  name: string;
  categoryId: number;
  categoryName: string;
  branchId: number;
  branchName: string;
  status: AssetStatus;
  assignedTo: number | null;
  assignedToName: string | null;
  purchaseDate: string; // ISO date string YYYY-MM-DD
  value: number; // VNĐ
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
}

/** Response chi tiết asset kèm images */
export interface AssetDetail extends Asset {
  images: AssetImage[];
}

export interface AssetImage {
  id: number;
  assetId: number;
  url: string;
  createdAt: string;
}

export interface AssetAssignmentHistory {
  id: number;
  assetId: number;
  employeeId: number;
  employeeName: string;
  requestId: number | null;
  assignedAt: string;
  returnedAt: string | null;
}

export interface CreateAssetRequest {
  name: string;
  categoryId: number;
  branchId: number;
  purchaseDate: string; // YYYY-MM-DD
  value: number;
}

export interface UpdateAssetRequest {
  name?: string;
  categoryId?: number;
  purchaseDate?: string;
  value?: number;
  status?: AssetStatus;
}

export interface ForceReturnRequest {
  reason: string;
}

export interface AssetFilter {
  status?: AssetStatus | '';
  branchId?: number | '';
  categoryId?: number | '';
  assignedTo?: number | '';
}
