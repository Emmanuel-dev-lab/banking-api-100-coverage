package com.bank.web;

import com.bank.domain.exception.UnauthorizedException;

/** Extraction du jeton porteur depuis l'en-tete Authorization. */
public final class RequestAuth {

    private static final String PREFIX = "Bearer ";

    private RequestAuth() {
    }

    public static String bearer(String authorizationHeader) {
        if (authorizationHeader == null) {
            throw new UnauthorizedException("missing Authorization header");
        }
        if (!authorizationHeader.startsWith(PREFIX)) {
            throw new UnauthorizedException("invalid Authorization header");
        }
        return authorizationHeader.substring(PREFIX.length());
    }
}
