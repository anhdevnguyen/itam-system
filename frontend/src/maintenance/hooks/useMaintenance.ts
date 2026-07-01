import { useState, useEffect, useCallback } from 'react';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import { buildPagedQuery } from '@/lib/queryParams';
import type {
  MaintenanceRecord,
  CreateMaintenanceRequest,
  UpdateMaintenanceRequest,
  MaintenanceFilter,
} from '../types/maintenance.types';

/** Danh sách maintenance records có phân trang + filter */
export function useMaintenanceRecords(filter: MaintenanceFilter = {}, page = 0, size = 20) {
  const [records, setRecords] = useState<MaintenanceRecord[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchList = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const query = buildPagedQuery(
        {
          assetId: filter.assetId,
          status: filter.status,
          type: filter.type,
        },
        { page, size }
      );
      const res = await apiClient.get<ApiResponse<MaintenanceRecord[]>>(`/maintenance?${query}`);
      setRecords(res.data.data ?? []);
      const p = res.data.meta.pagination;
      if (p) {
        setTotalPages(p.totalPages);
        setTotalElements(p.totalElements);
      }
    } catch {
      setError('Không thể tải danh sách bảo trì');
    } finally {
      setIsLoading(false);
    }
  }, [page, size, filter.assetId, filter.status, filter.type]);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  return { records, isLoading, error, totalPages, totalElements, refetch: fetchList };
}

/** Chi tiết 1 maintenance record */
export function useMaintenanceRecord(id: number | null) {
  const [record, setRecord] = useState<MaintenanceRecord | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const fetch = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setError('');
    try {
      const res = await apiClient.get<ApiResponse<MaintenanceRecord>>(`/maintenance/${id}`);
      setRecord(res.data.data ?? null);
    } catch {
      setError('Không thể tải thông tin bảo trì');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { record, isLoading, error, refetch: fetch };
}

/** Actions: create, update, delete */
export function useMaintenanceActions() {
  const [isLoading, setIsLoading] = useState(false);

  const create = useCallback(async (data: CreateMaintenanceRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<MaintenanceRecord>>('/maintenance', data);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const update = useCallback(async (id: number, data: UpdateMaintenanceRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.put<ApiResponse<MaintenanceRecord>>(`/maintenance/${id}`, data);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const softDelete = useCallback(async (id: number) => {
    setIsLoading(true);
    try {
      await apiClient.delete(`/maintenance/${id}`);
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { isLoading, create, update, softDelete };
}
