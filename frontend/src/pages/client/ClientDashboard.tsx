import { useState } from 'react';
import { useMe, useMyAccounts, useOpenMyAccount } from '@/api/hooks';
import { PageHeader } from '@/components/PageHeader';
import { StatCard } from '@/components/StatCard';
import { AccountCard } from '@/components/AccountCard';
import { Button } from '@/components/ui/Button';
import { Spinner, ErrorState, EmptyState } from '@/components/ui/States';
import { Pagination } from '@/components/ui/Pagination';
import { OpenAccountModal } from '@/components/modals/OpenAccountModal';
import { useToast } from '@/components/Toast';
import { formatXAF } from '@/lib/format';

const SIZE = 12;

export function ClientDashboard() {
  const [page, setPage] = useState(0);
  const [openModal, setOpenModal] = useState(false);
  const toast = useToast();
  const me = useMe();
  const accounts = useMyAccounts(page, SIZE);
  const openAccount = useOpenMyAccount();

  const list = accounts.data?.content ?? [];
  const totalBalance = list.reduce((sum, a) => sum + a.balance, 0);

  return (
    <div className="animate-rise">
      <PageHeader
        eyebrow={me.data ? `Bonjour, ${me.data.firstName}` : 'Espace client'}
        title="Tableau de bord"
        description="Vos comptes en francs CFA, d’un coup d’œil."
        action={<Button onClick={() => setOpenModal(true)}>+ Ouvrir un compte</Button>}
      />

      <div className="mb-8 grid gap-4 sm:grid-cols-3">
        <StatCard
          label="Solde cumulé (page)"
          value={formatXAF(totalBalance)}
          foot="Somme des comptes affichés"
        />
        <StatCard label="Comptes" value={accounts.data?.totalElements ?? '—'} />
        <StatCard
          label="Identité"
          value={me.data ? `${me.data.firstName} ${me.data.lastName}` : '—'}
          foot={me.data ? `Réf. ${me.data.id.slice(0, 8)}…` : undefined}
        />
      </div>

      <h2 className="mb-4 text-[20px]">Mes comptes</h2>
      {accounts.isLoading ? (
        <Spinner />
      ) : accounts.isError ? (
        <ErrorState error={accounts.error} onRetry={accounts.refetch} />
      ) : list.length === 0 ? (
        <EmptyState
          title="Aucun compte"
          hint="Ouvrez votre premier compte pour commencer."
          action={<Button onClick={() => setOpenModal(true)}>Ouvrir un compte</Button>}
        />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {list.map((a) => (
              <AccountCard key={a.id} account={a} />
            ))}
          </div>
          {accounts.data && (
            <div className="mt-4">
              <Pagination
                page={accounts.data.page}
                totalPages={accounts.data.totalPages}
                totalElements={accounts.data.totalElements}
                onPage={setPage}
              />
            </div>
          )}
        </>
      )}

      <OpenAccountModal
        open={openModal}
        onClose={() => setOpenModal(false)}
        onSubmit={async (body) => {
          await openAccount.mutateAsync(body);
          toast.success('Compte ouvert.');
        }}
      />
    </div>
  );
}
