package com.bank.application.service;

import com.bank.domain.exception.AccountNotFoundException;
import com.bank.domain.exception.ClientNotFoundException;
import com.bank.domain.model.Account;
import com.bank.domain.model.AccountType;
import com.bank.domain.model.CurrentAccount;
import com.bank.domain.model.Money;
import com.bank.domain.model.SavingsAccount;
import com.bank.domain.model.Transaction;
import com.bank.domain.model.TransactionType;
import com.bank.domain.port.AccountRepository;
import com.bank.domain.port.ClientRepository;
import com.bank.domain.port.Clock;
import com.bank.domain.port.IdGenerator;
import com.bank.domain.port.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public AccountService(AccountRepository accountRepository, ClientRepository clientRepository,
                          TransactionRepository transactionRepository, IdGenerator idGenerator, Clock clock) {
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
        this.transactionRepository = transactionRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional
    public Account openAccount(String clientId, AccountType type, long overdraftLimit, double annualRate) {
        if (clientRepository.findById(clientId).isEmpty()) {
            throw new ClientNotFoundException(clientId);
        }
        String id = idGenerator.newId();
        Account account;
        if (type == AccountType.CURRENT) {
            if (overdraftLimit < 0) {
                throw new IllegalArgumentException("overdraftLimit must be >= 0");
            }
            account = new CurrentAccount(id, clientId, Money.ZERO, overdraftLimit);
        } else {
            if (annualRate < 0) {
                throw new IllegalArgumentException("annualRate must be >= 0");
            }
            account = new SavingsAccount(id, clientId, Money.ZERO, annualRate);
        }
        accountRepository.save(account);
        return account;
    }

    public Account getAccount(String id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional
    public Account deposit(String accountId, long amount) {
        Money money = Money.ofPositive(amount);
        Account account = getAccount(accountId);
        account.deposit(money);
        accountRepository.save(account);
        record(accountId, TransactionType.DEPOSIT, money, null);
        return account;
    }

    @Transactional
    public Account withdraw(String accountId, long amount) {
        Money money = Money.ofPositive(amount);
        Account account = getAccount(accountId);
        account.withdraw(money);
        accountRepository.save(account);
        record(accountId, TransactionType.WITHDRAWAL, money, null);
        return account;
    }

    public List<Transaction> getHistory(String accountId) {
        getAccount(accountId);
        return transactionRepository.findByAccountId(accountId);
    }

    private void record(String accountId, TransactionType type, Money amount, String relatedAccountId) {
        transactionRepository.save(new Transaction(
                idGenerator.newId(), accountId, type, amount, clock.today(), relatedAccountId));
    }
}
