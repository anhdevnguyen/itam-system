/**
 * audit.types.ts — Types cho module Audit
 * Khớp với docs/04-API.md mục 14, docs/05-DATABASE.md mục 5.12-5.14
 */

export type AuditSessionStatus = 'IN_PROGRESS' | 'COMPLETED';

export type DiscrepancyType = 'LOCATION_MISMATCH' | 'MISSING' | 'UNEXPECTED_FOUND';

export type DiscrepancyStatus = 'OPEN' | 'RESOLVED';

export type ResolutionAction = 'CONFIRM_LOST' | 'FOUND';

export interface AuditSession {
  id: number;
  branchId: number;
  branchName: string;
  status: AuditSessionStatus;
  createdBy: number;
  createdByName: string;
  note: string | null;
  startedAt: string;
  expiresAt: string;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
  /** Số asset đã quét / tổng asset chi nhánh (nếu BE trả về) */
  scannedCount?: number;
  totalAssetCount?: number;
}

export interface AuditScan {
  id: number;
  auditSessionId: number;
  assetId: number;
  assetCode: string;
  assetName: string;
  scannedBy: number;
  scannedByName: string;
  scannedLocation: string | null;
  scannedAt: string;
}

export interface Discrepancy {
  id: number;
  auditSessionId: number;
  assetId: number;
  assetCode: string;
  assetName: string;
  type: DiscrepancyType;
  status: DiscrepancyStatus;
  expectedLocation: string | null;
  actualLocation: string | null;
  resolutionAction: ResolutionAction | null;
  resolvedBy: number | null;
  resolvedByName: string | null;
  resolvedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAuditSessionRequest {
  branchId: number;
  note?: string;
}

export interface ScanRequest {
  assetCode: string;
  scannedLocation?: string;
}

export interface ResolveDiscrepancyRequest {
  action: ResolutionAction;
}
