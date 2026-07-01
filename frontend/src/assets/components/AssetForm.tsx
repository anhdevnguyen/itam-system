import { useState, type FormEvent } from 'react';
import axios from 'axios';
import type { ApiError } from '@/lib/apiResponse.types';
import type { Asset, CreateAssetRequest, UpdateAssetRequest } from '../types/asset.types';

interface Category {
  id: number;
  code: string;
  name: string;
}

interface Branch {
  id: number;
  code: string;
  name: string;
}

interface AssetFormProps {
  /** Nếu truyền asset → chế độ sửa; không truyền → chế độ tạo mới */
  asset?: Asset;
  categories: Category[];
  branches: Branch[];
  onSuccess: (saved: Asset) => void;
  onCancel: () => void;
  isLoading?: boolean;
  onSubmit: (data: CreateAssetRequest | UpdateAssetRequest) => Promise<Asset>;
}

export function AssetForm({
  asset,
  categories,
  branches,
  onSuccess,
  onCancel,
  isLoading = false,
  onSubmit,
}: AssetFormProps) {
  const isEdit = !!asset;

  const [name, setName] = useState(asset?.name ?? '');
  const [categoryId, setCategoryId] = useState<number | ''>(asset?.categoryId ?? '');
  const [branchId, setBranchId] = useState<number | ''>(asset?.branchId ?? '');
  const [purchaseDate, setPurchaseDate] = useState(asset?.purchaseDate ?? '');
  const [value, setValue] = useState(asset?.value ? String(asset.value) : '');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [globalError, setGlobalError] = useState('');

  const validate = () => {
    const errs: Record<string, string> = {};
    if (!name.trim()) errs.name = 'Tên thiết bị không được để trống';
    if (!categoryId) errs.categoryId = 'Vui lòng chọn danh mục';
    if (!branchId) errs.branchId = 'Vui lòng chọn chi nhánh';
    if (!purchaseDate) errs.purchaseDate = 'Ngày mua không được để trống';
    else if (new Date(purchaseDate) > new Date()) errs.purchaseDate = 'Ngày mua không được là ngày tương lai';
    const numVal = Number(value);
    if (!value || isNaN(numVal) || numVal <= 0) errs.value = 'Giá trị phải lớn hơn 0';
    return errs;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return;
    }
    setErrors({});
    setGlobalError('');

    const data: CreateAssetRequest | UpdateAssetRequest = isEdit
      ? { name: name.trim(), categoryId: Number(categoryId), purchaseDate, value: Number(value) }
      : {
          name: name.trim(),
          categoryId: Number(categoryId),
          branchId: Number(branchId),
          purchaseDate,
          value: Number(value),
        };

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

      {/* Tên thiết bị */}
      <div>
        <label htmlFor="asset-name" className="block text-sm font-medium text-gray-700 mb-1">
          Tên thiết bị <span className="text-red-500">*</span>
        </label>
        <input
          id="asset-name"
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="VD: Dell Latitude 5440"
          disabled={isLoading}
          aria-required="true"
          aria-invalid={!!errors.name}
          className={inputClass('name')}
        />
        {errors.name && <p className="mt-1 text-xs text-red-600">{errors.name}</p>}
      </div>

      {/* Danh mục */}
      <div>
        <label htmlFor="asset-category" className="block text-sm font-medium text-gray-700 mb-1">
          Danh mục <span className="text-red-500">*</span>
        </label>
        <select
          id="asset-category"
          value={categoryId}
          onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : '')}
          disabled={isLoading}
          aria-required="true"
          className={inputClass('categoryId') + ' bg-white'}
        >
          <option value="">— Chọn danh mục —</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        {errors.categoryId && <p className="mt-1 text-xs text-red-600">{errors.categoryId}</p>}
      </div>

      {/* Chi nhánh — chỉ hiện khi tạo mới */}
      {!isEdit && (
        <div>
          <label htmlFor="asset-branch" className="block text-sm font-medium text-gray-700 mb-1">
            Chi nhánh <span className="text-red-500">*</span>
          </label>
          <select
            id="asset-branch"
            value={branchId}
            onChange={(e) => setBranchId(e.target.value ? Number(e.target.value) : '')}
            disabled={isLoading}
            aria-required="true"
            className={inputClass('branchId') + ' bg-white'}
          >
            <option value="">— Chọn chi nhánh —</option>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
              </option>
            ))}
          </select>
          {errors.branchId && <p className="mt-1 text-xs text-red-600">{errors.branchId}</p>}
        </div>
      )}

      <div className="grid grid-cols-2 gap-3">
        {/* Ngày mua */}
        <div>
          <label htmlFor="asset-purchase-date" className="block text-sm font-medium text-gray-700 mb-1">
            Ngày mua <span className="text-red-500">*</span>
          </label>
          <input
            id="asset-purchase-date"
            type="date"
            value={purchaseDate}
            onChange={(e) => setPurchaseDate(e.target.value)}
            max={new Date().toISOString().split('T')[0]}
            disabled={isLoading}
            aria-required="true"
            className={inputClass('purchaseDate')}
          />
          {errors.purchaseDate && (
            <p className="mt-1 text-xs text-red-600">{errors.purchaseDate}</p>
          )}
        </div>

        {/* Giá trị */}
        <div>
          <label htmlFor="asset-value" className="block text-sm font-medium text-gray-700 mb-1">
            Giá trị (VNĐ) <span className="text-red-500">*</span>
          </label>
          <input
            id="asset-value"
            type="number"
            min="1"
            step="1000"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            placeholder="VD: 22000000"
            disabled={isLoading}
            aria-required="true"
            className={inputClass('value')}
          />
          {errors.value && <p className="mt-1 text-xs text-red-600">{errors.value}</p>}
        </div>
      </div>

      {/* Actions */}
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
          className="px-4 py-2 text-sm text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isLoading ? 'Đang lưu...' : isEdit ? 'Cập nhật' : 'Tạo thiết bị'}
        </button>
      </div>
    </form>
  );
}
