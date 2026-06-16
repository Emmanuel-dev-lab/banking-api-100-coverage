package com.bank.domain.model;

import com.bank.domain.exception.InvalidLoanTermsException;
import com.bank.domain.exception.LoanAlreadyClosedException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Agregat pret a amortissement constant. */
public class Loan {

    private final String id;
    private final String clientId;
    private final String accountId;
    private final Money principal;
    private final double annualRate;
    private final int termMonths;
    private final LocalDate startDate;
    private final List<Installment> schedule = new ArrayList<>();
    private Money outstandingPrincipal;
    private LoanStatus status;

    private Loan(String id, String clientId, String accountId, Money principal,
                 double annualRate, int termMonths, LocalDate startDate) {
        this.id = id;
        this.clientId = clientId;
        this.accountId = accountId;
        this.principal = principal;
        this.annualRate = annualRate;
        this.termMonths = termMonths;
        this.startDate = startDate;
        this.outstandingPrincipal = principal;
        this.status = LoanStatus.ACTIVE;
    }

    public static Loan create(String id, String clientId, String accountId, Money principal,
                              double annualRate, int termMonths, LocalDate startDate) {
        if (principal.amount() <= 0) {
            throw new InvalidLoanTermsException("principal must be > 0");
        }
        if (termMonths <= 0) {
            throw new InvalidLoanTermsException("termMonths must be > 0");
        }
        if (annualRate < 0) {
            throw new InvalidLoanTermsException("annualRate must be >= 0");
        }
        Loan loan = new Loan(id, clientId, accountId, principal, annualRate, termMonths, startDate);
        loan.buildSchedule();
        return loan;
    }

    /** Mensualite (amortissement constant). Cas taux nul : capital / duree arrondi superieur. */
    /** Reconstitution depuis la persistance (etat complet, sans recalcul). */
    public static Loan restore(String id, String clientId, String accountId, Money principal,
                               double annualRate, int termMonths, LocalDate startDate,
                               Money outstandingPrincipal, LoanStatus status, List<Installment> schedule) {
        Loan loan = new Loan(id, clientId, accountId, principal, annualRate, termMonths, startDate);
        loan.outstandingPrincipal = outstandingPrincipal;
        loan.status = status;
        loan.schedule.addAll(schedule);
        return loan;
    }

    public double annualRate() {
        return annualRate;
    }

    public int termMonths() {
        return termMonths;
    }

    public java.time.LocalDate startDate() {
        return startDate;
    }

    public Money monthlyPayment() {
        double monthlyRate = annualRate / 12.0;
        if (monthlyRate == 0.0) {
            return Money.of((long) Math.ceil((double) principal.amount() / termMonths));
        }
        double payment = principal.amount() * monthlyRate
                / (1 - Math.pow(1 + monthlyRate, -termMonths));
        return Money.of(Math.round(payment));
    }

    private void buildSchedule() {
        double monthlyRate = annualRate / 12.0;
        Money payment = monthlyPayment();
        long remaining = principal.amount();
        for (int i = 0; i < termMonths; i++) {
            long interest = Math.round(remaining * monthlyRate);
            long principalPart;
            long pay;
            if (i == termMonths - 1) {
                principalPart = remaining;
                pay = remaining + interest;
            } else {
                pay = payment.amount();
                principalPart = pay - interest;
            }
            remaining -= principalPart;
            schedule.add(new Installment(
                    i,
                    startDate.plusMonths(i + 1L),
                    Money.of(pay),
                    Money.of(principalPart),
                    Money.of(interest)));
        }
    }

    public void repay(Money amount) {
        if (status == LoanStatus.PAID_OFF) {
            throw new LoanAlreadyClosedException(id);
        }
        outstandingPrincipal = outstandingPrincipal.minus(amount);
        if (outstandingPrincipal.amount() <= 0) {
            status = LoanStatus.PAID_OFF;
        }
    }

    public boolean isLate(LocalDate today) {
        for (Installment installment : schedule) {
            if (!installment.paid() && installment.dueDate().isBefore(today)) {
                return true;
            }
        }
        return false;
    }

    public String id() {
        return id;
    }

    public String clientId() {
        return clientId;
    }

    public String accountId() {
        return accountId;
    }

    public Money principal() {
        return principal;
    }

    public Money outstandingPrincipal() {
        return outstandingPrincipal;
    }

    public LoanStatus status() {
        return status;
    }

    public List<Installment> schedule() {
        return schedule;
    }
}
