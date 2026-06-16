package com.bank.domain.model;

/**
 * Identite d'authentification. Un ADMIN a {@code clientId == null} ;
 * un CLIENT reference le client qu'il represente.
 */
public record User(String id, String username, String passwordHash, Role role, String clientId) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean owns(String ownerClientId) {
        return clientId != null && clientId.equals(ownerClientId);
    }
}
