import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

/** Exige un jeton valide ; sinon redirige vers /login. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <>{children}</>;
}

/** Restreint a un role. Un CLIENT sur une route ADMIN repart vers son espace. */
export function RequireRole({ role, children }: { role: 'ADMIN' | 'CLIENT'; children: ReactNode }) {
  const { auth } = useAuth();
  if (!auth) return <Navigate to="/login" replace />;
  if (auth.role !== role) {
    return <Navigate to={auth.role === 'ADMIN' ? '/admin' : '/'} replace />;
  }
  return <>{children}</>;
}
