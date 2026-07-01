import { useState } from 'react';
import { Modal } from '@/shared/components';
import type { Asset } from '../types/asset.types';

interface ForceReturnModalProps {
  asset: Asset;
  open: boolean;
  onClose: () => void;
  onConfirm: (reason: string) => Promise<void>;
  isLoading?: boolean;
}

export function ForceReturnModal({
  asset,
  open,
  onClose,
  onConfirm,
  isLoading = false,
}: ForceReturnModalProps) {
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async () => {
    if (!reason.trim()) {
      setError('Lý do thu hồi là bắt buộc');
      return;
    }
    setError('');
    await onConfirm(reason.trim());
    setReason('');
  };

  const handleClose = () => {
    setReason('');
    setError('');
    onClose();
  };

  return (
    <Modal open={open} onClose={handleClose} title="Thu hồi thiết bị (bắt buộc)">
      <div className="space-y-4">
        <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg">
          <p className="text-sm text-amber-800">
            Bạn đang thu hồi <strong>{asset.code}</strong> — {asset.name} từ{' '}
            <strong>{asset.assignedToName}</strong>. Thao tác này bỏ qua workflow duyệt thông thường.
          </p>
        </div>

        <div>
          <label htmlFor="force-return-reason" className="block text-sm font-medium text-gray-700 mb-1">
            Lý do thu hồi <span className="text-red-500">*</span>
          </label>
          <textarea
            id="force-return-reason"
            rows={3}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="VD: Nhân viên nghỉ việc, thu hồi gấp..."
            disabled={isLoading}
            aria-required="true"
            aria-invalid={!!error}
            className={`w-full px-3 py-2 border rounded-lg text-sm resize-none focus:outline-none focus:ring-2 focus:ring-indigo-500 ${
              error ? 'border-red-400 bg-red-50' : 'border-gray-200'
            }`}
          />
          {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
        </div>

        <div className="flex justify-end gap-3">
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
            className="px-4 py-2 text-sm text-white bg-amber-600 hover:bg-amber-700 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? 'Đang xử lý...' : 'Xác nhận thu hồi'}
          </button>
        </div>
      </div>
    </Modal>
  );
}
