import { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft, Edit2, Trash2, RotateCcw, History, Image, QrCode } from 'lucide-react';
import { useAsset, useAssetAssignmentHistory, useAssetActions } from '../hooks/useAssets';
import { AssetForm } from '../components/AssetForm';
import { ForceReturnModal } from '../components/ForceReturnModal';
import { SkeletonLoader, EmptyState, StatusBadge, Modal, ConfirmDialog } from '@/shared/components';
import { useAuth } from '@/auth/hooks/useAuth.tsx';
import apiClient from '@/lib/apiClient';
import type { ApiResponse } from '@/lib/apiResponse.types';
import type { Asset, UpdateAssetRequest } from '../types/asset.types';

interface Category { id: number; code: string; name: string; }
interface Branch { id: number; code: string; name: string; }

type Tab = 'info' | 'history' | 'images' | 'qr';

export default function AssetDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { user } = useAuth();

  const assetId = id ? Number(id) : null;
  const isItStaffOrAdmin = user?.role === 'ADMIN' || user?.role === 'IT_STAFF';

  const { asset, isLoading, error, refetch } = useAsset(assetId);
  const { history, isLoading: histLoading } = useAssetAssignmentHistory(assetId);
  const { update, softDelete, forceReturn, uploadImage, isLoading: actionLoading } = useAssetActions();

  const [activeTab, setActiveTab] = useState<Tab>(
    (searchParams.get('tab') as Tab) ?? 'info'
  );
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [showForceReturn, setShowForceReturn] = useState(false);
  const [qrUrl, setQrUrl] = useState('');

  // Dropdowns
  const [categories, setCategories] = useState<Category[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);

  useEffect(() => {
    const loadDropdowns = async () => {
      try {
        const [catRes, branchRes] = await Promise.all([
          apiClient.get<ApiResponse<Category[]>>('/categories?page=0&size=100'),
          apiClient.get<ApiResponse<Branch[]>>('/branches?page=0&size=100'),
        ]);
        setCategories(catRes.data.data ?? []);
        setBranches(branchRes.data.data ?? []);
      } catch { /* silent */ }
    };
    loadDropdowns();
  }, []);

  // Revoke object URL khi unmount để tránh memory leak
  useEffect(() => {
    return () => {
      if (qrUrl.startsWith('blob:')) URL.revokeObjectURL(qrUrl);
    };
  }, [qrUrl]);

  useEffect(() => {
    if (activeTab === 'qr' && assetId && !qrUrl) {
      // Backend trả raw PNG bytes — cần responseType: 'blob'
      apiClient
        .get(`/assets/${assetId}/qr-code`, { responseType: 'blob' })
        .then((res) => {
          const objectUrl = URL.createObjectURL(res.data as Blob);
          setQrUrl(objectUrl);
        })
        .catch(() => setQrUrl(''));
    }
  }, [activeTab, assetId, qrUrl]);

  const handleUpdate = async (data: UpdateAssetRequest) => {
    return update(assetId!, data) as Promise<Asset>;
  };

  const handleDelete = async () => {
    await softDelete(assetId!);
    navigate('/assets', { replace: true });
  };

  const handleForceReturn = async (reason: string) => {
    await forceReturn(assetId!, { reason });
    setShowForceReturn(false);
    refetch();
  };

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !assetId) return;
    await uploadImage(assetId, file);
    refetch();
    e.target.value = '';
  };

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });

  if (isLoading) {
    return (
      <div className="p-6">
        <SkeletonLoader rows={8} />
      </div>
    );
  }

  if (error || !asset) {
    return (
      <EmptyState
        message={error || 'Không tìm thấy thiết bị'}
        action={
          <button
            onClick={() => navigate('/assets')}
            className="px-4 py-2 text-sm text-indigo-600 border border-indigo-200 rounded-lg hover:bg-indigo-50 transition-colors"
          >
            Quay lại danh sách
          </button>
        }
      />
    );
  }

  const TABS: { key: Tab; label: string; icon: React.ElementType }[] = [
    { key: 'info', label: 'Thông tin', icon: Edit2 },
    { key: 'history', label: 'Lịch sử cấp phát', icon: History },
    { key: 'images', label: 'Hình ảnh', icon: Image },
    { key: 'qr', label: 'QR Code', icon: QrCode },
  ];

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <button
            onClick={() => navigate('/assets')}
            className="mt-0.5 p-1.5 rounded-lg text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors"
            aria-label="Quay lại danh sách"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <div className="flex items-center gap-2 mb-1">
              <h1 className="text-2xl font-semibold text-gray-900">{asset.name}</h1>
              <StatusBadge status={asset.status} />
            </div>
            <p className="text-sm text-gray-400 font-mono">{asset.code}</p>
          </div>
        </div>

        {isItStaffOrAdmin && (
          <div className="flex items-center gap-2 shrink-0">
            {asset.status === 'ASSIGNED' && (
              <button
                onClick={() => setShowForceReturn(true)}
                className="flex items-center gap-1.5 px-3 py-2 text-sm text-amber-700 border border-amber-200 rounded-lg hover:bg-amber-50 transition-colors"
              >
                <RotateCcw size={15} aria-hidden="true" />
                Thu hồi
              </button>
            )}
            <button
              onClick={() => setShowEditModal(true)}
              className="flex items-center gap-1.5 px-3 py-2 text-sm text-gray-700 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <Edit2 size={15} aria-hidden="true" />
              Sửa
            </button>
            <button
              onClick={() => setShowDeleteDialog(true)}
              className="flex items-center gap-1.5 px-3 py-2 text-sm text-red-600 border border-red-200 rounded-lg hover:bg-red-50 transition-colors"
            >
              <Trash2 size={15} aria-hidden="true" />
              Xóa
            </button>
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="flex border-b border-gray-200 overflow-x-auto">
          {TABS.map(({ key, label, icon: Icon }) => (
            <button
              key={key}
              onClick={() => setActiveTab(key)}
              className={`flex items-center gap-2 px-4 py-3 text-sm font-medium whitespace-nowrap transition-colors border-b-2 ${
                activeTab === key
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <Icon size={15} aria-hidden="true" />
              {label}
            </button>
          ))}
        </div>

        <div className="p-5">
          {/* Tab: Thông tin */}
          {activeTab === 'info' && (
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-4">
              <InfoRow label="Mã thiết bị" value={asset.code} mono />
              <InfoRow label="Tên thiết bị" value={asset.name} />
              <InfoRow label="Danh mục" value={asset.categoryName} />
              <InfoRow label="Chi nhánh" value={asset.branchName} />
              <InfoRow label="Trạng thái" value={<StatusBadge status={asset.status} />} />
              <InfoRow label="Người đang giữ" value={asset.assignedToName ?? '—'} />
              <InfoRow label="Ngày mua" value={formatDate(asset.purchaseDate)} />
              <InfoRow label="Giá trị" value={formatCurrency(asset.value)} />
              <InfoRow label="Ngày tạo" value={formatDate(asset.createdAt)} />
              <InfoRow label="Cập nhật lần cuối" value={formatDate(asset.updatedAt)} />
            </dl>
          )}

          {/* Tab: Lịch sử cấp phát */}
          {activeTab === 'history' && (
            <div>
              {histLoading ? (
                <SkeletonLoader rows={4} />
              ) : history.length === 0 ? (
                <EmptyState message="Chưa có lịch sử cấp phát" />
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-200">
                      <th className="text-left py-2 text-xs font-medium text-gray-500 uppercase">Nhân viên</th>
                      <th className="text-left py-2 text-xs font-medium text-gray-500 uppercase">Ngày cấp</th>
                      <th className="text-left py-2 text-xs font-medium text-gray-500 uppercase">Ngày trả</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {history.map((h) => (
                      <tr key={h.id}>
                        <td className="py-2.5 text-gray-900">{h.employeeName}</td>
                        <td className="py-2.5 text-gray-600">{formatDate(h.assignedAt)}</td>
                        <td className="py-2.5">
                          {h.returnedAt ? (
                            <span className="text-gray-600">{formatDate(h.returnedAt)}</span>
                          ) : (
                            <span className="text-green-600 text-xs font-medium">Đang giữ</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}

          {/* Tab: Hình ảnh */}
          {activeTab === 'images' && (
            <div>
              {isItStaffOrAdmin && (asset.images?.length ?? 0) < 5 && (
                <div className="mb-4">
                  <label
                    htmlFor="image-upload"
                    className="inline-flex items-center gap-2 px-3 py-2 text-sm text-indigo-700 border border-indigo-200 rounded-lg cursor-pointer hover:bg-indigo-50 transition-colors"
                  >
                    <Image size={15} aria-hidden="true" />
                    Tải lên ảnh ({asset.images?.length ?? 0}/5)
                  </label>
                  <input
                    id="image-upload"
                    type="file"
                    accept="image/*"
                    className="sr-only"
                    onChange={handleImageUpload}
                    disabled={actionLoading}
                  />
                  <p className="mt-1 text-xs text-gray-400">Tối đa 5MB mỗi ảnh</p>
                </div>
              )}

              {!asset.images || asset.images.length === 0 ? (
                <EmptyState message="Chưa có hình ảnh" />
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
                  {asset.images.map((img) => (
                    <a key={img.id} href={img.url} target="_blank" rel="noreferrer">
                      <img
                        src={img.url}
                        alt={`Ảnh thiết bị ${asset.name}`}
                        className="w-full aspect-square object-cover rounded-lg border border-gray-200 hover:opacity-90 transition-opacity"
                        loading="lazy"
                      />
                    </a>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Tab: QR Code */}
          {activeTab === 'qr' && (
            <div className="flex flex-col items-center gap-4 py-4">
              {qrUrl ? (
                <>
                  <img
                    src={qrUrl}
                    alt={`QR code cho ${asset.code}`}
                    className="w-48 h-48 border border-gray-200 rounded-xl p-2"
                  />
                  <p className="text-sm text-gray-500">Mã: {asset.code}</p>
                  <a
                    href={qrUrl}
                    download={`${asset.code}-qr.png`}
                    className="px-4 py-2 text-sm text-indigo-600 border border-indigo-200 rounded-lg hover:bg-indigo-50 transition-colors"
                  >
                    Tải xuống QR
                  </a>
                </>
              ) : (
                <EmptyState message="Không thể tải QR code" />
              )}
            </div>
          )}
        </div>
      </div>

      {/* Modal sửa */}
      <Modal open={showEditModal} onClose={() => setShowEditModal(false)} title="Sửa thông tin thiết bị">
        <AssetForm
          asset={asset}
          categories={categories}
          branches={branches}
          onSubmit={handleUpdate}
          onSuccess={() => { setShowEditModal(false); refetch(); }}
          onCancel={() => setShowEditModal(false)}
          isLoading={actionLoading}
        />
      </Modal>

      {/* Modal force return */}
      {showForceReturn && (
        <ForceReturnModal
          asset={asset}
          open={showForceReturn}
          onClose={() => setShowForceReturn(false)}
          onConfirm={handleForceReturn}
          isLoading={actionLoading}
        />
      )}

      {/* Confirm xóa */}
      <ConfirmDialog
        open={showDeleteDialog}
        onClose={() => setShowDeleteDialog(false)}
        onConfirm={handleDelete}
        title="Xóa thiết bị"
        message={`Bạn có chắc muốn xóa thiết bị "${asset.name}"? Hành động này không thể hoàn tác.`}
        confirmLabel="Xóa"
        variant="danger"
        isLoading={actionLoading}
      />
    </div>
  );
}

// ── InfoRow helper ────────────────────────────────────────────────────────────
function InfoRow({
  label,
  value,
  mono = false,
}: {
  label: string;
  value: React.ReactNode;
  mono?: boolean;
}) {
  return (
    <div>
      <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-0.5">{label}</dt>
      <dd className={`text-sm text-gray-900 ${mono ? 'font-mono' : ''}`}>{value}</dd>
    </div>
  );
}
