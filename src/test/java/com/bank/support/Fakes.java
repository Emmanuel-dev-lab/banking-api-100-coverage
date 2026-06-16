package com.bank.support;

import com.bank.domain.exception.UnauthorizedException;
import com.bank.domain.model.Account;
import com.bank.domain.model.Client;
import com.bank.domain.model.Loan;
import com.bank.domain.model.Transaction;
import com.bank.domain.model.User;
import com.bank.domain.port.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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

        @Override
        public List<Client> findAll(int offset, int limit) {
            return store.values().stream().skip(offset).limit(limit).toList();
        }

        @Override
        public long count() {
            return store.size();
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

        @Override
        public Optional<Account> findByIdForUpdate(String id) {
            return findById(id);
        }

        @Override
        public List<Account> findAll(int offset, int limit) {
            return store.values().stream().skip(offset).limit(limit).toList();
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public List<Account> findByClientId(String clientId, int offset, int limit) {
            return store.values().stream()
                    .filter(a -> a.clientId().equals(clientId))
                    .skip(offset).limit(limit).toList();
        }

        @Override
        public long countByClientId(String clientId) {
            return store.values().stream().filter(a -> a.clientId().equals(clientId)).count();
        }
    }

    public static final class InMemoryTransactionRepository implements TransactionRepository {
        private final List<Transaction> store = new ArrayList<>();

        @Override
        public void save(Transaction transaction) {
            store.add(transaction);
        }

        @Override
        public List<Transaction> findByAccountId(String accountId, int offset, int limit) {
            return store.stream()
                    .filter(t -> t.accountId().equals(accountId))
                    .sorted(Comparator.comparing(Transaction::date).reversed())
                    .skip(offset).limit(limit).toList();
        }

        @Override
        public long countByAccountId(String accountId) {
            return store.stream().filter(t -> t.accountId().equals(accountId)).count();
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

        @Override
        public List<Loan> findAll(int offset, int limit) {
            return store.values().stream().skip(offset).limit(limit).toList();
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public List<Loan> findByClientId(String clientId, int offset, int limit) {
            return store.values().stream()
                    .filter(l -> l.clientId().equals(clientId))
                    .skip(offset).limit(limit).toList();
        }

        @Override
        public long countByClientId(String clientId) {
            return store.values().stream().filter(l -> l.clientId().equals(clientId)).count();
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

        @Override
        public Optional<User> findById(String userId) {
            return store.values().stream().filter(u -> u.id().equals(userId)).findFirst();
        }
    }

    public static final class FixedClock implements Clock {
        private final LocalDate today;
        private Instant cursor;

        public FixedClock(LocalDate today) {
            this.today = today;
            this.cursor = today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        }

        @Override
        public LocalDate today() {
            return today;
        }

        /** Instant monotone : chaque appel avance d'une seconde -> ordre stable des ecritures. */
        @Override
        public Instant now() {
            cursor = cursor.plusSeconds(1);
            return cursor;
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

    /** Garde de connexion controlable : ne bloque que si {@code blocked} est arme. */
    public static final class FakeLoginAttemptGuard implements LoginAttemptGuard {
        private boolean blocked = false;
        private int failures = 0;
        private int successes = 0;

        public void block() {
            this.blocked = true;
        }

        public int failures() {
            return failures;
        }

        public int successes() {
            return successes;
        }

        @Override
        public void assertNotBlocked(String username) {
            if (blocked) {
                throw new com.bank.domain.exception.TooManyLoginAttemptsException("blocked");
            }
        }

        @Override
        public void recordFailure(String username) {
            failures++;
        }

        @Override
        public void recordSuccess(String username) {
            successes++;
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
