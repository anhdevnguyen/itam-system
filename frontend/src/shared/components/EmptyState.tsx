import { Inbox } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { ReactNode } from 'react';

interface EmptyStateProps {
  message?: string;
  description?: string;
  action?: ReactNode;
  className?: string;
}

export function EmptyState({
  message = 'Không có dữ liệu',
  description,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div className={cn('flex flex-col items-center justify-center py-16 text-gray-500', className)}>
      <Inbox className="w-12 h-12 mb-3 text-gray-300" aria-hidden="true" />
      <p className="text-sm font-medium text-gray-600">{message}</p>
      {description && <p className="text-xs text-gray-400 mt-1 text-center max-w-xs">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
