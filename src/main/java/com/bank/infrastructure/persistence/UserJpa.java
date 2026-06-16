package com.bank.infrastructure.persistence;

import com.bank.domain.model.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserJpa {

    @Id
    private String username;
    private String userId;
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    private Role role;
    private String clientId;

    protected UserJpa() {
    }

    public UserJpa(String username, String userId, String passwordHash, Role role, String clientId) {
        this.username = username;
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.role = role;
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public String getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getClientId() {
        return clientId;
    }
}
