package com.bank.infrastructure.seed;

import com.bank.application.service.AccountService;
import com.bank.application.service.ClientService;
import com.bank.application.service.LoanService;
import com.bank.application.service.TransferService;
import com.bank.domain.model.Account;
import com.bank.domain.model.AccountType;
import com.bank.domain.model.Client;
import com.bank.domain.model.Role;
import com.bank.domain.model.User;
import com.bank.domain.port.IdGenerator;
import com.bank.domain.port.PasswordHasher;
import com.bank.domain.port.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seed de demonstration. Idempotent : ne fait rien si l'utilisateur "admin"
 * existe deja. Active uniquement quand app.seed.enabled=true (profil prod).
 * Code d'infrastructure : hors perimetre de couverture.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private static final int CLIENT_COUNT = 50;
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String CLIENT_PASSWORD = "client123";

    private static final String[] FIRST_NAMES = {
            "Jean", "Marie", "Paul", "Aline", "Eric", "Sandrine", "Yves", "Nadine",
            "Serge", "Brigitte", "Claude", "Estelle", "Patrick", "Carine", "Roger",
            "Honorine", "Bertrand", "Solange", "Armand", "Pauline"
    };
    private static final String[] LAST_NAMES = {
            "Mballa", "Nguema", "Fotso", "Kamga", "Ndongo", "Bekolo", "Tchami",
            "Owona", "Ngono", "Essomba", "Manga", "Atangana", "Biya", "Eyenga",
            "Ze", "Ondoua", "Mvogo", "Abega", "Nkodo", "Ekani"
    };

    private final ClientService clientService;
    private final AccountService accountService;
    private final TransferService transferService;
    private final LoanService loanService;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final IdGenerator idGenerator;

    public DatabaseSeeder(ClientService clientService, AccountService accountService,
                          TransferService transferService, LoanService loanService,
                          UserRepository userRepository, PasswordHasher passwordHasher,
                          IdGenerator idGenerator) {
        this.clientService = clientService;
        this.accountService = accountService;
        this.transferService = transferService;
        this.loanService = loanService;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.idGenerator = idGenerator;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername(ADMIN_USERNAME).isPresent()) {
            log.info("Seed ignore : donnees deja presentes.");
            return;
        }
        log.info("Seed en cours...");
        seedAdmin();
        Random random = new Random(42);
        int loans = 0;
        int transfers = 0;

        for (int i = 1; i <= CLIENT_COUNT; i++) {
            Client client = clientService.createClient(
                    FIRST_NAMES[i % FIRST_NAMES.length],
                    LAST_NAMES[(i * 3) % LAST_NAMES.length],
                    "client" + i,
                    CLIENT_PASSWORD);

            List<Account> accounts = openAccounts(client, random);

            // Approvisionnement et mouvements
            for (Account account : accounts) {
                long deposit = (10 + random.nextInt(490)) * 1000L; // 10k..500k XAF
                accountService.deposit(account.id(), deposit);
            }

            // Virement interne courant -> epargne quand les deux existent
            if (accounts.size() >= 2) {
                long amount = (5 + random.nextInt(45)) * 1000L;
                transferService.transfer(accounts.get(0).id(), accounts.get(1).id(), amount);
                transfers++;
            }

            // Quelques retraits sur le premier compte
            if (random.nextBoolean()) {
                accountService.withdraw(accounts.get(0).id(), (1 + random.nextInt(20)) * 1000L);
            }

            // ~30% des clients contractent un pret sur leur compte courant
            if (random.nextInt(10) < 3) {
                long principal = (100 + random.nextInt(900)) * 1000L; // 100k..1M
                double rate = 0.05 + random.nextInt(16) / 100.0;      // 5%..20%
                int term = 6 + random.nextInt(31);                    // 6..36 mois
                var loan = loanService.requestLoan(client.id(), accounts.get(0).id(), principal, rate, term);
                loans++;
                // Rembourse une partie pour la moitie des prets
                if (random.nextBoolean()) {
                    loanService.repayLoan(loan.id(), Math.max(1000L, principal / 10));
                }
            }
        }

        log.info("Seed termine : 1 admin, {} clients, {} virements, {} prets.",
                CLIENT_COUNT, transfers, loans);
    }

    private void seedAdmin() {
        User admin = new User(idGenerator.newId(), ADMIN_USERNAME,
                passwordHasher.hash(ADMIN_PASSWORD), Role.ADMIN, null);
        userRepository.save(admin);
    }

    private List<Account> openAccounts(Client client, Random random) {
        List<Account> accounts = new ArrayList<>();
        // Toujours un compte courant
        accounts.add(accountService.openAccount(client.id(), AccountType.CURRENT,
                (random.nextInt(6)) * 50_000L, 0.0)); // decouvert 0..250k
        // ~70% ont aussi un compte epargne
        if (random.nextInt(10) < 7) {
            accounts.add(accountService.openAccount(client.id(), AccountType.SAVINGS,
                    0, 0.03 + random.nextInt(4) / 100.0)); // 3%..6%
        }
        return accounts;
    }
}
