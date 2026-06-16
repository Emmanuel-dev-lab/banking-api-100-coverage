package com.bank.infrastructure.security;

import com.bank.domain.exception.UnauthorizedException;
import com.bank.domain.model.Role;
import com.bank.domain.model.User;
import com.bank.domain.port.TokenClaims;
import com.bank.domain.port.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenService implements TokenService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtTokenService(@Value("${app.jwt.secret}") String secret,
                           @Value("${app.jwt.ttl-seconds}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public String issue(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.id())
                .claim("clientId", user.clientId())
                .claim("role", user.role().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlSeconds * 1000))
                .signWith(key)
                .compact();
    }

    @Override
    public TokenClaims verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new TokenClaims(
                    claims.getSubject(),
                    claims.get("clientId", String.class),
                    Role.valueOf(claims.get("role", String.class)));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("invalid token");
        }
    }
}
