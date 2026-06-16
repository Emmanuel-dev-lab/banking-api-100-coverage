import { useState } from 'react';
import { useForm } from 'react-hook-form';
import type { AccountResponse } from '@/api/types';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { FieldWrap, Input, Select } from '@/components/ui/Field';
import { ApiError } from '@/lib/errors';
import { formatXAF } from '@/lib/format';

export interface LoanFormValues {
  accountId: string;
  principal: number;
  annualRate: number;
  termMonths: number;
}

export function RequestLoanModal({
  open,
  onClose,
  accounts,
  onSubmit,
}: {
  open: boolean;
  onClose: () => void;
  accounts: AccountResponse[];
  onSubmit: (values: LoanFormValues) => Promise<unknown>;
}) {
  const [formError, setFormError] = useState<string | null>(null);
  const eligible = accounts.filter((a) => a.status === 'ACTIVE');
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<LoanFormValues>({
    defaultValues: { principal: 100000, annualRate: 0.12, termMonths: 12 },
  });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await onSubmit({
        accountId: values.accountId,
        principal: Number(values.principal),
        annualRate: Number(values.annualRate),
        termMonths: Number(values.termMonths),
      });
      reset();
      onClose();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Demande impossible.');
    }
  });

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Demander un prêt"
      description="Le capital est décaissé sur le compte choisi. Amortissement constant."
    >
      {eligible.length === 0 ? (
        <p className="py-4 text-sm text-ink-soft">
          Aucun compte actif disponible pour recevoir le décaissement.
        </p>
      ) : (
        <form onSubmit={submit} className="space-y-4" noValidate>
          <FieldWrap label="Compte de décaissement" htmlFor="accountId" error={errors.accountId?.message}>
            <Select id="accountId" {...register('accountId', { required: 'Compte requis' })}>
              {eligible.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.type === 'CURRENT' ? 'Courant' : 'Épargne'} · {a.id.slice(0, 8)}… ·{' '}
                  {formatXAF(a.balance)}
                </option>
              ))}
            </Select>
          </FieldWrap>

          <FieldWrap label="Capital (FCFA)" htmlFor="principal" error={errors.principal?.message}>
            <Input
              id="principal"
              type="number"
              min={1}
              step={1000}
              className="font-mono tnum"
              {...register('principal', {
                valueAsNumber: true,
                min: { value: 1, message: 'Doit être > 0' },
              })}
            />
          </FieldWrap>

          <div className="grid grid-cols-2 gap-3">
            <FieldWrap label="Taux annuel" htmlFor="annualRate" error={errors.annualRate?.message}>
              <Input
                id="annualRate"
                type="number"
                min={0}
                step={0.01}
                className="font-mono tnum"
                {...register('annualRate', {
                  valueAsNumber: true,
                  min: { value: 0, message: '≥ 0' },
                })}
              />
            </FieldWrap>
            <FieldWrap label="Durée (mois)" htmlFor="termMonths" error={errors.termMonths?.message}>
              <Input
                id="termMonths"
                type="number"
                min={1}
                step={1}
                className="font-mono tnum"
                {...register('termMonths', {
                  valueAsNumber: true,
                  min: { value: 1, message: '> 0' },
                })}
              />
            </FieldWrap>
          </div>

          {formError && (
            <div className="rounded-[10px] border border-danger/30 bg-danger-soft px-3 py-2.5 text-[13px] text-danger">
              {formError}
            </div>
          )}

          <div className="flex justify-end gap-2 pt-1">
            <Button type="button" variant="ghost" onClick={onClose}>
              Annuler
            </Button>
            <Button type="submit" loading={isSubmitting}>
              Demander
            </Button>
          </div>
        </form>
      )}
    </Modal>
  );
}
