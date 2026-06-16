import { Link } from 'react-router-dom';
import type { AccountResponse } from '@/api/types';
import { AccountStatusBadge, AccountTypeTag } from './ui/Badge';
import { Money } from './ui/Money';
import { Card } from './ui/Card';

/** Carte compte cliquable menant au detail. */
export function AccountCard({ account }: { account: AccountResponse }) {
  return (
    <Link to={`/accounts/${account.id}`} className="group block">
      <Card className="p-5 transition-all duration-150 hover:-translate-y-0.5 hover:border-line-strong hover:shadow-[0_14px_30px_-18px_rgba(40,35,20,0.4)]">
        <div className="flex items-center justify-between">
          <AccountTypeTag type={account.type} />
          <AccountStatusBadge status={account.status} />
        </div>
        <p className="mt-4 text-[13px] text-ink-faint">Solde</p>
        <Money amount={account.balance} colored className="mt-0.5 text-[24px]" />
        <p className="mt-3 font-mono text-[11.5px] tracking-tight text-ink-faint">
          {account.id.slice(0, 8)}… ·{' '}
          <span className="text-forest-deep opacity-0 transition-opacity group-hover:opacity-100">
            ouvrir →
          </span>
        </p>
      </Card>
    </Link>
  );
}
