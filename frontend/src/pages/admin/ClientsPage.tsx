import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useClients, useCreateClient } from '@/api/hooks';
import { PageHeader } from '@/components/PageHeader';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Spinner, ErrorState, EmptyState } from '@/components/ui/States';
import { Pagination } from '@/components/ui/Pagination';
import { CreateClientModal } from '@/components/modals/CreateClientModal';
import { useToast } from '@/components/Toast';
import { initials } from '@/lib/format';

const SIZE = 15;

export function ClientsPage() {
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const toast = useToast();
  const navigate = useNavigate();
  const clients = useClients(page, SIZE);
  const createClient = useCreateClient();

  const list = clients.data?.content ?? [];

  return (
    <div className="animate-rise">
      <PageHeader
        eyebrow="Console"
        title="Clients"
        description="Annuaire des clients et création de comptes."
        action={<Button onClick={() => setOpen(true)}>+ Créer un client</Button>}
      />

      <Card>
        {clients.isLoading ? (
          <Spinner />
        ) : clients.isError ? (
          <ErrorState error={clients.error} onRetry={clients.refetch} />
        ) : list.length === 0 ? (
          <EmptyState title="Aucun client" />
        ) : (
          <>
            <ul>
              {list.map((c) => (
                <li key={c.id}>
                  <button
                    onClick={() => navigate(`/admin/clients/${c.id}`)}
                    className="flex w-full items-center gap-4 border-b border-line px-5 py-3.5 text-left last:border-0 hover:bg-paper"
                  >
                    <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-forest-soft text-[13px] font-semibold text-forest-deep">
                      {initials(c.firstName, c.lastName)}
                    </span>
                    <span className="flex-1">
                      <span className="block text-ink">
                        {c.firstName} {c.lastName}
                      </span>
                      <span className="font-mono text-[12px] text-ink-faint">{c.id}</span>
                    </span>
                    <span className="text-ink-faint">→</span>
                  </button>
                </li>
              ))}
            </ul>
            {clients.data && (
              <Pagination
                page={clients.data.page}
                totalPages={clients.data.totalPages}
                totalElements={clients.data.totalElements}
                onPage={setPage}
              />
            )}
          </>
        )}
      </Card>

      <CreateClientModal
        open={open}
        onClose={() => setOpen(false)}
        onSubmit={async (body) => {
          await createClient.mutateAsync(body);
          toast.success('Client créé.');
        }}
      />
    </div>
  );
}
