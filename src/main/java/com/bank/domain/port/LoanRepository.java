package com.bank.domain.port;

import com.bank.domain.model.Loan;

import java.util.Optional;

public interface LoanRepository {
    void save(Loan loan);

    Optional<Loan> findById(String id);
}
