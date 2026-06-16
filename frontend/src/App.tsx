import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from '@/auth/AuthContext';
import { RequireAuth, RequireRole } from '@/auth/guards';
import { AppShell } from '@/layout/AppShell';
import { LoginPage } from '@/pages/LoginPage';
import { ClientDashboard } from '@/pages/client/ClientDashboard';
import { TransferPage } from '@/pages/client/TransferPage';
import { LoansPage } from '@/pages/client/LoansPage';
import { ProfilePage } from '@/pages/client/ProfilePage';
import { AccountDetailPage } from '@/pages/AccountDetailPage';
import { LoanDetailPage } from '@/pages/LoanDetailPage';
import { AdminDashboard } from '@/pages/admin/AdminDashboard';
import { ClientsPage } from '@/pages/admin/ClientsPage';
import { ClientDetailPage } from '@/pages/admin/ClientDetailPage';
import { AccountsPage } from '@/pages/admin/AccountsPage';
import { AdminLoansPage } from '@/pages/admin/AdminLoansPage';

/** Racine : l'ADMIN n'a pas d'espace client, on le renvoie sur sa console. */
function Home() {
  const { isAdmin } = useAuth();
  return isAdmin ? <Navigate to="/admin" replace /> : <ClientDashboard />;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        {/* Espace client + ressources partagees */}
        <Route index element={<Home />} />
        <Route path="transfer" element={<TransferPage />} />
        <Route path="loans" element={<LoansPage />} />
        <Route path="loans/:id" element={<LoanDetailPage />} />
        <Route path="accounts/:id" element={<AccountDetailPage />} />
        <Route path="profile" element={<ProfilePage />} />

        {/* Console ADMIN */}
        <Route
          path="admin"
          element={
            <RequireRole role="ADMIN">
              <AdminDashboard />
            </RequireRole>
          }
        />
        <Route
          path="admin/clients"
          element={
            <RequireRole role="ADMIN">
              <ClientsPage />
            </RequireRole>
          }
        />
        <Route
          path="admin/clients/:id"
          element={
            <RequireRole role="ADMIN">
              <ClientDetailPage />
            </RequireRole>
          }
        />
        <Route
          path="admin/accounts"
          element={
            <RequireRole role="ADMIN">
              <AccountsPage />
            </RequireRole>
          }
        />
        <Route
          path="admin/loans"
          element={
            <RequireRole role="ADMIN">
              <AdminLoansPage />
            </RequireRole>
          }
        />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
