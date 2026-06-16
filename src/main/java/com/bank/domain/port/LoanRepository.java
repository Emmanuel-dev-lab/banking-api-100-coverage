package com.bank.domain.port;

import com.bank.domain.model.Loan;

import java.util.List;
import java.util.Optional;

public interface LoanRepository {
    void save(Loan loan);

    Optional<Loan> findById(String id);

    List<Loan> findAll(int offset, int limit);

    long count();

    List<Loan> findByClientId(String clientId, int offset, int limit);

    long countByClientId(String clientId);
}
