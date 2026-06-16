import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  clearToken,
  getToken,
  request,
  setToken,
  setUnauthorizedHandler,
} from './client';
import { ApiError } from '@/lib/errors';

function mockFetch(status: number, body: unknown) {
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
    text: async () => (body === undefined ? '' : JSON.stringify(body)),
  } as Response);
}

beforeEach(() => {
  localStorage.clear();
  clearToken();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('client API', () => {
  it('attache le Bearer quand un jeton est present', async () => {
    setToken('tok-123');
    const fetchMock = mockFetch(200, { ok: true });
    vi.stubGlobal('fetch', fetchMock);

    await request('/api/me');

    const headers = fetchMock.mock.calls[0][1].headers as Record<string, string>;
    expect(headers.Authorization).toBe('Bearer tok-123');
  });

  it('n’attache pas de jeton en mode anonyme', async () => {
    setToken('tok-123');
    const fetchMock = mockFetch(200, { token: 'x' });
    vi.stubGlobal('fetch', fetchMock);

    await request('/api/auth/login', { method: 'POST', body: {}, anonymous: true });

    const headers = fetchMock.mock.calls[0][1].headers as Record<string, string>;
    expect(headers.Authorization).toBeUndefined();
  });

  it('mappe une reponse {code,message} en ApiError', async () => {
    vi.stubGlobal('fetch', mockFetch(422, { code: 'INSUFFICIENT_FUNDS', message: 'fonds insuffisants' }));

    await expect(request('/api/accounts/a/withdraw', { method: 'POST', body: { amount: 1 } }))
      .rejects.toMatchObject({ status: 422, code: 'INSUFFICIENT_FUNDS', message: 'fonds insuffisants' });
  });

  it('purge le jeton et notifie sur 401', async () => {
    setToken('tok-123');
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    vi.stubGlobal('fetch', mockFetch(401, { code: 'UNAUTHORIZED', message: 'invalid token' }));

    await expect(request('/api/me')).rejects.toBeInstanceOf(ApiError);
    expect(getToken()).toBeNull();
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });

  it('retourne undefined sur 204', async () => {
    setToken('tok-123');
    vi.stubGlobal('fetch', mockFetch(204, undefined));
    await expect(request('/api/me/password', { method: 'POST', body: {} })).resolves.toBeUndefined();
  });
});
