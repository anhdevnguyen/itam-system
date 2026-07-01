import { useState, type FormEvent } from 'react';
import axios from 'axios';
import type { ApiError } from '@/lib/apiResponse.types';
import type {
  MaintenanceRecord,
  CreateMaintenanceRequest,
  UpdateMaintenanceRequest,
  MaintenanceType,
  MaintenanceStatus,
} from '../types/maintenance.types';

const TYPE_OPTIONS: { value: MaintenanceType; label: string }[] = [
  { value: 'WARRANTY', label: 'Bảo hành' },
  { value: 'REPAIR', label: 'Sửa chữa' },
  { value: 'PERIODIC', label: 'Định kỳ' },
];

const STATUS_OPTIONS: { value: MaintenanceStatus; label: string }[] = [
  { value: 'SCHEDULED', label: 'Lên lịch' },
  { value: 'IN_PROGRESS', label: 'Đang thực hiện' },
  { value: 'COMPLETED', label: 'Hoàn thành' },
  { value: 'CANCELLED', label: 'Đã hủy' },
];

interface AssetOption {
  id: number;
  code: string;
  name: string;
}

interface MaintenanceFormProps {
  record?: MaintenanceRecord;
  assets: AssetOption[];
  onSuccess: (saved: MaintenanceRecord) => void;
  onCancel: () => void;
  isLoading?: boolean;
  onSubmit: (data: CreateMaintenanceRequest | UpdateMaintenanceRequest) => Promise<MaintenanceRecord>;
}

export function MaintenanceForm({
  record,
  assets,
  onSuccess,
  onCancel,
  isLoading = false,
  onSubmit,
}: MaintenanceFormProps) {
  const isEdit = !!record;

  const [assetId, setAssetId] = useState<number | ''>(record?.assetId ?? '');
  const [type, setType] = useState<MaintenanceType>(record?.type ?? 'REPAIR');
  const [status, setStatus] = useState<MaintenanceStatus>(record?.status ?? 'SCHEDULED');
  const [description, setDescription] = useState(record?.description ?? '');
  const [scheduledDate, setScheduledDate] = useState(record?.scheduledDate ?? '');
  const [completedDate, setCompletedDate] = useState(record?.completedDate ?? '');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [globalError, setGlobalError] = useState('');

  const validate = () => {
    const errs: Record<string, string> = {};
    if (!isEdit && !assetId) errs.assetId = 'Vui lòng chọn thiết bị';
    return errs;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }
    setErrors({});
    setGlobalError('');

    const data = isEdit
      ? ({
          type,
          status,
          description: description.trim() || undefined,
          scheduledDate: scheduledDate || undefined,
          completedDate: completedDate || undefined,
        } as UpdateMaintenanceRequest)
      : ({
          assetId: Number(assetId),
          type,
          description: description.trim() || undefined,
          scheduledDate: scheduledDate || undefined,
        } as CreateMaintenanceRequest);

    try {
      const saved = await onSubmit(data);
      onSuccess(saved);
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
    }
  };

  const inputClass = (field: string) =>
    `w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 ${
      errors[field] ? 'border-red-400 bg-red-50' : 'border-gray-200'
    }`;

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-4">
      {globalError && (
        <div role="alert" className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
          {globalError}
        </div>
      )}

      {/* Thiết bị — chỉ khi tạo mới */}
      {!isEdit && (
        <div>
          <label htmlFor="maint-asset" className="block text-sm font-medium text-gray-700 mb-1">
            Thiết bị <span className="text-red-500">*</span>
          </label>
          <select
            id="maint-asset"
            value={assetId}
            onChange={(e) => setAssetId(e.target.value ? Number(e.target.value) : '')}
            disabled={isLoading}
            className={inputClass('assetId') + ' bg-white'}
          >
            <option value="">— Chọn thiết bị —</option>
            {assets.map((a) => (
              <option key={a.id} value={a.id}>{a.code} — {a.name}</option>
            ))}
          </select>
          {errors.assetId && <p className="mt-1 text-xs text-red-600">{errors.assetId}</p>}
        </div>
      )}

      <div className="grid grid-cols-2 gap-3">
        {/* Loại */}
        <div>
          <label htmlFor="maint-type" className="block text-sm font-medium text-gray-700 mb-1">Loại</label>
          <select
            id="maint-type"
            value={type}
            onChange={(e) => setType(e.target.value as MaintenanceType)}
            disabled={isLoading}
            className={inputClass('type') + ' bg-white'}
          >
            {TYPE_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
        </div>

        {/* Trạng thái — chỉ khi sửa */}
        {isEdit && (
          <div>
            <label htmlFor="maint-status" className="block text-sm font-medium text-gray-700 mb-1">Trạng thái</label>
            <select
              id="maint-status"
              value={status}
              onChange={(e) => setStatus(e.target.value as MaintenanceStatus)}
              disabled={isLoading}
              className={inputClass('status') + ' bg-white'}
            >
              {STATUS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Mô tả */}
      <div>
        <label htmlFor="maint-desc" className="block text-sm font-medium text-gray-700 mb-1">Mô tả</label>
        <textarea
          id="maint-desc"
          rows={3}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Mô tả vấn đề hoặc công việc bảo trì..."
          disabled={isLoading}
          className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm resize-none focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      <div className="grid grid-cols-2 gap-3">
        {/* Ngày lên lịch */}
        <div>
          <label htmlFor="maint-sched" className="block text-sm font-medium text-gray-700 mb-1">Ngày lên lịch</label>
          <input
            id="maint-sched"
            type="date"
            value={scheduledDate}
            onChange={(e) => setScheduledDate(e.target.value)}
            disabled={isLoading}
            className={inputClass('scheduledDate')}
          />
        </div>

        {/* Ngày hoàn thành — chỉ khi sửa và status = COMPLETED */}
        {isEdit && (
          <div>
            <label htmlFor="maint-completed" className="block text-sm font-medium text-gray-700 mb-1">Ngày hoàn thành</label>
            <input
              id="maint-completed"
              type="date"
              value={completedDate}
              onChange={(e) => setCompletedDate(e.target.value)}
              disabled={isLoading}
              className={inputClass('completedDate')}
            />
          </div>
        )}
      </div>

      <div className="flex justify-end gap-3 pt-2">
        <button
          type="button"
          onClick={onCancel}
          disabled={isLoading}
          className="px-4 py-2 text-sm text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50"
        >
          Hủy
        </button>
        <button
          type="submit"
          disabled={isLoading}
          className="px-4 py-2 text-sm text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors disabled:opacity-50"
        >
          {isLoading ? 'Đang lưu...' : isEdit ? 'Cập nhật' : 'Tạo bản ghi'}
        </button>
      </div>
    </form>
  );
}
