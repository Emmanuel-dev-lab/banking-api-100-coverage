import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { FieldWrap, Input } from '@/components/ui/Field';
import { ApiError } from '@/lib/errors';

interface FormValues {
  amount: number;
}

export function AmountModal({
  open,
  onClose,
  title,
  description,
  cta,
  onSubmit,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  cta: string;
  onSubmit: (amount: number) => Promise<unknown>;
}) {
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>();

  const submit = handleSubmit(async ({ amount }) => {
    setFormError(null);
    try {
      await onSubmit(Number(amount));
      reset();
      onClose();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Opération impossible.');
    }
  });

  return (
    <Modal open={open} onClose={onClose} title={title} description={description}>
      <form onSubmit={submit} className="space-y-4" noValidate>
        <FieldWrap label="Montant (FCFA)" htmlFor="amount" error={errors.amount?.message}>
          <Input
            id="amount"
            type="number"
            min={1}
            step={1000}
            autoFocus
            className="font-mono tnum text-[18px]"
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

        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="ghost" onClick={onClose}>
            Annuler
          </Button>
          <Button type="submit" loading={isSubmitting}>
            {cta}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
