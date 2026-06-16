package com.bank.infrastructure.persistence;

import com.bank.domain.model.Installment;
import com.bank.domain.model.Loan;
import com.bank.domain.model.LoanStatus;
import com.bank.domain.model.Money;
import com.bank.domain.port.LoanRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class LoanRepositoryAdapter implements LoanRepository {

    private final LoanJpaRepository jpa;

    public LoanRepositoryAdapter(LoanJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Loan loan) {
        List<InstallmentEmb> schedule = loan.schedule().stream()
                .map(i -> new InstallmentEmb(i.index(), i.dueDate(), i.amount().amount(),
                        i.principalPart().amount(), i.interestPart().amount(), i.paid()))
                .toList();
        jpa.save(new LoanJpa(loan.id(), loan.clientId(), loan.accountId(), loan.principal().amount(),
                loan.annualRate(), loan.termMonths(), loan.status(),
                loan.outstandingPrincipal().amount(), loan.startDate(), schedule));
    }

    @Override
    public Optional<Loan> findById(String id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Loan> findAll(int offset, int limit) {
        return jpa.findAll(PageRequest.of(offset / limit, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return jpa.count();
    }

    @Override
    public List<Loan> findByClientId(String clientId, int offset, int limit) {
        return jpa.findByClientId(clientId, PageRequest.of(offset / limit, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByClientId(String clientId) {
        return jpa.countByClientId(clientId);
    }

    private Loan toDomain(LoanJpa e) {
        List<Installment> schedule = e.getSchedule().stream()
                .map(i -> new Installment(i.getIdx(), i.getDueDate(), Money.of(i.getAmount()),
                        Money.of(i.getPrincipalPart()), Money.of(i.getInterestPart()), i.isPaid()))
                .toList();
        // Le capital restant peut etre <= 0 (dernier remboursement) -> fromStored.
        return Loan.restore(e.getId(), e.getClientId(), e.getAccountId(), Money.of(e.getPrincipal()),
                e.getAnnualRate(), e.getTermMonths(), e.getStartDate(),
                Money.fromStored(e.getOutstandingPrincipal()), e.getStatus(), schedule);
    }
}
