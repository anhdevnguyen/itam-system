export interface Category {
  id: number;
  code: string;
  name: string;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
}

export interface CreateCategoryRequest {
  code: string;
  name: string;
}

export interface UpdateCategoryRequest {
  name?: string;
}
