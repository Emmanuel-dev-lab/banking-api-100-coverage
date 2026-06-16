import { formatXAF } from '@/lib/format';
import { cn } from '@/lib/cn';

interface MoneyProps {
  amount: number;
  /** Colore selon le signe (negatif = decouvert/debit). */
  colored?: boolean;
  className?: string;
}

/** Affiche un montant XAF en chasse fixe a figures tabulaires. */
export function Money({ amount, colored = false, className }: MoneyProps) {
  return (
    <span
      className={cn(
        'tnum font-mono',
        colored && amount < 0 && 'text-danger',
        colored && amount > 0 && 'text-forest-deep',
        className,
      )}
    >
      {formatXAF(amount)}
    </span>
  );
}
