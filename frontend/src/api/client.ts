import { ApiError, defaultMessageFor, isErrorResponse } from '@/lib/errors';

const TOKEN_KEY = 'comptoir.token';

let inMemoryToken: string | null = null;
let onUnauthorized: (() => void) | null = null;

/** Branche le handler de deconnexion (appele sur 401). */
export function setUnauthorizedHandler(handler: () => void): void {
  onUnauthorized = handler;
}

export function getToken(): string | null {
  if (inMemoryToken) return inMemoryToken;
  inMemoryToken = localStorage.getItem(TOKEN_KEY);
  return inMemoryToken;
}

export function setToken(token: string): void {
  inMemoryToken = token;
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  inMemoryToken = null;
  localStorage.removeItem(TOKEN_KEY);
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE';
  body?: unknown;
  /** N'attache pas le jeton (ex. login). */
  anonymous?: boolean;
}

/**
 * Point d'entree unique des appels API. Injecte le Bearer, parse les
 * reponses {code, message} en ApiError typee, et declenche la deconnexion
 * sur 401.
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, anonymous = false } = options;

  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (!anonymous) {
    const token = getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401 && !anonymous) {
    clearToken();
    onUnauthorized?.();
  }

  if (!response.ok) {
    throw await toApiError(response);
  }

  if (response.status === 204) return undefined as T;
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

async function toApiError(response: Response): Promise<ApiError> {
  let code = 'ERROR';
  let message = defaultMessageFor(response.status);
  try {
    const payload = await response.json();
    if (isErrorResponse(payload)) {
      code = payload.code;
      message = payload.message || message;
    }
  } catch {
    // Corps non-JSON : on garde le message par defaut.
  }
  return new ApiError(response.status, code, message);
}
