import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAccounts } from '@/api/hooks';
import { PageHeader } from '@/components/PageHeader';
import { Card } from '@/components/ui/Card';
import { Money } from '@/components/ui/Money';
import { AccountStatusBadge, AccountTypeTag } from '@/components/ui/Badge';
import { Spinner, ErrorState, EmptyState } from '@/components/ui/States';
import { Pagination } from '@/components/ui/Pagination';

const SIZE = 15;

export function AccountsPage() {
  const [page, setPage] = useState(0);
  const navigate = useNavigate();
  const accounts = useAccounts(page, SIZE);
  const list = accounts.data?.content ?? [];

  return (
    <div className="animate-rise">
      <PageHeader
        eyebrow="Console"
        title="Comptes"
        description="Tous les comptes. Cliquez pour gérer le statut et l’historique."
      />

      <Card>
        {accounts.isLoading ? (
          <Spinner />
        ) : accounts.isError ? (
          <ErrorState error={accounts.error} onRetry={accounts.refetch} />
        ) : list.length === 0 ? (
          <EmptyState title="Aucun compte" />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-[14px]">
                <thead>
                  <tr className="border-b border-line text-left text-[12px] uppercase tracking-[0.05em] text-ink-faint">
                    <th className="px-5 py-2.5 font-semibold">Compte</th>
                    <th className="px-5 py-2.5 font-semibold">Type</th>
                    <th className="px-5 py-2.5 text-right font-semibold">Solde</th>
                    <th className="px-5 py-2.5 text-right font-semibold">Statut</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((a) => (
                    <tr
                      key={a.id}
                      onClick={() => navigate(`/accounts/${a.id}`)}
                      className="cursor-pointer border-b border-line last:border-0 hover:bg-paper"
                    >
                      <td className="px-5 py-3 font-mono text-[12.5px] text-ink-soft">{a.id.slice(0, 14)}…</td>
                      <td className="px-5 py-3">
                        <AccountTypeTag type={a.type} />
                      </td>
                      <td className="px-5 py-3 text-right">
                        <Money amount={a.balance} colored className="text-[14px]" />
                      </td>
                      <td className="px-5 py-3 text-right">
                        <AccountStatusBadge status={a.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {accounts.data && (
              <Pagination
                page={accounts.data.page}
                totalPages={accounts.data.totalPages}
                totalElements={accounts.data.totalElements}
                onPage={setPage}
              />
            )}
          </>
        )}
      </Card>
    </div>
  );
}
