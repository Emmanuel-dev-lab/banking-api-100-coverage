package com.bank.application.service;

import com.bank.domain.model.Account;
import com.bank.domain.model.AccountStatus;
import com.bank.domain.model.Money;
import com.bank.domain.model.SavingsAccount;
import com.bank.domain.model.Transaction;
import com.bank.domain.model.TransactionType;
import com.bank.domain.port.AccountRepository;
import com.bank.domain.port.Clock;
import com.bank.domain.port.IdGenerator;
import com.bank.domain.port.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Capitalisation periodique des interets des comptes epargne. Declenchee par
 * un job planifie ou un endpoint d'administration.
 */
@Service
public class InterestService {

    private static final int PAGE = 100;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public InterestService(AccountRepository accountRepository, TransactionRepository transactionRepository,
                           IdGenerator idGenerator, Clock clock) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /**
     * Applique un mois d'interets a chaque compte epargne actif. Renvoie le
     * nombre de comptes effectivement credites.
     */
    @Transactional
    public int capitalizeSavings() {
        long total = accountRepository.count();
        int credited = 0;
        for (int offset = 0; offset < total; offset += PAGE) {
            List<Account> batch = accountRepository.findAll(offset, PAGE);
            for (Account account : batch) {
                if (account instanceof SavingsAccount savings && savings.status() == AccountStatus.ACTIVE) {
                    Money interest = savings.applyMonthlyInterest();
                    if (interest.amount() > 0) {
                        accountRepository.save(savings);
                        transactionRepository.save(new Transaction(idGenerator.newId(), savings.id(),
                                TransactionType.INTEREST, interest, clock.now(), null));
                        credited++;
                    }
                }
            }
        }
        return credited;
    }
}
