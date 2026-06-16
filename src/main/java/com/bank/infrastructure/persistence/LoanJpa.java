package com.bank.infrastructure.persistence;

import com.bank.domain.model.LoanStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loans")
public class LoanJpa {

    @Id
    private String id;
    private String clientId;
    private String accountId;
    private long principal;
    private double annualRate;
    private int termMonths;
    @Enumerated(EnumType.STRING)
    private LoanStatus status;
    private long outstandingPrincipal;
    private LocalDate startDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "installments", joinColumns = @JoinColumn(name = "loan_id"))
    @OrderColumn(name = "line_no")
    private List<InstallmentEmb> schedule = new ArrayList<>();

    protected LoanJpa() {
    }

    public LoanJpa(String id, String clientId, String accountId, long principal, double annualRate,
                   int termMonths, LoanStatus status, long outstandingPrincipal, LocalDate startDate,
                   List<InstallmentEmb> schedule) {
        this.id = id;
        this.clientId = clientId;
        this.accountId = accountId;
        this.principal = principal;
        this.annualRate = annualRate;
        this.termMonths = termMonths;
        this.status = status;
        this.outstandingPrincipal = outstandingPrincipal;
        this.startDate = startDate;
        this.schedule = schedule;
    }

    public String getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getAccountId() {
        return accountId;
    }

    public long getPrincipal() {
        return principal;
    }

    public double getAnnualRate() {
        return annualRate;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public long getOutstandingPrincipal() {
        return outstandingPrincipal;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public List<InstallmentEmb> getSchedule() {
        return schedule;
    }
}
