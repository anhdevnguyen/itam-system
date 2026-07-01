import { Component, type ReactNode, type ErrorInfo } from 'react';
import { AlertTriangle } from 'lucide-react';

interface ErrorBoundaryProps {
  children: ReactNode;
  /** Tên trang/route để hiển thị trong thông báo lỗi (tùy chọn) */
  pageName?: string;
  /**
   * Thay đổi giá trị này (vd: pathname) sẽ tự động reset lỗi.
   * Dùng khi bọc nhiều route — đảm bảo lỗi ở route A không "lây" sang route B.
   */
  resetKey?: string;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  /** Lưu resetKey tại lúc xảy ra lỗi để so sánh */
  errorResetKey?: string;
}

/**
 * ErrorBoundary — Bắt runtime error từ bất kỳ component con nào.
 * Bao gồm lỗi import của lazy-loaded routes (chunk load fail, v.v.)
 *
 * Tự động reset khi `resetKey` thay đổi (vd: pathname thay đổi khi navigate).
 * Dùng như class component vì React chưa có hooks equivalent cho componentDidCatch.
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    return { hasError: true, error };
  }

  static getDerivedStateFromProps(
    props: ErrorBoundaryProps,
    state: ErrorBoundaryState,
  ): Partial<ErrorBoundaryState> | null {
    // Khi resetKey thay đổi so với lúc lỗi xảy ra → reset boundary
    if (state.hasError && props.resetKey !== state.errorResetKey) {
      return { hasError: false, error: null, errorResetKey: props.resetKey };
    }
    // Khi lỗi vừa xảy ra → lưu resetKey hiện tại để so sánh sau
    if (state.hasError && state.errorResetKey === undefined) {
      return { errorResetKey: props.resetKey };
    }
    return null;
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  handleReload = () => {
    this.setState({ hasError: false, error: null, errorResetKey: undefined });
    window.location.reload();
  };

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    const isChunkError =
      this.state.error?.message?.includes('Failed to fetch dynamically imported module') ||
      this.state.error?.message?.includes('Loading chunk') ||
      this.state.error?.name === 'ChunkLoadError';

    return (
      <div className="flex flex-col items-center justify-center min-h-[50vh] p-8 gap-4">
        <div className="w-12 h-12 rounded-full bg-red-50 flex items-center justify-center">
          <AlertTriangle size={24} className="text-red-500" aria-hidden="true" />
        </div>
        <div className="text-center max-w-sm">
          <h2 className="text-base font-semibold text-gray-900 mb-1">
            {this.props.pageName ? `Không thể tải trang ${this.props.pageName}` : 'Đã xảy ra lỗi'}
          </h2>
          <p className="text-sm text-gray-500">
            {isChunkError
              ? 'Không thể tải module. Ứng dụng có thể đã được cập nhật — vui lòng tải lại trang.'
              : 'Một lỗi không mong muốn đã xảy ra. Vui lòng thử lại.'}
          </p>
        </div>
        <button
          onClick={this.handleReload}
          className="px-4 py-2 text-sm text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors"
        >
          Tải lại trang
        </button>
        {import.meta.env.DEV && this.state.error && (
          <pre className="mt-2 p-3 bg-gray-100 rounded-lg text-xs text-red-700 max-w-lg overflow-auto max-h-40">
            {this.state.error.message}
          </pre>
        )}
      </div>
    );
  }
}
