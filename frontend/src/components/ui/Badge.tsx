import type { AccountStatus, AccountType, LoanStatus, TransactionType } from '@/api/types';
import { txnLabel } from '@/lib/format';
import { cn } from '@/lib/cn';

function Pill({ children, className }: { children: React.ReactNode; className: string }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[11.5px] font-semibold uppercase tracking-[0.04em]',
        className,
      )}
    >
      {children}
    </span>
  );
}

const accountStatus: Record<AccountStatus, { label: string; cls: string }> = {
  ACTIVE: { label: 'Actif', cls: 'bg-forest-soft text-forest-deep' },
  FROZEN: { label: 'Gelé', cls: 'bg-warn-soft text-warn' },
  CLOSED: { label: 'Fermé', cls: 'bg-line text-ink-soft' },
};

export function AccountStatusBadge({ status }: { status: AccountStatus }) {
  const s = accountStatus[status];
  return (
    <Pill className={s.cls}>
      <span className="size-1.5 rounded-full bg-current opacity-70" />
      {s.label}
    </Pill>
  );
}

const loanStatus: Record<LoanStatus, { label: string; cls: string }> = {
  ACTIVE: { label: 'En cours', cls: 'bg-brass-soft text-brass' },
  PAID_OFF: { label: 'Soldé', cls: 'bg-forest-soft text-forest-deep' },
};

export function LoanStatusBadge({ status }: { status: LoanStatus }) {
  const s = loanStatus[status];
  return <Pill className={s.cls}>{s.label}</Pill>;
}

export function AccountTypeTag({ type }: { type: AccountType }) {
  return (
    <span className="text-[12px] font-semibold uppercase tracking-[0.08em] text-ink-faint">
      {type === 'CURRENT' ? 'Courant' : 'Épargne'}
    </span>
  );
}

const credits: TransactionType[] = ['DEPOSIT', 'TRANSFER_IN', 'LOAN_DISBURSEMENT', 'INTEREST'];

/** Marqueur "en retard" pour un pret dont une echeance est echue. */
export function LateBadge() {
  return (
    <Pill className="bg-danger-soft text-danger">
      <span className="size-1.5 rounded-full bg-current" />
      En retard
    </Pill>
  );
}

export function TxnTypeBadge({ type }: { type: TransactionType }) {
  const credit = credits.includes(type);
  return (
    <Pill className={credit ? 'bg-forest-soft text-forest-deep' : 'bg-line text-ink-soft'}>
      {txnLabel(type)}
    </Pill>
  );
}
