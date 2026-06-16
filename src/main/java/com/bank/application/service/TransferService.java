package com.bank.application.service;

import com.bank.domain.exception.AccountNotFoundException;
import com.bank.domain.exception.SameAccountTransferException;
import com.bank.domain.model.Account;
import com.bank.domain.model.Money;
import com.bank.domain.model.Transaction;
import com.bank.domain.model.TransactionType;
import com.bank.domain.port.AccountRepository;
import com.bank.domain.port.Clock;
import com.bank.domain.port.IdGenerator;
import com.bank.domain.port.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public TransferService(AccountRepository accountRepository, TransactionRepository transactionRepository,
                           IdGenerator idGenerator, Clock clock) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional
    public void transfer(String sourceId, String destId, long amount) {
        Money money = Money.ofPositive(amount);
        if (sourceId.equals(destId)) {
            throw new SameAccountTransferException(sourceId);
        }
        Account source = accountRepository.findByIdForUpdate(sourceId)
                .orElseThrow(() -> new AccountNotFoundException(sourceId));
        Account dest = accountRepository.findByIdForUpdate(destId)
                .orElseThrow(() -> new AccountNotFoundException(destId));
        source.ensureActive();
        dest.ensureActive();
        source.withdraw(money);
        dest.deposit(money);
        accountRepository.save(source);
        accountRepository.save(dest);
        record(sourceId, TransactionType.TRANSFER_OUT, money, destId);
        record(destId, TransactionType.TRANSFER_IN, money, sourceId);
    }

    private void record(String accountId, TransactionType type, Money amount, String relatedAccountId) {
        transactionRepository.save(new Transaction(
                idGenerator.newId(), accountId, type, amount, clock.now(), relatedAccountId));
    }
}
