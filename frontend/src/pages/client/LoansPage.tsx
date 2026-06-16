import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMyAccounts, useMyLoans, useRequestMyLoan } from '@/api/hooks';
import { PageHeader } from '@/components/PageHeader';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Money } from '@/components/ui/Money';
import { LoanStatusBadge } from '@/components/ui/Badge';
import { Spinner, ErrorState, EmptyState } from '@/components/ui/States';
import { Pagination } from '@/components/ui/Pagination';
import { RequestLoanModal } from '@/components/modals/RequestLoanModal';
import { useToast } from '@/components/Toast';

const SIZE = 10;

export function LoansPage() {
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const toast = useToast();
  const loans = useMyLoans(page, SIZE);
  const accounts = useMyAccounts(0, 100);
  const requestLoan = useRequestMyLoan();

  const list = loans.data?.content ?? [];

  return (
    <div className="animate-rise">
      <PageHeader
        eyebrow="Financement"
        title="Mes prêts"
        description="Suivi du capital restant dû et des échéanciers."
        action={<Button onClick={() => setOpen(true)}>+ Demander un prêt</Button>}
      />

      {loans.isLoading ? (
        <Spinner />
      ) : loans.isError ? (
        <ErrorState error={loans.error} onRetry={loans.refetch} />
      ) : list.length === 0 ? (
        <EmptyState
          title="Aucun prêt"
          hint="Demandez un prêt à amortissement constant, décaissé sur votre compte."
          action={<Button onClick={() => setOpen(true)}>Demander un prêt</Button>}
        />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2">
            {list.map((loan) => (
              <Link key={loan.id} to={`/loans/${loan.id}`} className="block">
                <Card className="p-5 transition-all hover:-translate-y-0.5 hover:border-line-strong">
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-[12px] text-ink-faint">
                      {loan.id.slice(0, 8)}…
                    </span>
                    <LoanStatusBadge status={loan.status} />
                  </div>
                  <p className="mt-4 text-[13px] text-ink-faint">Capital restant dû</p>
                  <Money amount={loan.outstandingPrincipal} className="mt-0.5 text-[22px]" />
                  <p className="mt-2 text-[13px] text-ink-soft">
                    sur <Money amount={loan.principal} className="text-[13px]" /> ·{' '}
                    {loan.schedule.length} échéances
                  </p>
                </Card>
              </Link>
            ))}
          </div>
          {loans.data && (
            <div className="mt-4">
              <Pagination
                page={loans.data.page}
                totalPages={loans.data.totalPages}
                totalElements={loans.data.totalElements}
                onPage={setPage}
              />
            </div>
          )}
        </>
      )}

      <RequestLoanModal
        open={open}
        onClose={() => setOpen(false)}
        accounts={accounts.data?.content ?? []}
        onSubmit={async (values) => {
          await requestLoan.mutateAsync(values);
          toast.success('Prêt accordé et décaissé.');
        }}
      />
    </div>
  );
}
