import { describe, expect, it } from 'vitest';
import { decodeJwt, isExpired } from './jwt';

// Construit un JWT factice (header.payload.signature) non signe.
function fakeJwt(payload: Record<string, unknown>): string {
  const b64 = (o: unknown) =>
    btoa(JSON.stringify(o)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${b64({ alg: 'HS256' })}.${b64(payload)}.sig`;
}

describe('decodeJwt', () => {
  it('decode un payload client', () => {
    const token = fakeJwt({ sub: 'u1', clientId: 'c1', role: 'CLIENT', exp: 9999999999 });
    const p = decodeJwt(token);
    expect(p).not.toBeNull();
    expect(p!.sub).toBe('u1');
    expect(p!.clientId).toBe('c1');
    expect(p!.role).toBe('CLIENT');
  });

  it('admet un clientId nul (ADMIN)', () => {
    const token = fakeJwt({ sub: 'a1', clientId: null, role: 'ADMIN', exp: 9999999999 });
    expect(decodeJwt(token)!.clientId).toBeNull();
  });

  it('rejette un jeton malforme', () => {
    expect(decodeJwt('pas-un-jwt')).toBeNull();
    expect(decodeJwt('a.b')).toBeNull();
  });

  it('rejette un payload sans sub/role', () => {
    expect(decodeJwt(fakeJwt({ foo: 'bar' }))).toBeNull();
  });
});

describe('isExpired', () => {
  it('considere un jeton sans payload comme expire', () => {
    expect(isExpired(null)).toBe(true);
  });

  it('detecte un exp passe', () => {
    const past = Math.floor(Date.now() / 1000) - 100;
    expect(isExpired({ sub: 'u', clientId: null, role: 'CLIENT', exp: past })).toBe(true);
  });

  it('accepte un exp futur', () => {
    const future = Math.floor(Date.now() / 1000) + 3600;
    expect(isExpired({ sub: 'u', clientId: null, role: 'CLIENT', exp: future })).toBe(false);
  });
});
