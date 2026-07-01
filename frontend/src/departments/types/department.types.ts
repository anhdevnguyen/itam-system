export interface Department {
  id: number;
  name: string;
  branchId: number;
  branchName: string;
  managerId: number | null;
  managerName: string | null;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
}

export interface CreateDepartmentRequest {
  name: string;
  branchId: number;
  managerId?: number | null;
}

export interface UpdateDepartmentRequest {
  name?: string;
  managerId?: number | null;
}
