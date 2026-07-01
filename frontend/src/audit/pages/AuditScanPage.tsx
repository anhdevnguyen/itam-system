import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, CheckCircle, XCircle, ScanLine } from 'lucide-react';
import { Html5QrcodeScanner, Html5QrcodeScanType } from 'html5-qrcode';
import { useAuditSession, useAuditActions } from '../hooks/useAudit';
import { SkeletonLoader, StatusBadge } from '@/shared/components';

interface ScanResult {
  assetCode: string;
  assetName: string;
  success: boolean;
  message: string;
  timestamp: Date;
}

export default function AuditScanPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const sessionId = id ? Number(id) : null;
  const { session, isLoading } = useAuditSession(sessionId);
  const { scan, isLoading: scanLoading } = useAuditActions();

  const [scanResults, setScanResults] = useState<ScanResult[]>([]);
  const [manualCode, setManualCode] = useState('');
  const [scannerActive, setScannerActive] = useState(false);
  const [location, setLocation] = useState('');
  // useRef ở đây để tránh lastScanTime đưa vào deps của useCallback
  // — nếu dùng useState, mỗi lần quét sẽ tạo reference handleScan mới
  // → useEffect scanner sẽ cleanup + re-init camera sau mỗi lần quét.
  const lastScanTimeRef = useRef(0);

  const scannerRef = useRef<Html5QrcodeScanner | null>(null);
  const scannerContainerId = 'qr-scanner-container';

  const handleScan = useCallback(
    async (assetCode: string) => {
      // Tránh scan trùng trong 2 giây — dùng ref để không thêm vào deps
      const now = Date.now();
      if (now - lastScanTimeRef.current < 2000) return;
      lastScanTimeRef.current = now;

      try {
        const result = await scan(sessionId!, {
          assetCode: assetCode.trim().toUpperCase(),
          scannedLocation: location.trim() || undefined,
        });

        setScanResults((prev) => [
          {
            assetCode: result.assetCode,
            assetName: result.assetName,
            success: true,
            message: 'Quét thành công',
            timestamp: new Date(),
          },
          ...prev.slice(0, 19), // giữ tối đa 20 kết quả gần nhất
        ]);
      } catch (err: unknown) {
        const message =
          (err as { response?: { data?: { errors?: { message: string }[] } } })
            ?.response?.data?.errors?.[0]?.message ?? 'Thiết bị không hợp lệ hoặc thuộc chi nhánh khác';

        setScanResults((prev) => [
          {
            assetCode: assetCode.trim().toUpperCase(),
            assetName: '',
            success: false,
            message,
            timestamp: new Date(),
          },
          ...prev.slice(0, 19),
        ]);
      }
    },
    [sessionId, scan, location]
  );

  // Khởi tạo QR scanner
  useEffect(() => {
    if (!scannerActive) return;

    const scanner = new Html5QrcodeScanner(
      scannerContainerId,
      {
        fps: 10,
        qrbox: { width: 250, height: 250 },
        supportedScanTypes: [Html5QrcodeScanType.SCAN_TYPE_CAMERA],
        rememberLastUsedCamera: true,
      },
      false
    );

    scanner.render(
      (decodedText) => handleScan(decodedText),
      () => { /* lỗi scan — không làm gì */ }
    );

    scannerRef.current = scanner;

    return () => {
      scanner.clear().catch(() => { /* ignore cleanup error */ });
      scannerRef.current = null;
    };
  }, [scannerActive, handleScan]);

  const handleManualSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!manualCode.trim()) return;
    await handleScan(manualCode.trim());
    setManualCode('');
  };

  const formatTime = (date: Date) =>
    date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

  if (isLoading) return <div className="p-4"><SkeletonLoader rows={4} /></div>;

  if (!session || session.status === 'COMPLETED') {
    return (
      <div className="flex flex-col items-center justify-center p-8 gap-3">
        <p className="text-gray-500 text-sm">
          {!session ? 'Không tìm thấy phiên kiểm kê' : 'Phiên kiểm kê đã hoàn tất'}
        </p>
        <button
          onClick={() => navigate(`/audit/${id}`)}
          className="px-4 py-2 text-sm text-indigo-600 border border-indigo-200 rounded-lg hover:bg-indigo-50 transition-colors"
        >
          Quay lại chi tiết
        </button>
      </div>
    );
  }

  return (
    // Mobile-first layout theo docs/08-UI-UX.md mục 6
    <div className="flex flex-col gap-4 max-w-md mx-auto pb-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <button
          onClick={() => navigate(`/audit/${id}`)}
          className="p-1.5 rounded-lg text-gray-400 hover:bg-gray-100 transition-colors"
          aria-label="Quay lại"
        >
          <ArrowLeft size={20} />
        </button>
        <div>
          <h1 className="text-lg font-semibold text-gray-900">Quét QR</h1>
          <p className="text-xs text-gray-400">{session.branchName} — Phiên #{session.id}</p>
        </div>
        <div className="ml-auto">
          <StatusBadge status={session.status} />
        </div>
      </div>

      {/* Vị trí */}
      <div>
        <label htmlFor="scan-location" className="block text-sm font-medium text-gray-700 mb-1">
          Vị trí quét
        </label>
        <input
          id="scan-location"
          type="text"
          value={location}
          onChange={(e) => setLocation(e.target.value)}
          placeholder="VD: Tầng 3 - Phòng Kỹ thuật"
          className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      {/* Camera scanner */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {!scannerActive ? (
          <button
            onClick={() => setScannerActive(true)}
            className="w-full flex flex-col items-center justify-center gap-3 py-12 text-indigo-600 hover:bg-indigo-50 transition-colors"
          >
            <ScanLine size={40} className="text-indigo-400" aria-hidden="true" />
            <span className="text-sm font-medium">Bật camera để quét QR</span>
          </button>
        ) : (
          <div>
            <div id={scannerContainerId} className="w-full" />
            <div className="flex justify-center p-2 border-t border-gray-100">
              <button
                onClick={() => setScannerActive(false)}
                className="text-xs text-gray-500 hover:text-gray-700"
              >
                Tắt camera
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Nhập thủ công */}
      <form onSubmit={handleManualSubmit} className="flex gap-2">
        <input
          type="text"
          value={manualCode}
          onChange={(e) => setManualCode(e.target.value.toUpperCase())}
          placeholder="Nhập mã thiết bị thủ công (VD: HN-LAP-0001)"
          disabled={scanLoading}
          className="flex-1 px-3 py-2 border border-gray-200 rounded-lg text-sm font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        <button
          type="submit"
          disabled={scanLoading || !manualCode.trim()}
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm rounded-lg transition-colors disabled:opacity-50"
        >
          {scanLoading ? '...' : 'Quét'}
        </button>
      </form>

      {/* Kết quả scan */}
      {scanResults.length > 0 && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <div className="px-4 py-2.5 border-b border-gray-100">
            <p className="text-sm font-medium text-gray-900">
              Kết quả quét ({scanResults.length})
            </p>
          </div>
          <ul className="divide-y divide-gray-50 max-h-80 overflow-y-auto">
            {scanResults.map((r, i) => (
              <li key={i} className="flex items-start gap-3 px-4 py-3">
                {r.success ? (
                  <CheckCircle size={18} className="text-green-500 shrink-0 mt-0.5" aria-hidden="true" />
                ) : (
                  <XCircle size={18} className="text-red-500 shrink-0 mt-0.5" aria-hidden="true" />
                )}
                <div className="flex-1 min-w-0">
                  <p className={`text-sm font-medium font-mono ${r.success ? 'text-gray-900' : 'text-red-700'}`}>
                    {r.assetCode}
                  </p>
                  {r.assetName && <p className="text-xs text-gray-500 truncate">{r.assetName}</p>}
                  {!r.success && <p className="text-xs text-red-500">{r.message}</p>}
                </div>
                <span className="text-xs text-gray-400 shrink-0">{formatTime(r.timestamp)}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
