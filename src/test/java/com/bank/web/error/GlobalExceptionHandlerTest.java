package com.bank.web.error;

import com.bank.domain.exception.AccountNotActiveException;
import com.bank.domain.exception.AccountNotFoundException;
import com.bank.domain.exception.BankException;
import com.bank.domain.exception.ClientNotFoundException;
import com.bank.domain.exception.ForbiddenException;
import com.bank.domain.exception.InsufficientFundsException;
import com.bank.domain.exception.InvalidAmountException;
import com.bank.domain.exception.InvalidLoanTermsException;
import com.bank.domain.exception.LoanAlreadyClosedException;
import com.bank.domain.exception.LoanNotFoundException;
import com.bank.domain.exception.SameAccountTransferException;
import com.bank.domain.exception.TooManyLoginAttemptsException;
import com.bank.domain.exception.UnauthorizedException;
import com.bank.web.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    static Stream<BankException> bankExceptions() {
        return Stream.of(
                new InvalidAmountException(0),               // H1 400
                new AccountNotFoundException("a"),           // H2 404
                new InsufficientFundsException("a"),         // H3 422
                new AccountNotActiveException("a"),          // H4 409
                new SameAccountTransferException("a"),       // H5 422
                new UnauthorizedException("x"),              // H6 401
                new ForbiddenException("x"),                 // H7 403
                new InvalidLoanTermsException("x"),          // H8 400
                new LoanAlreadyClosedException("l"),         // H9 409
                new ClientNotFoundException("c"),            // H10 404
                new LoanNotFoundException("l"),              // H10 404
                new TooManyLoginAttemptsException("t")       // H11 429
        );
    }

    // H1..H10 : chaque exception passe par handleBank -> code()/httpStatus() executes
    @ParameterizedTest
    @MethodSource("bankExceptions")
    void handleBank_mapsStatusAndCode(BankException ex) {
        ResponseEntity<ErrorResponse> response = handler.handleBank(ex);
        assertEquals(ex.httpStatus(), response.getStatusCode().value());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ex.code());
    }

    @Test
    void handleIllegalArgument_400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException("bad"));
        assertEquals(400, response.getStatusCode().value());
        assertThat(response.getBody().code()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void handleIllegalState_409() {
        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalState(new IllegalStateException("conflict"));
        assertEquals(409, response.getStatusCode().value());
        assertThat(response.getBody().code()).isEqualTo("CONFLICT");
    }

    @Test
    void handleValidation_400_usesFirstFieldError() {
        BindException ex = new BindException(new Object(), "request");
        ex.addError(new FieldError("request", "amount", "amount must be > 0"));
        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);
        assertEquals(400, response.getStatusCode().value());
        assertThat(response.getBody().code()).isEqualTo("BAD_REQUEST");
        assertThat(response.getBody().message()).isEqualTo("amount must be > 0");
    }

    @Test
    void handleValidation_400_noFieldError_fallbackMessage() {
        BindException ex = new BindException(new BeanPropertyBindingResult(new Object(), "request"));
        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);
        assertEquals(400, response.getStatusCode().value());
        assertThat(response.getBody().message()).isEqualTo("validation failed");
    }

    @Test
    void handleUnreadable_400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnreadable(new HttpMessageNotReadableException("nope", (Throwable) null, null));
        assertEquals(400, response.getStatusCode().value());
        assertThat(response.getBody().code()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void handleUnexpected_500_noLeak() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new RuntimeException("secret stacktrace detail"));
        assertEquals(500, response.getStatusCode().value());
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).doesNotContain("secret");
    }
}
