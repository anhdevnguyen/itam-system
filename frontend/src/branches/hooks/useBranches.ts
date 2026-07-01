import { useState, useEffect, useCallback } from 'react';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import type { Branch, CreateBranchRequest, UpdateBranchRequest } from '../types/branch.types';

export function useBranches(page = 0, size = 100) {
  const [branches, setBranches] = useState<Branch[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchList = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const res = await apiClient.get<ApiResponse<Branch[]>>(
        `/branches?page=${page}&size=${size}&sort=createdAt,desc`
      );
      setBranches(res.data.data ?? []);
      const p = res.data.meta.pagination;
      if (p) { setTotalPages(p.totalPages); setTotalElements(p.totalElements); }
    } catch {
      setError('Không thể tải danh sách chi nhánh');
    } finally {
      setIsLoading(false);
    }
  }, [page, size]);

  useEffect(() => { fetchList(); }, [fetchList]);

  return { branches, isLoading, error, totalPages, totalElements, refetch: fetchList };
}

export function useBranchActions() {
  const [isLoading, setIsLoading] = useState(false);

  const create = useCallback(async (data: CreateBranchRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<Branch>>('/branches', data);
      return res.data.data!;
    } finally { setIsLoading(false); }
  }, []);

  const update = useCallback(async (id: number, data: UpdateBranchRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.put<ApiResponse<Branch>>(`/branches/${id}`, data);
      return res.data.data!;
    } finally { setIsLoading(false); }
  }, []);

  const softDelete = useCallback(async (id: number) => {
    setIsLoading(true);
    try {
      await apiClient.delete(`/branches/${id}`);
    } finally { setIsLoading(false); }
  }, []);

  return { isLoading, create, update, softDelete };
}
