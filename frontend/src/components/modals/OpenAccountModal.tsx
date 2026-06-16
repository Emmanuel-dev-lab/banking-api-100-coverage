import { useState } from 'react';
import { useForm } from 'react-hook-form';
import type { AccountType, OpenAccountRequest } from '@/api/types';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { FieldWrap, Input, Select } from '@/components/ui/Field';
import { ApiError } from '@/lib/errors';

interface FormValues {
  type: AccountType;
  overdraftLimit: number;
  annualRate: number;
}

export function OpenAccountModal({
  open,
  onClose,
  onSubmit,
}: {
  open: boolean;
  onClose: () => void;
  onSubmit: (body: OpenAccountRequest) => Promise<unknown>;
}) {
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ defaultValues: { type: 'CURRENT', overdraftLimit: 0, annualRate: 0 } });

  const type = watch('type');

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await onSubmit({
        type: values.type,
        overdraftLimit: values.type === 'CURRENT' ? Number(values.overdraftLimit) : 0,
        annualRate: values.type === 'SAVINGS' ? Number(values.annualRate) : 0,
      });
      reset();
      onClose();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Ouverture impossible.');
    }
  });

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Ouvrir un compte"
      description="Le solde initial est de 0 FCFA."
    >
      <form onSubmit={submit} className="space-y-4" noValidate>
        <FieldWrap label="Type de compte" htmlFor="type">
          <Select id="type" {...register('type')}>
            <option value="CURRENT">Compte courant (découvert autorisé)</option>
            <option value="SAVINGS">Compte épargne (taux d’intérêt)</option>
          </Select>
        </FieldWrap>

        {type === 'CURRENT' ? (
          <FieldWrap
            label="Plafond de découvert (FCFA)"
            htmlFor="overdraftLimit"
            hint="Montant maximal de solde négatif autorisé."
            error={errors.overdraftLimit?.message}
          >
            <Input
              id="overdraftLimit"
              type="number"
              min={0}
              step={1000}
              className="font-mono tnum"
              {...register('overdraftLimit', {
                valueAsNumber: true,
                min: { value: 0, message: 'Doit être ≥ 0' },
              })}
            />
          </FieldWrap>
        ) : (
          <FieldWrap
            label="Taux annuel"
            htmlFor="annualRate"
            hint="Ex. 0.03 pour 3 %."
            error={errors.annualRate?.message}
          >
            <Input
              id="annualRate"
              type="number"
              min={0}
              step={0.01}
              className="font-mono tnum"
              {...register('annualRate', {
                valueAsNumber: true,
                min: { value: 0, message: 'Doit être ≥ 0' },
              })}
            />
          </FieldWrap>
        )}

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
            Ouvrir le compte
          </Button>
        </div>
      </form>
    </Modal>
  );
}
