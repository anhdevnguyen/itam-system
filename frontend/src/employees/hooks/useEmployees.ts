import { useState, useEffect, useCallback } from 'react';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import type { Employee, CreateEmployeeRequest, UpdateEmployeeRequest, EmployeeFilter } from '../types/employee.types';

/** Danh sách employees có phân trang + filter */
export function useEmployees(filter: EmployeeFilter = {}, page = 0, size = 20) {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchList = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const params = new URLSearchParams({ page: String(page), size: String(size), sort: 'createdAt,desc' });
      if (filter.branchId) params.set('branchId', String(filter.branchId));
      if (filter.departmentId) params.set('departmentId', String(filter.departmentId));
      if (filter.roleCode) params.set('roleCode', filter.roleCode);

      const res = await apiClient.get<ApiResponse<Employee[]>>(`/employees?${params}`);
      setEmployees(res.data.data ?? []);
      const p = res.data.meta.pagination;
      if (p) { setTotalPages(p.totalPages); setTotalElements(p.totalElements); }
    } catch {
      setError('Không thể tải danh sách nhân viên');
    } finally {
      setIsLoading(false);
    }
  }, [page, size, filter.branchId, filter.departmentId, filter.roleCode]);

  useEffect(() => { fetchList(); }, [fetchList]);

  return { employees, isLoading, error, totalPages, totalElements, refetch: fetchList };
}

/** Chi tiết 1 employee */
export function useEmployee(id: number | null) {
  const [employee, setEmployee] = useState<Employee | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (!id) return;
    setIsLoading(true);
    apiClient.get<ApiResponse<Employee>>(`/employees/${id}`)
      .then((res) => setEmployee(res.data.data ?? null))
      .catch(() => setEmployee(null))
      .finally(() => setIsLoading(false));
  }, [id]);

  return { employee, isLoading };
}

/** Actions: create, update, delete, resetPassword */
export function useEmployeeActions() {
  const [isLoading, setIsLoading] = useState(false);

  const create = useCallback(async (data: CreateEmployeeRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<{ employee: Employee; temporaryPassword: string }>>('/employees', data);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const update = useCallback(async (id: number, data: UpdateEmployeeRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.put<ApiResponse<Employee>>(`/employees/${id}`, data);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const softDelete = useCallback(async (id: number) => {
    setIsLoading(true);
    try {
      const res = await apiClient.delete<ApiResponse<Employee>>(`/employees/${id}`);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const resetPassword = useCallback(async (id: number) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<{ temporaryPassword: string }>>(`/employees/${id}/reset-password`);
      return res.data.data!.temporaryPassword;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { isLoading, create, update, softDelete, resetPassword };
}
