package com.bank.application.service;

import com.bank.domain.exception.AccountNotFoundException;
import com.bank.domain.exception.ClientNotFoundException;
import com.bank.domain.exception.ForbiddenException;
import com.bank.domain.exception.LoanNotFoundException;
import com.bank.domain.model.Account;
import com.bank.domain.model.Loan;
import com.bank.domain.model.Money;
import com.bank.domain.model.Page;
import com.bank.domain.model.PageRequest;
import com.bank.domain.model.Transaction;
import com.bank.domain.model.TransactionType;
import com.bank.domain.port.AccountRepository;
import com.bank.domain.port.ClientRepository;
import com.bank.domain.port.Clock;
import com.bank.domain.port.IdGenerator;
import com.bank.domain.port.LoanRepository;
import com.bank.domain.port.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

    private static final int OVERDUE_PAGE = 100;

    private final LoanRepository loanRepository;
    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public LoanService(LoanRepository loanRepository, ClientRepository clientRepository,
                       AccountRepository accountRepository, TransactionRepository transactionRepository,
                       IdGenerator idGenerator, Clock clock) {
        this.loanRepository = loanRepository;
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional
    public Loan requestLoan(String clientId, String accountId, long principal, double annualRate, int termMonths) {
        if (clientRepository.findById(clientId).isEmpty()) {
            throw new ClientNotFoundException(clientId);
        }
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (!account.clientId().equals(clientId)) {
            throw new ForbiddenException("account does not belong to client");
        }
        Loan loan = Loan.create(idGenerator.newId(), clientId, accountId,
                Money.of(principal), annualRate, termMonths, clock.today());
        account.deposit(loan.principal());
        accountRepository.save(account);
        loanRepository.save(loan);
        transactionRepository.save(new Transaction(idGenerator.newId(), accountId,
                TransactionType.LOAN_DISBURSEMENT, loan.principal(), clock.now(), null));
        return loan;
    }

    public Loan getLoan(String loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
    }

    @Transactional
    public void repayLoan(String loanId, long amount) {
        Money money = Money.ofPositive(amount);
        Loan loan = getLoan(loanId);
        Money applied = loan.repay(money);
        Account account = accountRepository.findByIdForUpdate(loan.accountId())
                .orElseThrow(() -> new AccountNotFoundException(loan.accountId()));
        account.withdraw(applied);
        accountRepository.save(account);
        loanRepository.save(loan);
        transactionRepository.save(new Transaction(idGenerator.newId(), loan.accountId(),
                TransactionType.LOAN_REPAYMENT, applied, clock.now(), null));
    }

    /**
     * Rafraichit le marqueur de retard de tous les prets et renvoie le nombre
     * de prets actuellement en retard. Declenche par un job ou un admin.
     */
    @Transactional
    public int flagOverdueLoans() {
        long total = loanRepository.count();
        int late = 0;
        for (int offset = 0; offset < total; offset += OVERDUE_PAGE) {
            for (Loan loan : loanRepository.findAll(offset, OVERDUE_PAGE)) {
                if (loan.refreshOverdue(clock.today())) {
                    late++;
                }
                loanRepository.save(loan);
            }
        }
        return late;
    }

    public Page<Loan> listLoans(int page, int size) {
        PageRequest pr = new PageRequest(page, size);
        return new Page<>(loanRepository.findAll(pr.offset(), pr.size()),
                loanRepository.count(), pr.page(), pr.size());
    }

    public Page<Loan> listClientLoans(String clientId, int page, int size) {
        if (clientRepository.findById(clientId).isEmpty()) {
            throw new ClientNotFoundException(clientId);
        }
        PageRequest pr = new PageRequest(page, size);
        return new Page<>(loanRepository.findByClientId(clientId, pr.offset(), pr.size()),
                loanRepository.countByClientId(clientId), pr.page(), pr.size());
    }
}
