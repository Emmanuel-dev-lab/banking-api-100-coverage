import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import type { Role } from '@/api/types';
import { api } from '@/api/endpoints';
import { clearToken, getToken, setToken, setUnauthorizedHandler } from '@/api/client';
import { decodeJwt, isExpired } from './jwt';

interface AuthState {
  userId: string;
  clientId: string | null;
  role: Role;
}

interface AuthContextValue {
  auth: AuthState | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function stateFromToken(token: string | null): AuthState | null {
  if (!token) return null;
  const payload = decodeJwt(token);
  if (isExpired(payload) || !payload) return null;
  return { userId: payload.sub, clientId: payload.clientId, role: payload.role };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [auth, setAuth] = useState<AuthState | null>(() => stateFromToken(getToken()));

  const logout = useCallback(() => {
    clearToken();
    setAuth(null);
    queryClient.clear();
  }, [queryClient]);

  // Le client API declenche logout sur tout 401.
  useEffect(() => {
    setUnauthorizedHandler(logout);
  }, [logout]);

  // Purge automatique a l'expiration du jeton.
  useEffect(() => {
    if (!auth) return;
    const token = getToken();
    const payload = decodeJwt(token ?? '');
    if (!payload) return;
    const ms = payload.exp * 1000 - Date.now();
    const timer = setTimeout(logout, Math.max(0, ms));
    return () => clearTimeout(timer);
  }, [auth, logout]);

  const login = useCallback(async (username: string, password: string) => {
    const { token } = await api.login({ username, password });
    setToken(token);
    const next = stateFromToken(token);
    if (!next) throw new Error('Jeton invalide reçu du serveur.');
    setAuth(next);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      auth,
      isAuthenticated: auth !== null,
      isAdmin: auth?.role === 'ADMIN',
      login,
      logout,
    }),
    [auth, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth doit etre utilise dans AuthProvider.');
  return ctx;
}
