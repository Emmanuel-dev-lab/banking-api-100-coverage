import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useLoans } from '@/api/hooks';
import { PageHeader } from '@/components/PageHeader';
import { Card } from '@/components/ui/Card';
import { Money } from '@/components/ui/Money';
import { LoanStatusBadge } from '@/components/ui/Badge';
import { Spinner, ErrorState, EmptyState } from '@/components/ui/States';
import { Pagination } from '@/components/ui/Pagination';

const SIZE = 15;

export function AdminLoansPage() {
  const [page, setPage] = useState(0);
  const navigate = useNavigate();
  const loans = useLoans(page, SIZE);
  const list = loans.data?.content ?? [];

  return (
    <div className="animate-rise">
      <PageHeader eyebrow="Console" title="Prêts" description="Suivi des engagements de crédit." />

      <Card>
        {loans.isLoading ? (
          <Spinner />
        ) : loans.isError ? (
          <ErrorState error={loans.error} onRetry={loans.refetch} />
        ) : list.length === 0 ? (
          <EmptyState title="Aucun prêt" />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-[14px]">
                <thead>
                  <tr className="border-b border-line text-left text-[12px] uppercase tracking-[0.05em] text-ink-faint">
                    <th className="px-5 py-2.5 font-semibold">Prêt</th>
                    <th className="px-5 py-2.5 text-right font-semibold">Capital</th>
                    <th className="px-5 py-2.5 text-right font-semibold">Restant dû</th>
                    <th className="px-5 py-2.5 text-right font-semibold">Statut</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((l) => (
                    <tr
                      key={l.id}
                      onClick={() => navigate(`/loans/${l.id}`)}
                      className="cursor-pointer border-b border-line last:border-0 hover:bg-paper"
                    >
                      <td className="px-5 py-3 font-mono text-[12.5px] text-ink-soft">{l.id.slice(0, 14)}…</td>
                      <td className="px-5 py-3 text-right">
                        <Money amount={l.principal} className="text-[14px]" />
                      </td>
                      <td className="px-5 py-3 text-right">
                        <Money amount={l.outstandingPrincipal} className="text-[14px]" />
                      </td>
                      <td className="px-5 py-3 text-right">
                        <LoanStatusBadge status={l.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {loans.data && (
              <Pagination
                page={loans.data.page}
                totalPages={loans.data.totalPages}
                totalElements={loans.data.totalElements}
                onPage={setPage}
              />
            )}
          </>
        )}
      </Card>
    </div>
  );
}
