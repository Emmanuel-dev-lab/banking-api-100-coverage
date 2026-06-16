package com.bank.domain.port;

import com.bank.domain.model.User;

public interface TokenService {
    String issue(User user);

    /** Verifie le jeton et renvoie ses claims, ou leve UnauthorizedException. */
    TokenClaims verify(String token);
}
