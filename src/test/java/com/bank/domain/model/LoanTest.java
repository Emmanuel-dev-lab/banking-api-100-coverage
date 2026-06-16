package com.bank.domain.model;

import com.bank.domain.exception.InvalidLoanTermsException;
import com.bank.domain.exception.LoanAlreadyClosedException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    private Loan loan(long principal, double rate, int term) {
        return Loan.create("l1", "c1", "a1", Money.of(principal), rate, term, START);
    }

    // L1
    @Test
    void create_nonPositivePrincipal_throws() {
        assertThatThrownBy(() -> Loan.create("l1", "c1", "a1", Money.of(0), 0.1, 12, START))
                .isInstanceOf(InvalidLoanTermsException.class);
    }

    // L2
    @Test
    void create_nonPositiveTerm_throws() {
        assertThatThrownBy(() -> loan(100000, 0.1, 0))
                .isInstanceOf(InvalidLoanTermsException.class);
    }

    // L3
    @Test
    void create_negativeRate_throws() {
        assertThatThrownBy(() -> loan(100000, -0.01, 12))
                .isInstanceOf(InvalidLoanTermsException.class);
    }

    // L4
    @Test
    void create_valid_ok() {
        Loan l = loan(120000, 0.0, 12);
        assertThat(l.id()).isEqualTo("l1");
        assertThat(l.clientId()).isEqualTo("c1");
        assertThat(l.accountId()).isEqualTo("a1");
        assertThat(l.principal().amount()).isEqualTo(120000);
        assertThat(l.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(l.outstandingPrincipal().amount()).isEqualTo(120000);
        assertThat(l.annualRate()).isEqualTo(0.0);
        assertThat(l.termMonths()).isEqualTo(12);
        assertThat(l.startDate()).isEqualTo(START);
    }

    // reconstitution (persistance)
    @Test
    void restore_rebuildsState() {
        Loan original = loan(120000, 0.12, 6);
        Loan restored = Loan.restore("l1", "c1", "a1", Money.of(120000), 0.12, 6, START,
                Money.of(60000), LoanStatus.ACTIVE, original.schedule());
        assertThat(restored.outstandingPrincipal().amount()).isEqualTo(60000);
        assertThat(restored.schedule()).hasSize(6);
        assertThat(restored.status()).isEqualTo(LoanStatus.ACTIVE);
    }

    // L5
    @Test
    void payment_zeroRate_principalOverTerm() {
        Loan l = loan(100000, 0.0, 12);
        // ceil(100000/12) = 8334
        assertThat(l.monthlyPayment().amount()).isEqualTo(8334);
    }

    // L6
    @Test
    void payment_positiveRate_amortized() {
        Loan l = loan(1200000, 0.12, 12); // taux mensuel 1%
        // mensualite amortissement constant ~ 106619
        assertThat(l.monthlyPayment().amount()).isEqualTo(106619);
    }

    // L7
    @Test
    void schedule_size_equalsTerm() {
        assertThat(loan(120000, 0.12, 6).schedule()).hasSize(6);
    }

    // L8
    @Test
    void schedule_lastInstallment_balancesPrincipal() {
        Loan l = loan(120000, 0.12, 6);
        long totalPrincipal = l.schedule().stream()
                .mapToLong(i -> i.principalPart().amount())
                .sum();
        assertThat(totalPrincipal).isEqualTo(120000);
    }

    // L9
    @Test
    void repay_alreadyPaidOff_throws() {
        Loan l = loan(100000, 0.0, 12);
        l.repay(Money.of(100000)); // solde
        assertThatThrownBy(() -> l.repay(Money.of(1)))
                .isInstanceOf(LoanAlreadyClosedException.class);
    }

    // L10
    @Test
    void repay_partial_stillActive() {
        Loan l = loan(100000, 0.0, 12);
        l.repay(Money.of(40000));
        assertThat(l.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(l.outstandingPrincipal().amount()).isEqualTo(60000);
    }

    // L11
    @Test
    void repay_full_paidOff() {
        Loan l = loan(100000, 0.0, 12);
        Money applied = l.repay(Money.of(100000));
        assertThat(applied.amount()).isEqualTo(100000);
        assertThat(l.status()).isEqualTo(LoanStatus.PAID_OFF);
    }

    // L11b : sur-paiement borne au capital restant, renvoie le montant applique
    @Test
    void repay_overpayment_clampedToOutstanding() {
        Loan l = loan(100000, 0.0, 12);
        Money applied = l.repay(Money.of(150000));
        assertThat(applied.amount()).isEqualTo(100000);
        assertThat(l.outstandingPrincipal().amount()).isZero();
        assertThat(l.status()).isEqualTo(LoanStatus.PAID_OFF);
    }

    // L12
    @Test
    void isLate_overdueUnpaid_true() {
        Loan l = loan(120000, 0.0, 6);
        // premiere echeance 2026-02-01, aujourd'hui apres
        assertThat(l.isLate(LocalDate.of(2026, 3, 1))).isTrue();
    }

    // L13 : sous-condition !paid fausse + sortie de boucle false
    @Test
    void isLate_allPaid_false() {
        Loan l = loan(120000, 0.0, 6);
        l.schedule().forEach(Installment::markPaid);
        assertThat(l.isLate(LocalDate.of(2030, 1, 1))).isFalse();
    }

    // L14 : echeance non echue (dueDate future)
    @Test
    void isLate_futureDue_false() {
        Loan l = loan(120000, 0.0, 6);
        assertThat(l.isLate(LocalDate.of(2026, 1, 15))).isFalse();
    }

    @Test
    void installment_accessors() {
        Installment i = loan(120000, 0.12, 6).schedule().get(0);
        assertThat(i.index()).isZero();
        assertThat(i.dueDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(i.amount().amount()).isPositive();
        assertThat(i.interestPart().amount()).isPositive();
        assertThat(i.paid()).isFalse();
    }
}
