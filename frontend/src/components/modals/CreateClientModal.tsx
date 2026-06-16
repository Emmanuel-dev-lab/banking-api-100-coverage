import { useState } from 'react';
import { useForm } from 'react-hook-form';
import type { CreateClientRequest } from '@/api/types';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { FieldWrap, Input } from '@/components/ui/Field';
import { ApiError } from '@/lib/errors';

export function CreateClientModal({
  open,
  onClose,
  onSubmit,
}: {
  open: boolean;
  onClose: () => void;
  onSubmit: (body: CreateClientRequest) => Promise<unknown>;
}) {
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateClientRequest>();

  const submit = handleSubmit(async (data) => {
    setFormError(null);
    try {
      await onSubmit(data);
      reset();
      onClose();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Création impossible.');
    }
  });

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Créer un client"
      description="Crée le client et son compte utilisateur associé."
    >
      <form onSubmit={submit} className="space-y-4" noValidate>
        <div className="grid grid-cols-2 gap-3">
          <FieldWrap label="Prénom" htmlFor="firstName" error={errors.firstName?.message}>
            <Input id="firstName" {...register('firstName', { required: 'Prénom requis' })} />
          </FieldWrap>
          <FieldWrap label="Nom" htmlFor="lastName" error={errors.lastName?.message}>
            <Input id="lastName" {...register('lastName', { required: 'Nom requis' })} />
          </FieldWrap>
        </div>
        <FieldWrap label="Identifiant de connexion" htmlFor="username" error={errors.username?.message}>
          <Input id="username" className="font-mono" {...register('username', { required: 'Identifiant requis' })} />
        </FieldWrap>
        <FieldWrap label="Mot de passe initial" htmlFor="password" error={errors.password?.message}>
          <Input
            id="password"
            type="password"
            {...register('password', { required: 'Mot de passe requis' })}
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
            Créer le client
          </Button>
        </div>
      </form>
    </Modal>
  );
}
