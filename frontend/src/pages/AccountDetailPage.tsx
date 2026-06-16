import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAccount, useAccountStatusAction, useDeposit, useWithdraw } from '@/api/hooks';
import { useAuth } from '@/auth/AuthContext';
import { PageHeader } from '@/components/PageHeader';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Money } from '@/components/ui/Money';
import { AccountStatusBadge, AccountTypeTag } from '@/components/ui/Badge';
import { Spinner, ErrorState } from '@/components/ui/States';
import { TransactionList } from '@/components/TransactionList';
import { AmountModal } from '@/components/modals/AmountModal';
import { useToast } from '@/components/Toast';

type Dialog = 'deposit' | 'withdraw' | null;

export function AccountDetailPage() {
  const { id = '' } = useParams();
  const { isAdmin } = useAuth();
  const toast = useToast();
  const account = useAccount(id);
  const deposit = useDeposit(id);
  const withdraw = useWithdraw(id);
  const statusAction = useAccountStatusAction();
  const [dialog, setDialog] = useState<Dialog>(null);

  if (account.isLoading) return <Spinner label="Chargement du compte…" />;
  if (account.isError || !account.data)
    return <ErrorState error={account.error} onRetry={account.refetch} />;

  const a = account.data;
  const active = a.status === 'ACTIVE';

  const runStatus = async (action: 'freeze' | 'close' | 'reactivate', label: string) => {
    try {
      await statusAction.mutateAsync({ id, action });
      toast.success(label);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Action impossible.');
    }
  };

  return (
    <div className="animate-rise">
      <Link to={isAdmin ? '/admin/accounts' : '/'} className="mb-4 inline-block text-[13px] text-ink-soft hover:text-forest-deep">
        ← Retour
      </Link>

      <PageHeader
        eyebrow={a.type === 'CURRENT' ? 'Compte courant' : 'Compte épargne'}
        title="Détail du compte"
        action={
          active ? (
            <div className="flex gap-2">
              <Button variant="secondary" onClick={() => setDialog('deposit')}>
                Déposer
              </Button>
              <Button onClick={() => setDialog('withdraw')}>Retirer</Button>
            </div>
          ) : undefined
        }
      />

      <div className="mb-6 grid gap-4 lg:grid-cols-[1.4fr_1fr]">
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <AccountTypeTag type={a.type} />
            <AccountStatusBadge status={a.status} />
          </div>
          <p className="mt-5 text-[13px] text-ink-faint">Solde disponible</p>
          <Money amount={a.balance} colored className="mt-1 text-[40px]" />
          {!active && (
            <p className="mt-4 rounded-[10px] border border-warn/30 bg-warn-soft px-3 py-2 text-[13px] text-warn">
              Compte {a.status === 'FROZEN' ? 'gelé' : 'fermé'} : aucun mouvement possible.
            </p>
          )}
          <dl className="mt-5 grid grid-cols-2 gap-y-2 border-t border-line pt-4 text-[13px]">
            <dt className="text-ink-faint">Référence compte</dt>
            <dd className="text-right font-mono text-ink">{a.id}</dd>
            <dt className="text-ink-faint">Titulaire</dt>
            <dd className="text-right font-mono text-ink">{a.clientId.slice(0, 12)}…</dd>
          </dl>
        </Card>

        {isAdmin && (
          <Card className="p-6">
            <h3 className="text-[16px]">Administration</h3>
            <p className="mt-1 text-[13px] text-ink-soft">Gestion du statut du compte.</p>
            <div className="mt-4 flex flex-col gap-2">
              <Button
                variant="secondary"
                disabled={!active || statusAction.isPending}
                onClick={() => runStatus('freeze', 'Compte gelé.')}
              >
                Geler le compte
              </Button>
              <Button
                variant="secondary"
                disabled={a.status !== 'FROZEN' || statusAction.isPending}
                onClick={() => runStatus('reactivate', 'Compte réactivé.')}
              >
                Réactiver
              </Button>
              <Button
                variant="danger"
                disabled={a.status === 'CLOSED' || statusAction.isPending}
                onClick={() => runStatus('close', 'Compte fermé.')}
              >
                Fermer (solde nul requis)
              </Button>
            </div>
          </Card>
        )}
      </div>

      <TransactionList accountId={id} />

      <AmountModal
        open={dialog === 'deposit'}
        onClose={() => setDialog(null)}
        title="Effectuer un dépôt"
        description="Crédite le compte d’un montant en FCFA."
        cta="Déposer"
        onSubmit={async (amount) => {
          await deposit.mutateAsync({ amount });
          toast.success('Dépôt effectué.');
        }}
      />
      <AmountModal
        open={dialog === 'withdraw'}
        onClose={() => setDialog(null)}
        title="Effectuer un retrait"
        description="Débite le compte. Le découvert est limité par le plafond du compte."
        cta="Retirer"
        onSubmit={async (amount) => {
          await withdraw.mutateAsync({ amount });
          toast.success('Retrait effectué.');
        }}
      />
    </div>
  );
}
