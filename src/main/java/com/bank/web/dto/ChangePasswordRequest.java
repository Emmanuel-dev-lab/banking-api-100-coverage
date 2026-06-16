package com.bank.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "oldPassword is required") String oldPassword,
        @NotBlank(message = "newPassword is required") String newPassword) {
}
