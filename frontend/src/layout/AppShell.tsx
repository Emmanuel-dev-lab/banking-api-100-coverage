import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/AuthContext';
import { cn } from '@/lib/cn';

interface NavItem {
  to: string;
  label: string;
  glyph: string;
  end?: boolean;
}

const clientNav: NavItem[] = [
  { to: '/', label: 'Tableau de bord', glyph: '◰', end: true },
  { to: '/transfer', label: 'Virement', glyph: '⇄' },
  { to: '/loans', label: 'Mes prêts', glyph: '❡' },
  { to: '/profile', label: 'Profil', glyph: '◔' },
];

const adminNav: NavItem[] = [
  { to: '/admin', label: 'Vue générale', glyph: '◰', end: true },
  { to: '/admin/clients', label: 'Clients', glyph: '⊞' },
  { to: '/admin/accounts', label: 'Comptes', glyph: '▤' },
  { to: '/admin/loans', label: 'Prêts', glyph: '❡' },
];

export function AppShell() {
  const { auth, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const items = isAdmin ? adminNav : clientNav;

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="flex min-h-screen">
      {/* Rail lateral */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-30 flex w-[248px] flex-col border-r border-line bg-paper-raised/80 backdrop-blur-sm transition-transform lg:static lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex items-center gap-2.5 px-5 py-6">
          <div className="flex size-9 items-center justify-center rounded-[9px] bg-forest text-paper-raised">
            <span className="font-display text-lg font-semibold text-brass-soft">C</span>
          </div>
          <div className="leading-tight">
            <p className="font-display text-[18px] font-semibold text-ink">Comptoir</p>
            <p className="text-[11px] uppercase tracking-[0.12em] text-ink-faint">
              {isAdmin ? 'Console' : 'Espace client'}
            </p>
          </div>
        </div>

        <nav className="flex-1 px-3 py-2">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                cn(
                  'mb-1 flex items-center gap-3 rounded-[10px] px-3 py-2.5 text-[14.5px] transition-colors',
                  isActive
                    ? 'bg-forest text-paper-raised shadow-[0_6px_16px_-10px_rgba(20,65,46,0.8)]'
                    : 'text-ink-soft hover:bg-forest-soft hover:text-forest-deep',
                )
              }
            >
              <span className="w-4 text-center text-[15px] opacity-90">{item.glyph}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-line p-3">
          <div className="mb-2 px-2 text-[12px] text-ink-faint">
            <span className="font-mono">{auth?.role === 'ADMIN' ? 'ADMIN' : 'CLIENT'}</span>
          </div>
          <button
            onClick={handleLogout}
            className="flex w-full items-center gap-3 rounded-[10px] px-3 py-2.5 text-[14.5px] text-ink-soft transition-colors hover:bg-danger-soft hover:text-danger"
          >
            <span className="w-4 text-center">⏻</span>
            Se déconnecter
          </button>
        </div>
      </aside>

      {open && (
        <div className="fixed inset-0 z-20 bg-black/20 lg:hidden" onClick={() => setOpen(false)} />
      )}

      {/* Contenu */}
      <div className="flex min-w-0 flex-1 flex-col">
        <div className="flex items-center gap-3 border-b border-line px-4 py-3 lg:hidden">
          <button
            onClick={() => setOpen(true)}
            className="rounded-[9px] border border-line-strong px-3 py-1.5 text-ink-soft"
            aria-label="Menu"
          >
            ☰
          </button>
          <span className="font-display text-lg">Comptoir</span>
        </div>
        <main className="mx-auto w-full max-w-6xl flex-1 px-5 py-8 lg:px-10 lg:py-12">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
