import type { TransactionType } from '@/api/types';

// Le franc CFA (XAF) n'a pas de decimale : on formate des entiers.
const xafFormatter = new Intl.NumberFormat('fr-FR', {
  maximumFractionDigits: 0,
});

// Intl(fr-FR) separe les milliers par une espace fine insecable (U+202F) ou
// une espace insecable (U+00A0) ; on normalise en espace simple pour un rendu
// et des tests stables.
function normalizeSpaces(value: string): string {
  return value.replace(/\s/g, " ");
}

/** Formate un montant XAF entier : 1234567 -> "1 234 567 FCFA". */
export function formatXAF(amount: number): string {
  return `${normalizeSpaces(xafFormatter.format(amount))} FCFA`;
}

/** Variante signee pour les mouvements (+/-). */
export function formatSignedXAF(amount: number): string {
  const sign = amount > 0 ? '+' : amount < 0 ? '−' : '';
  return `${sign}${normalizeSpaces(xafFormatter.format(Math.abs(amount)))} FCFA`;
}

const dateTimeFormatter = new Intl.DateTimeFormat('fr-FR', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

const dateFormatter = new Intl.DateTimeFormat('fr-FR', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
});

export function formatDateTime(iso: string): string {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : normalizeSpaces(dateTimeFormatter.format(d));
}

export function formatDate(iso: string): string {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : normalizeSpaces(dateFormatter.format(d));
}

/** Taux annuel 0.12 -> "12 %". */
export function formatRate(rate: number): string {
  return `${normalizeSpaces((rate * 100).toLocaleString('fr-FR', { maximumFractionDigits: 2 }))} %`;
}

const txnLabels: Record<TransactionType, string> = {
  DEPOSIT: 'Depot',
  WITHDRAWAL: 'Retrait',
  TRANSFER_IN: 'Virement recu',
  TRANSFER_OUT: 'Virement emis',
  LOAN_DISBURSEMENT: 'Decaissement pret',
  LOAN_REPAYMENT: 'Remboursement pret',
  INTEREST: 'Interets',
};

export function txnLabel(type: TransactionType): string {
  return txnLabels[type];
}

/** Sens comptable : credit (+) ou debit (-) pour le compte concerne. */
export function txnIsCredit(type: TransactionType): boolean {
  return (
    type === 'DEPOSIT' ||
    type === 'TRANSFER_IN' ||
    type === 'LOAN_DISBURSEMENT' ||
    type === 'INTEREST'
  );
}

export function initials(firstName: string, lastName: string): string {
  return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
}
