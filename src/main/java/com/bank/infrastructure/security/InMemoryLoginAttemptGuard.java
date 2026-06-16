package com.bank.infrastructure.security;

import com.bank.domain.exception.TooManyLoginAttemptsException;
import com.bank.domain.port.LoginAttemptGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limiteur de tentatives en memoire (par instance). Bloque un identifiant
 * apres {@code maxAttempts} echecs, pendant {@code blockSeconds}. Suffisant
 * pour un deploiement mono-instance ; pour du multi-instance, deporter l'etat
 * vers un store partage (Redis). Code d'infrastructure : hors couverture.
 */
@Component
public class InMemoryLoginAttemptGuard implements LoginAttemptGuard {

    private final int maxAttempts;
    private final long blockMillis;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public InMemoryLoginAttemptGuard(
            @Value("${app.login.max-attempts:5}") int maxAttempts,
            @Value("${app.login.block-seconds:900}") long blockSeconds) {
        this.maxAttempts = maxAttempts;
        this.blockMillis = blockSeconds * 1000L;
    }

    @Override
    public void assertNotBlocked(String username) {
        Attempt a = attempts.get(username);
        if (a != null && a.count >= maxAttempts
                && System.currentTimeMillis() - a.lastFailure < blockMillis) {
            throw new TooManyLoginAttemptsException("too many login attempts, retry later");
        }
    }

    @Override
    public void recordFailure(String username) {
        attempts.compute(username, (k, a) -> {
            long now = System.currentTimeMillis();
            if (a == null || now - a.lastFailure >= blockMillis) {
                return new Attempt(1, now);
            }
            return new Attempt(a.count + 1, now);
        });
    }

    @Override
    public void recordSuccess(String username) {
        attempts.remove(username);
    }

    private static final class Attempt {
        private final int count;
        private final long lastFailure;

        private Attempt(int count, long lastFailure) {
            this.count = count;
            this.lastFailure = lastFailure;
        }
    }
}
