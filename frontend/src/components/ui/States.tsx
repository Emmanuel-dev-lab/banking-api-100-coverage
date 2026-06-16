import type { ReactNode } from 'react';
import { ApiError } from '@/lib/errors';
import { Button } from './Button';

export function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 py-12 text-ink-faint">
      <span className="size-5 animate-spin rounded-full border-2 border-line-strong border-r-forest" />
      {label && <span className="text-sm">{label}</span>}
    </div>
  );
}

export function EmptyState({
  title,
  hint,
  action,
}: {
  title: string;
  hint?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center gap-2 px-6 py-14 text-center">
      <div className="mb-1 flex size-11 items-center justify-center rounded-full border border-line bg-paper text-ink-faint">
        ∅
      </div>
      <p className="font-display text-[17px] text-ink">{title}</p>
      {hint && <p className="max-w-sm text-sm text-ink-soft">{hint}</p>}
      {action && <div className="mt-3">{action}</div>}
    </div>
  );
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const message =
    error instanceof ApiError ? error.message : 'Impossible de charger les données.';
  return (
    <div className="flex flex-col items-center gap-3 px-6 py-12 text-center">
      <div className="flex size-11 items-center justify-center rounded-full border border-danger/30 bg-danger-soft text-danger">
        !
      </div>
      <p className="text-sm text-ink-soft">{message}</p>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Réessayer
        </Button>
      )}
    </div>
  );
}
