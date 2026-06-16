import type { ErrorResponse } from '@/api/types';

/** Erreur applicative typee, portant le statut HTTP et le code metier. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

/** Message lisible par defaut selon le statut, si le serveur n'en fournit pas. */
export function defaultMessageFor(status: number): string {
  switch (status) {
    case 401:
      return 'Session expiree ou identifiants invalides.';
    case 403:
      return "Vous n'avez pas acces a cette ressource.";
    case 404:
      return 'Ressource introuvable.';
    case 409:
      return 'Operation impossible dans l’etat actuel.';
    case 422:
      return 'Operation refusee (fonds ou contrainte metier).';
    default:
      return 'Une erreur inattendue est survenue.';
  }
}

export function isErrorResponse(value: unknown): value is ErrorResponse {
  return (
    typeof value === 'object' &&
    value !== null &&
    'code' in value &&
    'message' in value
  );
}
