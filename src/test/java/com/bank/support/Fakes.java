package com.bank.support;

import com.bank.domain.exception.UnauthorizedException;
import com.bank.domain.model.Account;
import com.bank.domain.model.Client;
import com.bank.domain.model.Loan;
import com.bank.domain.model.Transaction;
import com.bank.domain.model.User;
import com.bank.domain.port.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementations in-memory deterministes des ports, pour les tests unitaires
 * de la couche application. Aucune ne contient de logique branchante metier
 * (elles sont hors perimetre de couverture, en src/test).
 */
public final class Fakes {

    private Fakes() {
    }

    public static final class InMemoryClientRepository implements ClientRepository {
        private final Map<String, Client> store = new HashMap<>();

        @Override
        public void save(Client client) {
            store.put(client.id(), client);
        }

        @Override
        public Optional<Client> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }
    }

    public static final class InMemoryAccountRepository implements AccountRepository {
        private final Map<String, Account> store = new HashMap<>();

        @Override
        public void save(Account account) {
            store.put(account.id(), account);
        }

        @Override
        public Optional<Account> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }
    }

    public static final class InMemoryTransactionRepository implements TransactionRepository {
        private final List<Transaction> store = new ArrayList<>();

        @Override
        public void save(Transaction transaction) {
            store.add(transaction);
        }

        @Override
        public List<Transaction> findByAccountId(String accountId) {
            List<Transaction> result = new ArrayList<>();
            for (Transaction t : store) {
                if (t.accountId().equals(accountId)) {
                    result.add(t);
                }
            }
            return result;
        }

        public int count() {
            return store.size();
        }
    }

    public static final class InMemoryLoanRepository implements LoanRepository {
        private final Map<String, Loan> store = new HashMap<>();

        @Override
        public void save(Loan loan) {
            store.put(loan.id(), loan);
        }

        @Override
        public Optional<Loan> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }
    }

    public static final class InMemoryUserRepository implements UserRepository {
        private final Map<String, User> store = new HashMap<>();

        @Override
        public void save(User user) {
            store.put(user.username(), user);
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(store.get(username));
        }
    }

    public static final class FixedClock implements Clock {
        private final LocalDate today;

        public FixedClock(LocalDate today) {
            this.today = today;
        }

        @Override
        public LocalDate today() {
            return today;
        }
    }

    public static final class SequentialIdGenerator implements IdGenerator {
        private int counter = 0;

        @Override
        public String newId() {
            counter++;
            return "id-" + counter;
        }
    }

    /** Hash trivial deterministe : "h:" + raw. */
    public static final class FakePasswordHasher implements PasswordHasher {
        @Override
        public String hash(String raw) {
            return "h:" + raw;
        }

        @Override
        public boolean matches(String raw, String hash) {
            return hash.equals("h:" + raw);
        }
    }

    /** Jeton = "tok-" + userId ; verify decode via la table peuplee par issue. */
    public static final class FakeTokenService implements TokenService {
        private final Map<String, TokenClaims> issued = new HashMap<>();

        @Override
        public String issue(User user) {
            String token = "tok-" + user.id();
            issued.put(token, new TokenClaims(user.id(), user.clientId(), user.role()));
            return token;
        }

        @Override
        public TokenClaims verify(String token) {
            TokenClaims claims = issued.get(token);
            if (claims == null) {
                throw new UnauthorizedException("invalid token");
            }
            return claims;
        }
    }
}
