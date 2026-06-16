package com.bank.application.service;

import com.bank.domain.exception.UnauthorizedException;
import com.bank.domain.model.User;
import com.bank.domain.port.LoginAttemptGuard;
import com.bank.domain.port.PasswordHasher;
import com.bank.domain.port.TokenClaims;
import com.bank.domain.port.TokenService;
import com.bank.domain.port.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final LoginAttemptGuard loginAttemptGuard;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher,
                       TokenService tokenService, LoginAttemptGuard loginAttemptGuard) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
        this.loginAttemptGuard = loginAttemptGuard;
    }

    public String login(String username, String rawPassword) {
        loginAttemptGuard.assertNotBlocked(username);
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordHasher.matches(rawPassword, user.passwordHash())) {
            loginAttemptGuard.recordFailure(username);
            throw new UnauthorizedException("invalid credentials");
        }
        loginAttemptGuard.recordSuccess(username);
        return tokenService.issue(user);
    }

    public TokenClaims authenticate(String token) {
        return tokenService.verify(token);
    }

    /**
     * Change le mot de passe de l'utilisateur identifie par {@code userId}.
     * Refuse (401) si l'utilisateur est introuvable ou si l'ancien mot de
     * passe ne correspond pas.
     */
    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !passwordHasher.matches(oldPassword, user.passwordHash())) {
            throw new UnauthorizedException("invalid credentials");
        }
        userRepository.save(new User(user.id(), user.username(),
                passwordHasher.hash(newPassword), user.role(), user.clientId()));
    }
}
