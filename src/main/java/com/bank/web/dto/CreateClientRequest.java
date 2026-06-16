package com.bank.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateClientRequest(
        @NotBlank(message = "firstName is required") String firstName,
        @NotBlank(message = "lastName is required") String lastName,
        @NotBlank(message = "username is required") String username,
        @NotBlank(message = "password is required") String password) {
}
