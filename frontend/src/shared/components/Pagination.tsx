import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { Pagination as PaginationMeta } from '@/lib/apiResponse.types';

interface PaginationProps {
  pagination: PaginationMeta;
  onPageChange: (page: number) => void;
  className?: string;
}

export function Pagination({ pagination, onPageChange, className }: PaginationProps) {
  const { page, totalPages, totalElements, size } = pagination;

  if (totalPages <= 1) return null;

  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  // Tính danh sách trang hiển thị (tối đa 5 nút, window quanh trang hiện tại)
  const getPageNumbers = (): (number | '...')[] => {
    if (totalPages <= 5) return Array.from({ length: totalPages }, (_, i) => i);
    const pages: (number | '...')[] = [];
    if (page <= 2) {
      pages.push(0, 1, 2, 3, '...', totalPages - 1);
    } else if (page >= totalPages - 3) {
      pages.push(0, '...', totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1);
    } else {
      pages.push(0, '...', page - 1, page, page + 1, '...', totalPages - 1);
    }
    return pages;
  };

  return (
    <div className={cn('flex items-center justify-between px-1 py-2', className)}>
      <p className="text-sm text-gray-500">
        {from}–{to} / {totalElements} kết quả
      </p>
      <div className="flex items-center gap-1">
        <button
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          aria-label="Trang trước"
          className="p-1.5 rounded-md text-gray-500 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        >
          <ChevronLeft size={16} />
        </button>

        {getPageNumbers().map((p, i) =>
          p === '...' ? (
            <span key={`ellipsis-${i}`} className="px-1 text-gray-400 text-sm">
              …
            </span>
          ) : (
            <button
              key={p}
              onClick={() => onPageChange(p as number)}
              aria-current={p === page ? 'page' : undefined}
              className={cn(
                'w-8 h-8 rounded-md text-sm transition-colors',
                p === page
                  ? 'bg-indigo-600 text-white font-medium'
                  : 'text-gray-600 hover:bg-gray-100'
              )}
            >
              {(p as number) + 1}
            </button>
          )
        )}

        <button
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          aria-label="Trang tiếp"
          className="p-1.5 rounded-md text-gray-500 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        >
          <ChevronRight size={16} />
        </button>
      </div>
    </div>
  );
}
