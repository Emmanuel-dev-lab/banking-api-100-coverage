package com.bank.web.dto;

/** Resultat d'un job declenche manuellement : nombre d'entites traitees. */
public record JobResultResponse(int processed) {
}
