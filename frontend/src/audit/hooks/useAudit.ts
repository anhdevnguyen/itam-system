import { useState, useEffect, useCallback } from 'react';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import { buildPagedQuery } from '@/lib/queryParams';
import type {
  AuditSession,
  AuditScan,
  Discrepancy,
  CreateAuditSessionRequest,
  ScanRequest,
  ResolveDiscrepancyRequest,
} from '../types/audit.types';

/** Danh sách audit sessions */
export function useAuditSessions(page = 0, size = 20) {
  const [sessions, setSessions] = useState<AuditSession[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchList = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const query = buildPagedQuery({}, { page, size });
      const res = await apiClient.get<ApiResponse<AuditSession[]>>(`/audits?${query}`);
      setSessions(res.data.data ?? []);
      const p = res.data.meta.pagination;
      if (p) { setTotalPages(p.totalPages); setTotalElements(p.totalElements); }
    } catch {
      setError('Không thể tải danh sách phiên kiểm kê');
    } finally {
      setIsLoading(false);
    }
  }, [page, size]);

  useEffect(() => { fetchList(); }, [fetchList]);

  return { sessions, isLoading, error, totalPages, totalElements, refetch: fetchList };
}

/** Chi tiết 1 audit session */
export function useAuditSession(id: number | null) {
  const [session, setSession] = useState<AuditSession | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const fetch = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setError('');
    try {
      const res = await apiClient.get<ApiResponse<AuditSession>>(`/audits/${id}`);
      setSession(res.data.data ?? null);
    } catch {
      setError('Không thể tải thông tin phiên kiểm kê');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => { fetch(); }, [fetch]);

  return { session, isLoading, error, refetch: fetch };
}

/** Danh sách discrepancies của 1 session */
export function useDiscrepancies(sessionId: number | null, page = 0) {
  const [discrepancies, setDiscrepancies] = useState<Discrepancy[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(false);

  const fetch = useCallback(async () => {
    if (!sessionId) return;
    setIsLoading(true);
    try {
      const query = buildPagedQuery({}, { page, size: 50 });
      const res = await apiClient.get<ApiResponse<Discrepancy[]>>(
        `/audits/${sessionId}/discrepancies?${query}`
      );
      setDiscrepancies(res.data.data ?? []);
      const p = res.data.meta.pagination;
      if (p) setTotalPages(p.totalPages);
    } catch {
      setDiscrepancies([]);
    } finally {
      setIsLoading(false);
    }
  }, [sessionId, page]);

  useEffect(() => { fetch(); }, [fetch]);

  return { discrepancies, isLoading, totalPages, refetch: fetch };
}

/** Actions: create session, scan, complete, resolve discrepancy */
export function useAuditActions() {
  const [isLoading, setIsLoading] = useState(false);

  const createSession = useCallback(async (data: CreateAuditSessionRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<AuditSession>>('/audits', data);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const scan = useCallback(async (sessionId: number, data: ScanRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<AuditScan>>(`/audits/${sessionId}/scan`, data);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const completeSession = useCallback(async (sessionId: number) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<AuditSession>>(`/audits/${sessionId}/complete`);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const resolveDiscrepancy = useCallback(async (discrepancyId: number, data: ResolveDiscrepancyRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<Discrepancy>>(
        `/audits/discrepancies/${discrepancyId}/resolve`,
        data
      );
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { isLoading, createSession, scan, completeSession, resolveDiscrepancy };
}
