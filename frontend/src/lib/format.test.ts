import { describe, expect, it } from 'vitest';
import { formatXAF, formatSignedXAF, formatRate, txnIsCredit, initials } from './format';

describe('format', () => {
  it('formate un entier XAF avec separateurs et sans decimale', () => {
    expect(formatXAF(1234567)).toBe('1 234 567 FCFA');
    expect(formatXAF(0)).toBe('0 FCFA');
  });

  it('prefixe le signe des montants signes', () => {
    expect(formatSignedXAF(500)).toBe('+500 FCFA');
    expect(formatSignedXAF(-500)).toBe('−500 FCFA');
    expect(formatSignedXAF(0)).toBe('0 FCFA');
  });

  it('formate un taux en pourcentage', () => {
    expect(formatRate(0.12)).toBe('12 %');
  });

  it('classe credits et debits', () => {
    expect(txnIsCredit('DEPOSIT')).toBe(true);
    expect(txnIsCredit('TRANSFER_IN')).toBe(true);
    expect(txnIsCredit('LOAN_DISBURSEMENT')).toBe(true);
    expect(txnIsCredit('WITHDRAWAL')).toBe(false);
    expect(txnIsCredit('TRANSFER_OUT')).toBe(false);
    expect(txnIsCredit('LOAN_REPAYMENT')).toBe(false);
  });

  it('derive les initiales', () => {
    expect(initials('John', 'Doe')).toBe('JD');
  });
});
