import { useState, useEffect, useCallback } from 'react';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import { buildPagedQuery } from '@/lib/queryParams';
import type {
  Request,
  CreateRequestRequest,
  RejectRequestRequest,
  RequestFilter,
} from '../types/request.types';

/** Danh sách requests có phân trang + filter */
export function useRequests(filter: RequestFilter = {}, page = 0, size = 20) {
  const [requests, setRequests] = useState<Request[]>([]);
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
          status: filter.status,
          employeeId: filter.employeeId,
          branchId: filter.branchId,
        },
        { page, size }
      );
      const res = await apiClient.get<ApiResponse<Request[]>>(`/requests?${query}`);
      setRequests(res.data.data ?? []);
      const p = res.data.meta.pagination;
      if (p) {
        setTotalPages(p.totalPages);
        setTotalElements(p.totalElements);
      }
    } catch {
      setError('Không thể tải danh sách yêu cầu');
    } finally {
      setIsLoading(false);
    }
  }, [page, size, filter.status, filter.employeeId, filter.branchId]);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  return { requests, isLoading, error, totalPages, totalElements, refetch: fetchList };
}

/** Chi tiết 1 request */
export function useRequest(id: number | null) {
  const [request, setRequest] = useState<Request | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const fetch = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setError('');
    try {
      const res = await apiClient.get<ApiResponse<Request>>(`/requests/${id}`);
      setRequest(res.data.data ?? null);
    } catch {
      setError('Không thể tải thông tin yêu cầu');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { request, isLoading, error, refetch: fetch };
}

/** Actions: create, approve, reject, fulfill, cancel */
export function useRequestActions() {
  const [isLoading, setIsLoading] = useState(false);

  const create = useCallback(async (data: CreateRequestRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<Request>>('/requests', data);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const approve = useCallback(async (id: number) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<Request>>(`/requests/${id}/approve`);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const reject = useCallback(async (id: number, data: RejectRequestRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<Request>>(`/requests/${id}/reject`, data);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const fulfill = useCallback(async (id: number) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<Request>>(`/requests/${id}/fulfill`);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const cancel = useCallback(async (id: number) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<Request>>(`/requests/${id}/cancel`);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { isLoading, create, approve, reject, fulfill, cancel };
}
