import type { ReactNode } from 'react';
import { Card } from './ui/Card';

export function StatCard({
  label,
  value,
  foot,
}: {
  label: string;
  value: ReactNode;
  foot?: ReactNode;
}) {
  return (
    <Card className="p-5">
      <p className="text-[12px] font-semibold uppercase tracking-[0.08em] text-ink-faint">{label}</p>
      <p className="mt-2 font-mono text-[26px] leading-none tracking-tight text-ink tnum">{value}</p>
      {foot && <p className="mt-2 text-[13px] text-ink-soft">{foot}</p>}
    </Card>
  );
}
