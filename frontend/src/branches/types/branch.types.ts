export interface Branch {
  id: number;
  code: string;
  name: string;
  address: string | null;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
}

export interface CreateBranchRequest {
  code: string;
  name: string;
  address?: string;
}

export interface UpdateBranchRequest {
  code?: string;
  name?: string;
  address?: string;
}
