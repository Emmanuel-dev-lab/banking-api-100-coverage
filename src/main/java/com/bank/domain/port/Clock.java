package com.bank.domain.port;

import java.time.Instant;
import java.time.LocalDate;

public interface Clock {
    LocalDate today();

    /** Instant de l'horloge, monotone : sert d'ordre chronologique des ecritures. */
    Instant now();
}
