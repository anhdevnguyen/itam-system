import type { RoleCode } from '@/auth/types/auth.types';

export type { RoleCode };

/** Flat DTO — khớp với EmployeeResponse từ backend */
export interface Employee {
  id: number;
  email: string;
  fullName: string;
  roleId: number;
  roleCode: RoleCode;
  branchId: number;
  branchName: string;
  departmentId: number | null;
  departmentName: string | null;
  mustChangePassword: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEmployeeRequest {
  fullName: string;
  email: string;
  roleId: number;
  branchId: number;
  departmentId?: number | null;
}

export interface UpdateEmployeeRequest {
  fullName?: string;
  roleId?: number;
  branchId?: number;
  departmentId?: number | null;
}

export interface EmployeeFilter {
  branchId?: number;
  departmentId?: number;
  roleCode?: RoleCode;
}
