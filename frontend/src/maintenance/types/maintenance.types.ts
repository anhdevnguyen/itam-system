/**
 * maintenance.types.ts — Types cho module Maintenance
 * Khớp với docs/04-API.md mục 13, docs/05-DATABASE.md mục 5.11
 */

export type MaintenanceType = 'WARRANTY' | 'REPAIR' | 'PERIODIC';

export type MaintenanceStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface MaintenanceRecord {
  id: number;
  assetId: number;
  assetCode: string;
  assetName: string;
  type: MaintenanceType;
  status: MaintenanceStatus;
  description: string | null;
  scheduledDate: string | null; // YYYY-MM-DD
  completedDate: string | null; // YYYY-MM-DD
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
}

export interface CreateMaintenanceRequest {
  assetId: number;
  type: MaintenanceType;
  description?: string;
  scheduledDate?: string; // YYYY-MM-DD
}

export interface UpdateMaintenanceRequest {
  type?: MaintenanceType;
  status?: MaintenanceStatus;
  description?: string;
  scheduledDate?: string;
  completedDate?: string;
}

export interface MaintenanceFilter {
  assetId?: number | '';
  status?: MaintenanceStatus | '';
  type?: MaintenanceType | '';
}
