package com.bank.infrastructure.persistence;

import com.bank.domain.model.Account;
import com.bank.domain.model.AccountType;
import com.bank.domain.model.CurrentAccount;
import com.bank.domain.model.Money;
import com.bank.domain.model.SavingsAccount;
import com.bank.domain.port.AccountRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository jpa;

    public AccountRepositoryAdapter(AccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Account account) {
        Long overdraft = null;
        Double rate = null;
        if (account instanceof CurrentAccount current) {
            overdraft = current.overdraftLimit();
        } else if (account instanceof SavingsAccount savings) {
            rate = savings.annualRate();
        }
        jpa.save(new AccountJpa(account.id(), account.clientId(), account.type(),
                account.balance().amount(), account.status(), overdraft, rate));
    }

    @Override
    public Optional<Account> findById(String id) {
        return jpa.findById(id).map(this::toDomain);
    }

    private Account toDomain(AccountJpa e) {
        if (e.getType() == AccountType.CURRENT) {
            return new CurrentAccount(e.getId(), e.getClientId(), Money.of(e.getBalance()),
                    e.getStatus(), e.getOverdraftLimit());
        }
        return new SavingsAccount(e.getId(), e.getClientId(), Money.of(e.getBalance()),
                e.getStatus(), e.getAnnualRate());
    }
}
