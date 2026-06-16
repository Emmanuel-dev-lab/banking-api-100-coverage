import type { Role } from '@/api/types';

export interface JwtPayload {
  sub: string; // userId
  clientId: string | null;
  role: Role;
  exp: number; // secondes epoch
  iss?: string;
}

function decodeBase64Url(segment: string): string {
  const padded = segment.replace(/-/g, '+').replace(/_/g, '/');
  const json = atob(padded);
  // Gere les caracteres non-ASCII eventuels.
  return decodeURIComponent(
    json
      .split('')
      .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
      .join(''),
  );
}

/** Decode le payload d'un JWT sans verifier la signature (verification serveur). */
export function decodeJwt(token: string): JwtPayload | null {
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  try {
    const payload = JSON.parse(decodeBase64Url(parts[1])) as Record<string, unknown>;
    if (typeof payload.sub !== 'string' || typeof payload.role !== 'string') {
      return null;
    }
    return {
      sub: payload.sub,
      clientId: (payload.clientId as string | null) ?? null,
      role: payload.role as Role,
      exp: typeof payload.exp === 'number' ? payload.exp : 0,
      iss: typeof payload.iss === 'string' ? payload.iss : undefined,
    };
  } catch {
    return null;
  }
}

/** true si le jeton est absent, illisible ou expire (marge de 5 s). */
export function isExpired(payload: JwtPayload | null): boolean {
  if (!payload) return true;
  return payload.exp * 1000 <= Date.now() + 5000;
}
