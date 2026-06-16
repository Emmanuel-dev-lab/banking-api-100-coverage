import { Link } from 'react-router-dom';
import {
  useAccounts,
  useCapitalizeInterest,
  useClients,
  useFlagOverdue,
  useLoans,
} from '@/api/hooks';
import { PageHeader } from '@/components/PageHeader';
import { StatCard } from '@/components/StatCard';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Money } from '@/components/ui/Money';
import { AccountStatusBadge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/States';
import { useToast } from '@/components/Toast';

export function AdminDashboard() {
  // On lit la 1re page de chaque ressource : totalElements donne les compteurs,
  // content donne un apercu.
  const clients = useClients(0, 5);
  const accounts = useAccounts(0, 6);
  const loans = useLoans(0, 1);
  const toast = useToast();
  const capitalize = useCapitalizeInterest();
  const flagOverdue = useFlagOverdue();

  const activeLoans = loans.data?.totalElements ?? 0;

  const runInterest = async () => {
    try {
      const r = await capitalize.mutateAsync();
      toast.success(`Intérêts capitalisés sur ${r.processed} compte(s).`);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Échec du job.');
    }
  };

  const runOverdue = async () => {
    try {
      const r = await flagOverdue.mutateAsync();
      toast.success(`${r.processed} prêt(s) marqué(s) en retard.`);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Échec du job.');
    }
  };

  return (
    <div className="animate-rise">
      <PageHeader
        eyebrow="Console d’administration"
        title="Vue générale"
        description="État du portefeuille : clients, comptes et engagements."
      />

      <div className="mb-8 grid gap-4 sm:grid-cols-3">
        <StatCard label="Clients" value={clients.data?.totalElements ?? '—'} foot="Comptes clients ouverts" />
        <StatCard label="Comptes" value={accounts.data?.totalElements ?? '—'} foot="Tous types confondus" />
        <StatCard label="Prêts" value={activeLoans} foot="Dossiers enregistrés" />
      </div>

      <Card className="mb-5 flex flex-wrap items-center justify-between gap-4 p-5">
        <div>
          <h3 className="text-[16px]">Traitements périodiques</h3>
          <p className="mt-0.5 text-[13px] text-ink-soft">
            Déclenchés automatiquement (cron) — exécution manuelle possible ici.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" loading={capitalize.isPending} onClick={runInterest}>
            Capitaliser les intérêts
          </Button>
          <Button variant="secondary" loading={flagOverdue.isPending} onClick={runOverdue}>
            Marquer les retards
          </Button>
        </div>
      </Card>

      <div className="grid gap-5 lg:grid-cols-2">
        <Card>
          <CardHeader
            title="Derniers comptes"
            action={
              <Link to="/admin/accounts" className="text-[13px] text-forest-deep hover:underline">
                Tout voir →
              </Link>
            }
          />
          {accounts.isLoading ? (
            <Spinner />
          ) : (
            <ul>
              {(accounts.data?.content ?? []).map((a) => (
                <li key={a.id}>
                  <Link
                    to={`/accounts/${a.id}`}
                    className="flex items-center justify-between gap-3 border-b border-line px-5 py-3 last:border-0 hover:bg-paper"
                  >
                    <span className="font-mono text-[12.5px] text-ink-soft">{a.id.slice(0, 10)}…</span>
                    <span className="flex items-center gap-3">
                      <Money amount={a.balance} colored className="text-[13.5px]" />
                      <AccountStatusBadge status={a.status} />
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card>
          <CardHeader
            title="Derniers clients"
            action={
              <Link to="/admin/clients" className="text-[13px] text-forest-deep hover:underline">
                Tout voir →
              </Link>
            }
          />
          {clients.isLoading ? (
            <Spinner />
          ) : (
            <ul>
              {(clients.data?.content ?? []).map((c) => (
                <li key={c.id}>
                  <Link
                    to={`/admin/clients/${c.id}`}
                    className="flex items-center justify-between gap-3 border-b border-line px-5 py-3 last:border-0 hover:bg-paper"
                  >
                    <span className="text-ink">
                      {c.firstName} {c.lastName}
                    </span>
                    <span className="font-mono text-[12px] text-ink-faint">{c.id.slice(0, 8)}…</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  );
}
