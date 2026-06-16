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

    /** Taille minimale du secret pour HS256 (256 bits = 32 octets). */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long ttlSeconds;
    private final String issuer;

    public JwtTokenService(@Value("${app.jwt.secret}") String secret,
                           @Value("${app.jwt.ttl-seconds}") long ttlSeconds,
                           @Value("${app.jwt.issuer}") String issuer) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret doit faire au moins " + MIN_SECRET_BYTES
                            + " octets (256 bits) pour HS256; recu: " + bytes.length);
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.ttlSeconds = ttlSeconds;
        this.issuer = issuer;
    }

    @Override
    public String issue(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(issuer)
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
                    .requireIssuer(issuer)
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
