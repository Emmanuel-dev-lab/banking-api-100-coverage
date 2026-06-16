package com.bank.domain.port;

/**
 * Protege la connexion contre le bruteforce. L'implementation decide de la
 * fenetre, du seuil et de la duree de blocage.
 */
public interface LoginAttemptGuard {

    /** Refuse (TooManyLoginAttemptsException) si l'identifiant est actuellement bloque. */
    void assertNotBlocked(String username);

    /** Comptabilise un echec d'authentification. */
    void recordFailure(String username);

    /** Remet le compteur a zero apres une connexion reussie. */
    void recordSuccess(String username);
}
