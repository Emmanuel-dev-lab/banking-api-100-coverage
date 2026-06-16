import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { useAuth } from '@/auth/AuthContext';
import { ApiError } from '@/lib/errors';
import { Button } from '@/components/ui/Button';
import { FieldWrap, Input } from '@/components/ui/Field';
import type { LoginRequest } from '@/api/types';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginRequest>();

  const onSubmit = handleSubmit(async (data) => {
    setFormError(null);
    try {
      await login(data.username.trim(), data.password);
      navigate('/', { replace: true });
    } catch (err) {
      setFormError(
        err instanceof ApiError ? err.message : 'Connexion impossible. Réessayez.',
      );
    }
  });

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.1fr_1fr]">
      {/* Volet editorial */}
      <div className="relative hidden flex-col justify-between overflow-hidden bg-forest-deep p-12 text-paper lg:flex">
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.18]"
          style={{
            backgroundImage:
              'radial-gradient(60% 50% at 80% 10%, oklch(0.66 0.1 75), transparent 60%), radial-gradient(50% 40% at 10% 90%, oklch(0.5 0.11 158), transparent 60%)',
          }}
        />
        <div className="relative flex items-center gap-2.5">
          <div className="flex size-9 items-center justify-center rounded-[9px] bg-brass text-forest-deep">
            <span className="font-display text-lg font-bold">C</span>
          </div>
          <span className="font-display text-[20px] font-semibold">Comptoir</span>
        </div>
        <div className="relative">
          <h1 className="max-w-md font-display text-[44px] font-semibold leading-[1.05] text-paper">
            La banque, tenue comme un registre.
          </h1>
          <p className="mt-4 max-w-sm text-[15px] text-paper/70">
            Comptes, virements et prêts en francs CFA. Chaque écriture, à sa place,
            dans l’ordre.
          </p>
        </div>
        <p className="relative text-[12px] uppercase tracking-[0.16em] text-paper/40">
          XAF · Franc CFA · Afrique centrale
        </p>
      </div>

      {/* Formulaire */}
      <div className="flex items-center justify-center px-6 py-12">
        <div className="animate-rise w-full max-w-sm">
          <div className="mb-8">
            <p className="mb-1.5 text-[12px] font-semibold uppercase tracking-[0.14em] text-brass">
              Connexion
            </p>
            <h2 className="text-[28px] text-ink">Accéder à votre espace</h2>
          </div>

          <form onSubmit={onSubmit} className="space-y-4" noValidate>
            <FieldWrap label="Identifiant" htmlFor="username" error={errors.username?.message}>
              <Input
                id="username"
                autoComplete="username"
                autoFocus
                placeholder="ex. client1"
                {...register('username', { required: 'Identifiant requis' })}
              />
            </FieldWrap>

            <FieldWrap label="Mot de passe" htmlFor="password" error={errors.password?.message}>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                placeholder="••••••••"
                {...register('password', { required: 'Mot de passe requis' })}
              />
            </FieldWrap>

            {formError && (
              <div className="rounded-[10px] border border-danger/30 bg-danger-soft px-3 py-2.5 text-[13px] text-danger">
                {formError}
              </div>
            )}

            <Button type="submit" loading={isSubmitting} className="w-full">
              Se connecter
            </Button>
          </form>

          <p className="mt-6 text-center text-[12.5px] text-ink-faint">
            Démo · admin / admin123 · client1 / client123
          </p>
        </div>
      </div>
    </div>
  );
}
