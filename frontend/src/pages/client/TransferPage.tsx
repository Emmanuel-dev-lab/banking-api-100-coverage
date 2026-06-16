import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useMyAccounts, useTransfer } from '@/api/hooks';
import { PageHeader } from '@/components/PageHeader';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { FieldWrap, Input, Select } from '@/components/ui/Field';
import { Spinner } from '@/components/ui/States';
import { useToast } from '@/components/Toast';
import { ApiError } from '@/lib/errors';
import { formatXAF } from '@/lib/format';
import type { TransferRequest } from '@/api/types';

export function TransferPage() {
  const toast = useToast();
  const accounts = useMyAccounts(0, 100);
  const transfer = useTransfer();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<TransferRequest>();

  const sourceId = watch('sourceAccountId');
  const eligible = (accounts.data?.content ?? []).filter((a) => a.status === 'ACTIVE');

  const onSubmit = handleSubmit(async (data) => {
    setFormError(null);
    if (data.sourceAccountId === data.destAccountId) {
      setFormError('Le compte source et le compte destination doivent différer.');
      return;
    }
    try {
      await transfer.mutateAsync({ ...data, amount: Number(data.amount) });
      toast.success('Virement effectué.');
      reset({ sourceAccountId: data.sourceAccountId, destAccountId: '', amount: undefined });
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Virement impossible.');
    }
  });

  return (
    <div className="animate-rise mx-auto max-w-xl">
      <PageHeader
        eyebrow="Mouvements"
        title="Effectuer un virement"
        description="Transfert atomique depuis l’un de vos comptes vers un autre compte."
      />

      {accounts.isLoading ? (
        <Spinner />
      ) : (
        <Card className="p-6">
          <form onSubmit={onSubmit} className="space-y-4" noValidate>
            <FieldWrap label="Compte source" htmlFor="source" error={errors.sourceAccountId?.message}>
              <Select
                id="source"
                {...register('sourceAccountId', { required: 'Compte source requis' })}
              >
                <option value="">Choisir un compte…</option>
                {eligible.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.type === 'CURRENT' ? 'Courant' : 'Épargne'} · {a.id.slice(0, 8)}… ·{' '}
                    {formatXAF(a.balance)}
                  </option>
                ))}
              </Select>
            </FieldWrap>

            <FieldWrap
              label="Compte destination"
              htmlFor="dest"
              hint="Identifiant du compte bénéficiaire."
              error={errors.destAccountId?.message}
            >
              <Input
                id="dest"
                placeholder="ex. 3f2a…"
                className="font-mono"
                {...register('destAccountId', { required: 'Compte destination requis' })}
              />
            </FieldWrap>

            <FieldWrap label="Montant (FCFA)" htmlFor="amount" error={errors.amount?.message}>
              <Input
                id="amount"
                type="number"
                min={1}
                step={1000}
                className="font-mono tnum text-[17px]"
                {...register('amount', {
                  valueAsNumber: true,
                  required: 'Montant requis',
                  min: { value: 1, message: 'Le montant doit être strictement positif' },
                })}
              />
            </FieldWrap>

            {formError && (
              <div className="rounded-[10px] border border-danger/30 bg-danger-soft px-3 py-2.5 text-[13px] text-danger">
                {formError}
              </div>
            )}

            <Button type="submit" loading={isSubmitting} disabled={!sourceId} className="w-full">
              Valider le virement
            </Button>
          </form>
        </Card>
      )}
    </div>
  );
}
