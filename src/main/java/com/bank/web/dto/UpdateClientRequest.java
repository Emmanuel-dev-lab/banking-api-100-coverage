package com.bank.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateClientRequest(
        @NotBlank(message = "firstName is required") String firstName,
        @NotBlank(message = "lastName is required") String lastName) {
}
