package com.bank.application.service;

import com.bank.domain.exception.AccountNotFoundException;
import com.bank.domain.exception.ClientNotFoundException;
import com.bank.domain.model.Account;
import com.bank.domain.model.AccountType;
import com.bank.domain.model.CurrentAccount;
import com.bank.domain.model.Money;
import com.bank.domain.model.Page;
import com.bank.domain.model.PageRequest;
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

    /** Chargement verrouille pour une mutation de solde (anti lost-update). */
    private Account getAccountForUpdate(String id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional
    public Account deposit(String accountId, long amount) {
        Money money = Money.ofPositive(amount);
        Account account = getAccountForUpdate(accountId);
        account.deposit(money);
        accountRepository.save(account);
        record(accountId, TransactionType.DEPOSIT, money, null);
        return account;
    }

    @Transactional
    public Account withdraw(String accountId, long amount) {
        Money money = Money.ofPositive(amount);
        Account account = getAccountForUpdate(accountId);
        account.withdraw(money);
        accountRepository.save(account);
        record(accountId, TransactionType.WITHDRAWAL, money, null);
        return account;
    }

    @Transactional
    public Account freeze(String accountId) {
        Account account = getAccountForUpdate(accountId);
        account.freeze();
        accountRepository.save(account);
        return account;
    }

    @Transactional
    public Account close(String accountId) {
        Account account = getAccountForUpdate(accountId);
        account.close();
        accountRepository.save(account);
        return account;
    }

    @Transactional
    public Account reactivate(String accountId) {
        Account account = getAccountForUpdate(accountId);
        account.reactivate();
        accountRepository.save(account);
        return account;
    }

    public Page<Transaction> listTransactions(String accountId, int page, int size) {
        getAccount(accountId);
        PageRequest pr = new PageRequest(page, size);
        return new Page<>(transactionRepository.findByAccountId(accountId, pr.offset(), pr.size()),
                transactionRepository.countByAccountId(accountId), pr.page(), pr.size());
    }

    public Page<Account> listAccounts(int page, int size) {
        PageRequest pr = new PageRequest(page, size);
        return new Page<>(accountRepository.findAll(pr.offset(), pr.size()),
                accountRepository.count(), pr.page(), pr.size());
    }

    public Page<Account> listClientAccounts(String clientId, int page, int size) {
        if (clientRepository.findById(clientId).isEmpty()) {
            throw new ClientNotFoundException(clientId);
        }
        PageRequest pr = new PageRequest(page, size);
        return new Page<>(accountRepository.findByClientId(clientId, pr.offset(), pr.size()),
                accountRepository.countByClientId(clientId), pr.page(), pr.size());
    }

    private void record(String accountId, TransactionType type, Money amount, String relatedAccountId) {
        transactionRepository.save(new Transaction(
                idGenerator.newId(), accountId, type, amount, clock.now(), relatedAccountId));
    }
}
