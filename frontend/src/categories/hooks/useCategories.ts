import { useState, useEffect, useCallback } from 'react';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import type { Category, CreateCategoryRequest, UpdateCategoryRequest } from '../types/category.types';

export function useCategories(page = 0, size = 100) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchList = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const res = await apiClient.get<ApiResponse<Category[]>>(
        `/categories?page=${page}&size=${size}&sort=createdAt,desc`
      );
      setCategories(res.data.data ?? []);
      const p = res.data.meta.pagination;
      if (p) { setTotalPages(p.totalPages); setTotalElements(p.totalElements); }
    } catch {
      setError('Không thể tải danh sách danh mục');
    } finally {
      setIsLoading(false);
    }
  }, [page, size]);

  useEffect(() => { fetchList(); }, [fetchList]);

  return { categories, isLoading, error, totalPages, totalElements, refetch: fetchList };
}

export function useCategoryActions() {
  const [isLoading, setIsLoading] = useState(false);

  const create = useCallback(async (data: CreateCategoryRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<Category>>('/categories', data);
      return res.data.data!;
    } finally { setIsLoading(false); }
  }, []);

  const update = useCallback(async (id: number, data: UpdateCategoryRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.put<ApiResponse<Category>>(`/categories/${id}`, data);
      return res.data.data!;
    } finally { setIsLoading(false); }
  }, []);

  const softDelete = useCallback(async (id: number) => {
    setIsLoading(true);
    try {
      await apiClient.delete(`/categories/${id}`);
    } finally { setIsLoading(false); }
  }, []);

  return { isLoading, create, update, softDelete };
}
