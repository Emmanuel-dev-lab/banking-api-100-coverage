import type { HTMLAttributes, ReactNode } from 'react';
import { cn } from '@/lib/cn';

export function Card({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        'rounded-[14px] border border-line bg-paper-raised shadow-[0_1px_2px_rgba(40,35,20,0.04)]',
        className,
      )}
      {...rest}
    />
  );
}

export function CardHeader({
  title,
  subtitle,
  action,
}: {
  title: ReactNode;
  subtitle?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-line px-5 py-4">
      <div>
        <h3 className="text-[17px]">{title}</h3>
        {subtitle && <p className="mt-0.5 text-[13px] text-ink-soft">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}
