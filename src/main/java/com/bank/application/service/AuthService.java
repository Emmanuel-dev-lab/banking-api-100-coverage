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
}
