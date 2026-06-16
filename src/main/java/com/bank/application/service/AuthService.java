package com.bank.application.service;

import com.bank.domain.exception.UnauthorizedException;
import com.bank.domain.model.User;
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

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    public String login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("invalid credentials"));
        if (!passwordHasher.matches(rawPassword, user.passwordHash())) {
            throw new UnauthorizedException("invalid credentials");
        }
        return tokenService.issue(user);
    }

    public TokenClaims authenticate(String token) {
        return tokenService.verify(token);
    }
}
