import { useState } from 'react';
import { useTransactions } from '@/api/hooks';
import { Card, CardHeader } from './ui/Card';
import { TxnTypeBadge } from './ui/Badge';
import { Pagination } from './ui/Pagination';
import { Spinner, EmptyState, ErrorState } from './ui/States';
import { formatDateTime, formatSignedXAF, txnIsCredit } from '@/lib/format';
import { cn } from '@/lib/cn';

const SIZE = 10;

/** Historique pagine d'un compte : credits en vert, debits en encre. */
export function TransactionList({ accountId }: { accountId: string }) {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError, error, refetch } = useTransactions(accountId, page, SIZE);

  return (
    <Card>
      <CardHeader title="Historique" subtitle="Du plus récent au plus ancien" />
      {isLoading ? (
        <Spinner />
      ) : isError ? (
        <ErrorState error={error} onRetry={refetch} />
      ) : !data || data.content.length === 0 ? (
        <EmptyState title="Aucune écriture" hint="Les mouvements de ce compte apparaîtront ici." />
      ) : (
        <>
          <ul>
            {data.content.map((t) => {
              const credit = txnIsCredit(t.type);
              return (
                <li
                  key={t.id}
                  className="flex items-center justify-between gap-4 border-b border-line px-5 py-3.5 last:border-0"
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2.5">
                      <TxnTypeBadge type={t.type} />
                      {t.relatedAccountId && (
                        <span className="truncate font-mono text-[11.5px] text-ink-faint">
                          ↔ {t.relatedAccountId.slice(0, 8)}…
                        </span>
                      )}
                    </div>
                    <p className="mt-1 text-[12.5px] text-ink-faint">{formatDateTime(t.date)}</p>
                  </div>
                  <span
                    className={cn(
                      'tnum shrink-0 font-mono text-[15px]',
                      credit ? 'text-forest-deep' : 'text-ink',
                    )}
                  >
                    {formatSignedXAF(credit ? t.amount : -t.amount)}
                  </span>
                </li>
              );
            })}
          </ul>
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onPage={setPage}
          />
        </>
      )}
    </Card>
  );
}
