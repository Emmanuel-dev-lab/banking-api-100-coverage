import type { ReactNode } from 'react';

export function PageHeader({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <header className="mb-7 flex flex-wrap items-end justify-between gap-4">
      <div>
        {eyebrow && (
          <p className="mb-1.5 text-[12px] font-semibold uppercase tracking-[0.14em] text-brass">
            {eyebrow}
          </p>
        )}
        <h1 className="text-[30px] leading-none text-ink">{title}</h1>
        {description && <p className="mt-2 max-w-xl text-[15px] text-ink-soft">{description}</p>}
      </div>
      {action}
    </header>
  );
}
