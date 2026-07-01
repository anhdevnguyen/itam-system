import { useState, useEffect, useCallback } from 'react';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import type { Department, CreateDepartmentRequest, UpdateDepartmentRequest } from '../types/department.types';

export function useDepartments(branchId?: number, page = 0, size = 100) {
  const [departments, setDepartments] = useState<Department[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchList = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const params = new URLSearchParams({ page: String(page), size: String(size), sort: 'createdAt,desc' });
      if (branchId) params.set('branchId', String(branchId));
      const res = await apiClient.get<ApiResponse<Department[]>>(`/departments?${params}`);
      setDepartments(res.data.data ?? []);
      const p = res.data.meta.pagination;
      if (p) { setTotalPages(p.totalPages); setTotalElements(p.totalElements); }
    } catch {
      setError('Không thể tải danh sách phòng ban');
    } finally {
      setIsLoading(false);
    }
  }, [page, size, branchId]);

  useEffect(() => { fetchList(); }, [fetchList]);

  return { departments, isLoading, error, totalPages, totalElements, refetch: fetchList };
}

export function useDepartmentActions() {
  const [isLoading, setIsLoading] = useState(false);

  const create = useCallback(async (data: CreateDepartmentRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<Department>>('/departments', data);
      return res.data.data!;
    } finally { setIsLoading(false); }
  }, []);

  const update = useCallback(async (id: number, data: UpdateDepartmentRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.put<ApiResponse<Department>>(`/departments/${id}`, data);
      return res.data.data!;
    } finally { setIsLoading(false); }
  }, []);

  const softDelete = useCallback(async (id: number) => {
    setIsLoading(true);
    try {
      await apiClient.delete(`/departments/${id}`);
    } finally { setIsLoading(false); }
  }, []);

  return { isLoading, create, update, softDelete };
}
