import { useState } from 'react';
import { Modal } from '@/shared/components';

interface RejectRequestModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (reason: string) => Promise<void>;
  isLoading?: boolean;
}

export function RejectRequestModal({
  open,
  onClose,
  onConfirm,
  isLoading = false,
}: RejectRequestModalProps) {
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async () => {
    if (!reason.trim()) {
      setError('Lý do từ chối là bắt buộc');
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
    <Modal open={open} onClose={handleClose} title="Từ chối yêu cầu" size="sm">
      <div className="space-y-4">
        <div>
          <label htmlFor="reject-reason" className="block text-sm font-medium text-gray-700 mb-1">
            Lý do từ chối <span className="text-red-500">*</span>
          </label>
          <textarea
            id="reject-reason"
            rows={3}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Nhập lý do từ chối yêu cầu..."
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
            className="px-4 py-2 text-sm text-white bg-red-600 hover:bg-red-700 rounded-lg transition-colors disabled:opacity-50"
          >
            {isLoading ? 'Đang xử lý...' : 'Xác nhận từ chối'}
          </button>
        </div>
      </div>
    </Modal>
  );
}
