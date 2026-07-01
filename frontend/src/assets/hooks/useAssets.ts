import { useState, useEffect, useCallback } from 'react';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import { buildPagedQuery } from '@/lib/queryParams';
import type {
  Asset,
  AssetDetail,
  AssetImage,
  AssetAssignmentHistory,
  CreateAssetRequest,
  UpdateAssetRequest,
  ForceReturnRequest,
  AssetFilter,
} from '../types/asset.types';

/** Danh sách assets có phân trang + filter */
export function useAssets(filter: AssetFilter = {}, page = 0, size = 20) {
  const [assets, setAssets] = useState<Asset[]>([]);
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
          branchId: filter.branchId,
          categoryId: filter.categoryId,
          assignedTo: filter.assignedTo,
        },
        { page, size }
      );
      const res = await apiClient.get<ApiResponse<Asset[]>>(`/assets?${query}`);
      setAssets(res.data.data ?? []);
      const p = res.data.meta.pagination;
      if (p) {
        setTotalPages(p.totalPages);
        setTotalElements(p.totalElements);
      }
    } catch {
      setError('Không thể tải danh sách thiết bị');
    } finally {
      setIsLoading(false);
    }
  }, [page, size, filter.status, filter.branchId, filter.categoryId, filter.assignedTo]);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  return { assets, isLoading, error, totalPages, totalElements, refetch: fetchList };
}

/** Chi tiết 1 asset — fetch kèm images */
export function useAsset(id: number | null) {
  const [asset, setAsset] = useState<AssetDetail | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const fetch = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setError('');
    try {
      // Fetch asset + images song song
      const [assetRes, imagesRes] = await Promise.all([
        apiClient.get<ApiResponse<AssetDetail>>(`/assets/${id}`),
        apiClient.get<ApiResponse<AssetImage[]>>(`/assets/${id}/images`),
      ]);
      const assetData = assetRes.data.data ?? null;
      if (assetData) {
        assetData.images = imagesRes.data.data ?? [];
      }
      setAsset(assetData);
    } catch {
      setError('Không thể tải thông tin thiết bị');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { asset, isLoading, error, refetch: fetch };
}

/** Lịch sử cấp phát của 1 asset */
export function useAssetAssignmentHistory(assetId: number | null) {
  const [history, setHistory] = useState<AssetAssignmentHistory[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (!assetId) return;
    setIsLoading(true);
    apiClient
      .get<ApiResponse<AssetAssignmentHistory[]>>(`/assets/${assetId}/assignment-history`)
      .then((res) => setHistory(res.data.data ?? []))
      .catch(() => setHistory([]))
      .finally(() => setIsLoading(false));
  }, [assetId]);

  return { history, isLoading };
}

/** Actions: create, update, delete, restore, forceReturn, uploadImage */
export function useAssetActions() {
  const [isLoading, setIsLoading] = useState(false);

  const create = useCallback(async (data: CreateAssetRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<Asset>>('/assets', data);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const update = useCallback(async (id: number, data: UpdateAssetRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.put<ApiResponse<Asset>>(`/assets/${id}`, data);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const softDelete = useCallback(async (id: number) => {
    setIsLoading(true);
    try {
      await apiClient.delete(`/assets/${id}`);
    } catch (err) {
      // Ném lại để caller có thể hiển thị thông báo lỗi (VD: asset đang có request PENDING)
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const restore = useCallback(async (id: number) => {
    setIsLoading(true);
    try {
      await apiClient.post(`/assets/${id}/restore`);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const forceReturn = useCallback(async (id: number, data: ForceReturnRequest) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<Asset>>(`/assets/${id}/force-return`, data);
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const uploadImage = useCallback(async (id: number, file: File) => {
    setIsLoading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await apiClient.post<ApiResponse<{ id: number; assetId: number; url: string }>>(
        `/assets/${id}/images`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      );
      return res.data.data!;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { isLoading, create, update, softDelete, restore, forceReturn, uploadImage };
}
