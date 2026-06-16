package com.bank.web.error;

import com.bank.domain.exception.BankException;
import com.bank.web.dto.ErrorResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Mapping des exceptions vers les reponses HTTP. Chaque handler est sans
 * branche : le statut provient directement de l'exception.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BankException.class)
    public ResponseEntity<ErrorResponse> handleBank(BankException ex) {
        return ResponseEntity.status(ex.httpStatus())
                .body(new ErrorResponse(ex.code(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICT", ex.getMessage()));
    }

    /** Echec de validation des DTO (@Valid). MethodArgumentNotValidException herite de BindException. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(BindException ex) {
        String message = ex.getAllErrors().stream().findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", message));
    }

    /** Corps de requete absent ou JSON malforme. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", "malformed or missing request body"));
    }

    /** Filet de securite : toute exception non prevue -> 500 sans fuite de detail interne. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "an unexpected error occurred"));
    }
}
