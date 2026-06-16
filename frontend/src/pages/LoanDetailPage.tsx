import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useLoan, useRepayLoan } from '@/api/hooks';
import { useAuth } from '@/auth/AuthContext';
import { PageHeader } from '@/components/PageHeader';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Money } from '@/components/ui/Money';
import { LoanStatusBadge } from '@/components/ui/Badge';
import { Spinner, ErrorState } from '@/components/ui/States';
import { AmountModal } from '@/components/modals/AmountModal';
import { useToast } from '@/components/Toast';
import { formatDate } from '@/lib/format';
import { cn } from '@/lib/cn';

export function LoanDetailPage() {
  const { id = '' } = useParams();
  const { isAdmin } = useAuth();
  const toast = useToast();
  const loan = useLoan(id);
  const repay = useRepayLoan(id);
  const [open, setOpen] = useState(false);

  if (loan.isLoading) return <Spinner label="Chargement du prêt…" />;
  if (loan.isError || !loan.data) return <ErrorState error={loan.error} onRetry={loan.refetch} />;

  const l = loan.data;
  const repaid = l.principal - l.outstandingPrincipal;
  const pct = l.principal > 0 ? Math.round((repaid / l.principal) * 100) : 0;
  const active = l.status === 'ACTIVE';

  return (
    <div className="animate-rise">
      <Link
        to={isAdmin ? '/admin/loans' : '/loans'}
        className="mb-4 inline-block text-[13px] text-ink-soft hover:text-forest-deep"
      >
        ← Retour
      </Link>

      <PageHeader
        eyebrow="Prêt"
        title="Détail et échéancier"
        action={
          active ? <Button onClick={() => setOpen(true)}>Rembourser</Button> : undefined
        }
      />

      <div className="mb-6 grid gap-4 lg:grid-cols-3">
        <Card className="p-5">
          <div className="flex items-center justify-between">
            <p className="text-[12px] font-semibold uppercase tracking-[0.08em] text-ink-faint">
              Capital restant
            </p>
            <LoanStatusBadge status={l.status} />
          </div>
          <Money amount={l.outstandingPrincipal} className="mt-2 text-[26px]" />
          <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-line">
            <div className="h-full rounded-full bg-forest" style={{ width: `${pct}%` }} />
          </div>
          <p className="mt-1.5 text-[12.5px] text-ink-soft">{pct}% remboursé</p>
        </Card>
        <Card className="p-5">
          <p className="text-[12px] font-semibold uppercase tracking-[0.08em] text-ink-faint">
            Capital initial
          </p>
          <Money amount={l.principal} className="mt-2 text-[26px]" />
        </Card>
        <Card className="p-5">
          <p className="text-[12px] font-semibold uppercase tracking-[0.08em] text-ink-faint">
            Compte lié
          </p>
          <p className="mt-2 font-mono text-[15px] text-ink">{l.accountId.slice(0, 14)}…</p>
          <p className="mt-1 text-[13px] text-ink-soft">{l.schedule.length} échéances</p>
        </Card>
      </div>

      <Card>
        <CardHeader title="Échéancier d’amortissement" subtitle="Capital, intérêts et statut par échéance" />
        <div className="overflow-x-auto">
          <table className="w-full text-[13.5px]">
            <thead>
              <tr className="border-b border-line text-left text-[12px] uppercase tracking-[0.05em] text-ink-faint">
                <th className="px-5 py-2.5 font-semibold">#</th>
                <th className="px-5 py-2.5 font-semibold">Échéance</th>
                <th className="px-5 py-2.5 text-right font-semibold">Mensualité</th>
                <th className="px-5 py-2.5 text-right font-semibold">Capital</th>
                <th className="px-5 py-2.5 text-right font-semibold">Intérêts</th>
                <th className="px-5 py-2.5 text-right font-semibold">Statut</th>
              </tr>
            </thead>
            <tbody>
              {l.schedule.map((it) => (
                <tr key={it.index} className="border-b border-line last:border-0">
                  <td className="px-5 py-2.5 font-mono text-ink-soft tnum">{it.index}</td>
                  <td className="px-5 py-2.5 text-ink-soft">{formatDate(it.dueDate)}</td>
                  <td className="px-5 py-2.5 text-right font-mono tnum">
                    <Money amount={it.amount} className="text-[13.5px]" />
                  </td>
                  <td className="px-5 py-2.5 text-right font-mono tnum text-ink-soft">
                    <Money amount={it.principalPart} className="text-[13.5px]" />
                  </td>
                  <td className="px-5 py-2.5 text-right font-mono tnum text-ink-soft">
                    <Money amount={it.interestPart} className="text-[13.5px]" />
                  </td>
                  <td className="px-5 py-2.5 text-right">
                    <span
                      className={cn(
                        'text-[12px] font-semibold',
                        it.paid ? 'text-forest-deep' : 'text-ink-faint',
                      )}
                    >
                      {it.paid ? '✓ Payée' : 'À venir'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <AmountModal
        open={open}
        onClose={() => setOpen(false)}
        title="Rembourser le prêt"
        description="Réduit le capital restant dû et débite le compte lié."
        cta="Rembourser"
        onSubmit={async (amount) => {
          await repay.mutateAsync({ amount });
          toast.success('Remboursement enregistré.');
        }}
      />
    </div>
  );
}
