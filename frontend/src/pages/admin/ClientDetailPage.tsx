import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
  useClient,
  useClientAccounts,
  useClientLoans,
  useOpenClientAccount,
  useUpdateClient,
} from '@/api/hooks';
import { PageHeader } from '@/components/PageHeader';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { FieldWrap, Input } from '@/components/ui/Field';
import { Money } from '@/components/ui/Money';
import { AccountStatusBadge, AccountTypeTag, LoanStatusBadge } from '@/components/ui/Badge';
import { Spinner, ErrorState, EmptyState } from '@/components/ui/States';
import { OpenAccountModal } from '@/components/modals/OpenAccountModal';
import { useToast } from '@/components/Toast';
import { ApiError } from '@/lib/errors';
import type { UpdateClientRequest } from '@/api/types';

export function ClientDetailPage() {
  const { id = '' } = useParams();
  const client = useClient(id);

  if (client.isLoading) return <Spinner label="Chargement du client…" />;
  if (client.isError || !client.data)
    return <ErrorState error={client.error} onRetry={client.refetch} />;

  return <ClientDetail id={id} firstName={client.data.firstName} lastName={client.data.lastName} />;
}

function ClientDetail({ id, firstName, lastName }: { id: string; firstName: string; lastName: string }) {
  const toast = useToast();
  const accounts = useClientAccounts(id, 0, 50);
  const loans = useClientLoans(id, 0, 50);
  const updateClient = useUpdateClient(id);
  const openAccount = useOpenClientAccount(id);
  const [openModal, setOpenModal] = useState(false);
  const [nameError, setNameError] = useState<string | null>(null);

  const form = useForm<UpdateClientRequest>({ defaultValues: { firstName, lastName } });
  useEffect(() => form.reset({ firstName, lastName }), [firstName, lastName, form]);

  const saveName = form.handleSubmit(async (data) => {
    setNameError(null);
    try {
      await updateClient.mutateAsync(data);
      toast.success('Client mis à jour.');
    } catch (err) {
      setNameError(err instanceof ApiError ? err.message : 'Mise à jour impossible.');
    }
  });

  return (
    <div className="animate-rise">
      <Link to="/admin/clients" className="mb-4 inline-block text-[13px] text-ink-soft hover:text-forest-deep">
        ← Clients
      </Link>
      <PageHeader eyebrow="Client" title={`${firstName} ${lastName}`} description={`Réf. ${id}`} />

      <div className="grid gap-5 lg:grid-cols-[1fr_1.5fr]">
        <Card>
          <CardHeader title="Identité" subtitle="Modifier le nom" />
          <form onSubmit={saveName} className="space-y-4 p-5" noValidate>
            <FieldWrap label="Prénom" htmlFor="firstName" error={form.formState.errors.firstName?.message}>
              <Input id="firstName" {...form.register('firstName', { required: 'Prénom requis' })} />
            </FieldWrap>
            <FieldWrap label="Nom" htmlFor="lastName" error={form.formState.errors.lastName?.message}>
              <Input id="lastName" {...form.register('lastName', { required: 'Nom requis' })} />
            </FieldWrap>
            {nameError && <p className="text-[13px] text-danger">{nameError}</p>}
            <div className="flex justify-end">
              <Button type="submit" loading={form.formState.isSubmitting}>
                Enregistrer
              </Button>
            </div>
          </form>
        </Card>

        <div className="space-y-5">
          <Card>
            <CardHeader
              title="Comptes"
              action={
                <Button size="sm" variant="secondary" onClick={() => setOpenModal(true)}>
                  + Ouvrir
                </Button>
              }
            />
            {accounts.isLoading ? (
              <Spinner />
            ) : (accounts.data?.content.length ?? 0) === 0 ? (
              <EmptyState title="Aucun compte" />
            ) : (
              <ul>
                {accounts.data!.content.map((a) => (
                  <li key={a.id}>
                    <Link
                      to={`/accounts/${a.id}`}
                      className="flex items-center justify-between gap-3 border-b border-line px-5 py-3 last:border-0 hover:bg-paper"
                    >
                      <span className="flex items-center gap-3">
                        <AccountTypeTag type={a.type} />
                        <AccountStatusBadge status={a.status} />
                      </span>
                      <Money amount={a.balance} colored className="text-[14px]" />
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card>
            <CardHeader title="Prêts" />
            {loans.isLoading ? (
              <Spinner />
            ) : (loans.data?.content.length ?? 0) === 0 ? (
              <EmptyState title="Aucun prêt" />
            ) : (
              <ul>
                {loans.data!.content.map((l) => (
                  <li key={l.id}>
                    <Link
                      to={`/loans/${l.id}`}
                      className="flex items-center justify-between gap-3 border-b border-line px-5 py-3 last:border-0 hover:bg-paper"
                    >
                      <span className="font-mono text-[12.5px] text-ink-soft">{l.id.slice(0, 10)}…</span>
                      <span className="flex items-center gap-3">
                        <Money amount={l.outstandingPrincipal} className="text-[14px]" />
                        <LoanStatusBadge status={l.status} />
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>
      </div>

      <OpenAccountModal
        open={openModal}
        onClose={() => setOpenModal(false)}
        onSubmit={async (body) => {
          await openAccount.mutateAsync(body);
          toast.success('Compte ouvert pour le client.');
        }}
      />
    </div>
  );
}
