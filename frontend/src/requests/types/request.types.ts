/**
 * request.types.ts — Types cho module Requests
 * Khớp với docs/04-API.md mục 12, docs/07-BUSINESS-RULES.md mục 1
 */

export type RequestType = 'ASSIGN' | 'RETURN';

export type RequestStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'FULFILLED'
  | 'CANCELLED';

export interface Request {
  id: number;
  type: RequestType;
  status: RequestStatus;
  assetId: number;
  assetCode: string;
  assetName: string;
  employeeId: number;
  employeeName: string;
  approvedBy: number | null;
  approvedByName: string | null;
  fulfilledBy: number | null;
  fulfilledByName: string | null;
  note: string | null;
  rejectionReason: string | null;
  approvedAt: string | null;
  fulfilledAt: string | null;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
}

export interface CreateRequestRequest {
  type: RequestType;
  assetId: number;
  note?: string;
}

export interface RejectRequestRequest {
  rejectionReason: string;
}

export interface RequestFilter {
  status?: RequestStatus | '';
  employeeId?: number | '';
  branchId?: number | '';
}
