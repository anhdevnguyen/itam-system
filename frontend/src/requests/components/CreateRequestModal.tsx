import { useState, useEffect } from 'react';
import axios from 'axios';
import { Modal } from '@/shared/components';
import apiClient from '@/lib/apiClient';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import type { ApiResponse } from '@/lib/apiResponse.types';
import type { ApiError } from '@/lib/apiResponse.types';
import type { Request, RequestType } from '../types/request.types';

interface AssetOption {
  id: number;
  code: string;
  name: string;
  status: string;
}

interface CreateRequestModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: (req: Request) => void;
  /** Nếu truyền vào → chế độ RETURN (chỉ dùng asset này) */
  fixedAsset?: AssetOption;
  /** Mặc định type */
  defaultType?: RequestType;
}

export function CreateRequestModal({
  open,
  onClose,
  onSuccess,
  fixedAsset,
  defaultType = 'ASSIGN',
}: CreateRequestModalProps) {
  const { user } = useAuth();
  const [type, setType] = useState<RequestType>(fixedAsset ? 'RETURN' : defaultType);
  const [assetId, setAssetId] = useState<number | ''>(fixedAsset?.id ?? '');
  const [note, setNote] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [globalError, setGlobalError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [availableAssets, setAvailableAssets] = useState<AssetOption[]>([]);
  const [loadingAssets, setLoadingAssets] = useState(false);

  // Load danh sách asset phù hợp khi mở modal:
  // - ASSIGN → asset AVAILABLE (chọn để cấp phát)
  // - RETURN → asset đang được giữ bởi chính user hiện tại (docs 07-BUSINESS-RULES mục 1.5)
  useEffect(() => {
    if (!open || fixedAsset) return;
    if (!user) return;
    setLoadingAssets(true);
    const url =
      type === 'ASSIGN'
        ? '/assets?status=AVAILABLE&page=0&size=200'
        : `/assets?status=ASSIGNED&assignedTo=${user.id}&page=0&size=200`;
    apiClient
      .get<ApiResponse<AssetOption[]>>(url)
      .then((res) => setAvailableAssets(res.data.data ?? []))
      .catch(() => setAvailableAssets([]))
      .finally(() => setLoadingAssets(false));
  }, [open, type, fixedAsset, user]);

  const handleClose = () => {
    setNote('');
    setErrors({});
    setGlobalError('');
    setAssetId(fixedAsset?.id ?? '');
    onClose();
  };

  const validate = () => {
    const errs: Record<string, string> = {};
    if (!assetId) errs.assetId = 'Vui lòng chọn thiết bị';
    return errs;
  };

  const handleSubmit = async () => {
    const errs = validate();
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return;
    }
    setErrors({});
    setGlobalError('');
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<Request>>('/requests', {
        type,
        assetId: Number(assetId),
        note: note.trim() || undefined,
      });
      onSuccess(res.data.data!);
      handleClose();
    } catch (err) {
      if (axios.isAxiosError(err) && err.response) {
        const apiErrors = err.response.data?.errors as ApiError[] | undefined;
        if (apiErrors) {
          const fieldErrs: Record<string, string> = {};
          let global = '';
          apiErrors.forEach((e) => {
            if (e.field) fieldErrs[e.field] = e.message;
            else global = e.message;
          });
          setErrors(fieldErrs);
          setGlobalError(global);
        } else {
          setGlobalError('Đã có lỗi xảy ra, vui lòng thử lại');
        }
      } else {
        setGlobalError('Không thể kết nối đến máy chủ');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const title = type === 'ASSIGN' ? 'Tạo yêu cầu cấp phát' : 'Tạo yêu cầu trả thiết bị';

  return (
    <Modal open={open} onClose={handleClose} title={title}>
      <div className="space-y-4">
        {globalError && (
          <div role="alert" className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
            {globalError}
          </div>
        )}

        {/* Loại yêu cầu — chỉ chọn được khi không có fixedAsset */}
        {!fixedAsset && (
          <div>
            <p className="text-sm font-medium text-gray-700 mb-2">Loại yêu cầu</p>
            <div className="flex gap-3">
              {(['ASSIGN', 'RETURN'] as RequestType[]).map((t) => (
                <label
                  key={t}
                  className={`flex items-center gap-2 px-4 py-2 border rounded-lg cursor-pointer text-sm transition-colors ${
                    type === t
                      ? 'border-indigo-500 bg-indigo-50 text-indigo-700 font-medium'
                      : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                  }`}
                >
                  <input
                    type="radio"
                    name="request-type"
                    value={t}
                    checked={type === t}
                    onChange={() => { setType(t); setAssetId(''); }}
                    className="sr-only"
                  />
                  {t === 'ASSIGN' ? 'Cấp phát' : 'Trả thiết bị'}
                </label>
              ))}
            </div>
          </div>
        )}

        {/* Thiết bị */}
        <div>
          <label htmlFor="req-asset" className="block text-sm font-medium text-gray-700 mb-1">
            Thiết bị <span className="text-red-500">*</span>
          </label>
          {fixedAsset ? (
            <div className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm text-gray-700">
              <span className="font-mono text-xs text-gray-400 mr-2">{fixedAsset.code}</span>
              {fixedAsset.name}
            </div>
          ) : (
            <select
              id="req-asset"
              value={assetId}
              onChange={(e) => setAssetId(e.target.value ? Number(e.target.value) : '')}
              disabled={isLoading || loadingAssets}
              className={`w-full px-3 py-2 border rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 ${
                errors.assetId ? 'border-red-400 bg-red-50' : 'border-gray-200'
              }`}
            >
              <option value="">
                {loadingAssets
                  ? 'Đang tải...'
                  : type === 'RETURN'
                  ? '— Chọn thiết bị bạn đang giữ —'
                  : '— Chọn thiết bị —'}
              </option>
              {availableAssets.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.code} — {a.name}
                </option>
              ))}
            </select>
          )}
          {errors.assetId && <p className="mt-1 text-xs text-red-600">{errors.assetId}</p>}
        </div>

        {/* Ghi chú */}
        <div>
          <label htmlFor="req-note" className="block text-sm font-medium text-gray-700 mb-1">
            Ghi chú
          </label>
          <textarea
            id="req-note"
            rows={3}
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder="Lý do yêu cầu (không bắt buộc)..."
            disabled={isLoading}
            className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm resize-none focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>

        <div className="flex justify-end gap-3 pt-1">
          <button
            type="button"
            onClick={handleClose}
            disabled={isLoading}
            className="px-4 py-2 text-sm text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50"
          >
            Hủy
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={isLoading}
            className="px-4 py-2 text-sm text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors disabled:opacity-50"
          >
            {isLoading ? 'Đang gửi...' : 'Gửi yêu cầu'}
          </button>
        </div>
      </div>
    </Modal>
  );
}
