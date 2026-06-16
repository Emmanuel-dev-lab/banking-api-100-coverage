import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useMe, useUpdateClient, useChangePassword } from '@/api/hooks';
import { PageHeader } from '@/components/PageHeader';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { FieldWrap, Input } from '@/components/ui/Field';
import { Spinner } from '@/components/ui/States';
import { useToast } from '@/components/Toast';
import { ApiError } from '@/lib/errors';
import { initials } from '@/lib/format';
import type { ChangePasswordRequest, UpdateClientRequest } from '@/api/types';

export function ProfilePage() {
  const me = useMe();
  if (me.isLoading || !me.data) return <Spinner label="Chargement du profil…" />;
  return <ProfileForms clientId={me.data.id} firstName={me.data.firstName} lastName={me.data.lastName} />;
}

function ProfileForms({
  clientId,
  firstName,
  lastName,
}: {
  clientId: string;
  firstName: string;
  lastName: string;
}) {
  const toast = useToast();
  const updateClient = useUpdateClient(clientId);
  const changePassword = useChangePassword();

  const nameForm = useForm<UpdateClientRequest>({ defaultValues: { firstName, lastName } });
  const pwForm = useForm<ChangePasswordRequest>();
  const [nameError, setNameError] = useState<string | null>(null);
  const [pwError, setPwError] = useState<string | null>(null);

  useEffect(() => {
    nameForm.reset({ firstName, lastName });
  }, [firstName, lastName, nameForm]);

  const saveName = nameForm.handleSubmit(async (data) => {
    setNameError(null);
    try {
      await updateClient.mutateAsync(data);
      toast.success('Profil mis à jour.');
    } catch (err) {
      setNameError(err instanceof ApiError ? err.message : 'Mise à jour impossible.');
    }
  });

  const savePassword = pwForm.handleSubmit(async (data) => {
    setPwError(null);
    try {
      await changePassword.mutateAsync(data);
      toast.success('Mot de passe changé.');
      pwForm.reset({ oldPassword: '', newPassword: '' });
    } catch (err) {
      setPwError(err instanceof ApiError ? err.message : 'Changement impossible.');
    }
  });

  return (
    <div className="animate-rise mx-auto max-w-2xl">
      <PageHeader eyebrow="Compte" title="Mon profil" />

      <div className="mb-6 flex items-center gap-4">
        <div className="flex size-16 items-center justify-center rounded-full bg-forest font-display text-[22px] font-semibold text-brass-soft">
          {initials(firstName, lastName)}
        </div>
        <div>
          <p className="font-display text-[22px] text-ink">
            {firstName} {lastName}
          </p>
          <p className="font-mono text-[12.5px] text-ink-faint">Réf. {clientId}</p>
        </div>
      </div>

      <div className="grid gap-5">
        <Card>
          <CardHeader title="Identité" subtitle="Modifier votre nom" />
          <form onSubmit={saveName} className="space-y-4 p-5" noValidate>
            <div className="grid grid-cols-2 gap-3">
              <FieldWrap label="Prénom" htmlFor="firstName" error={nameForm.formState.errors.firstName?.message}>
                <Input id="firstName" {...nameForm.register('firstName', { required: 'Prénom requis' })} />
              </FieldWrap>
              <FieldWrap label="Nom" htmlFor="lastName" error={nameForm.formState.errors.lastName?.message}>
                <Input id="lastName" {...nameForm.register('lastName', { required: 'Nom requis' })} />
              </FieldWrap>
            </div>
            {nameError && <p className="text-[13px] text-danger">{nameError}</p>}
            <div className="flex justify-end">
              <Button type="submit" loading={nameForm.formState.isSubmitting}>
                Enregistrer
              </Button>
            </div>
          </form>
        </Card>

        <Card>
          <CardHeader title="Sécurité" subtitle="Changer votre mot de passe" />
          <form onSubmit={savePassword} className="space-y-4 p-5" noValidate>
            <FieldWrap label="Mot de passe actuel" htmlFor="oldPassword" error={pwForm.formState.errors.oldPassword?.message}>
              <Input
                id="oldPassword"
                type="password"
                autoComplete="current-password"
                {...pwForm.register('oldPassword', { required: 'Mot de passe actuel requis' })}
              />
            </FieldWrap>
            <FieldWrap label="Nouveau mot de passe" htmlFor="newPassword" error={pwForm.formState.errors.newPassword?.message}>
              <Input
                id="newPassword"
                type="password"
                autoComplete="new-password"
                {...pwForm.register('newPassword', {
                  required: 'Nouveau mot de passe requis',
                  minLength: { value: 4, message: 'Au moins 4 caractères' },
                })}
              />
            </FieldWrap>
            {pwError && <p className="text-[13px] text-danger">{pwError}</p>}
            <div className="flex justify-end">
              <Button type="submit" loading={pwForm.formState.isSubmitting}>
                Changer le mot de passe
              </Button>
            </div>
          </form>
        </Card>
      </div>
    </div>
  );
}
