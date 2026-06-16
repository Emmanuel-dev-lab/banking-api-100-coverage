package com.bank.domain.port;

import com.bank.domain.model.Role;

/** Donnees extraites d'un jeton verifie. */
public record TokenClaims(String userId, String clientId, Role role) {
}
