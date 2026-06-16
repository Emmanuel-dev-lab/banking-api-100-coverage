package com.bank.infrastructure.schedule;

import com.bank.application.service.InterestService;
import com.bank.application.service.LoanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Declencheurs planifies des traitements periodiques. Couche d'infrastructure
 * (hors perimetre de couverture) : ne contient aucune logique metier, delegue
 * aux services applicatifs testes.
 */
@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final InterestService interestService;
    private final LoanService loanService;

    public ScheduledJobs(InterestService interestService, LoanService loanService) {
        this.interestService = interestService;
        this.loanService = loanService;
    }

    /** Capitalisation des interets epargne : le 1er de chaque mois a 02:00. */
    @Scheduled(cron = "${app.jobs.interest-cron:0 0 2 1 * *}")
    public void capitalizeSavingsInterest() {
        int credited = interestService.capitalizeSavings();
        log.info("Job interets epargne : {} compte(s) credite(s).", credited);
    }

    /** Marquage des prets en retard : chaque jour a 03:00. */
    @Scheduled(cron = "${app.jobs.overdue-cron:0 0 3 * * *}")
    public void flagOverdueLoans() {
        int late = loanService.flagOverdueLoans();
        log.info("Job retard prets : {} pret(s) en retard.", late);
    }
}
