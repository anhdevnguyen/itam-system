import { cn } from '@/lib/utils';

interface SkeletonLoaderProps {
  rows?: number;
  className?: string;
}

/** Skeleton loading cho danh sách bảng */
export function SkeletonLoader({ rows = 5, className }: SkeletonLoaderProps) {
  return (
    <div className={cn('space-y-3', className)} aria-busy="true" aria-label="Đang tải dữ liệu...">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="h-12 bg-gray-100 rounded-md animate-pulse" />
      ))}
    </div>
  );
}

/** Skeleton cho 1 dòng text */
export function SkeletonText({ width = 'w-full', className }: { width?: string; className?: string }) {
  return <div className={cn('h-4 bg-gray-100 rounded animate-pulse', width, className)} />;
}

/** Skeleton cho card */
export function SkeletonCard({ className }: { className?: string }) {
  return (
    <div className={cn('p-4 border border-gray-200 rounded-lg space-y-3', className)}>
      <div className="h-5 w-1/3 bg-gray-100 rounded animate-pulse" />
      <div className="h-4 w-2/3 bg-gray-100 rounded animate-pulse" />
      <div className="h-4 w-1/2 bg-gray-100 rounded animate-pulse" />
    </div>
  );
}
