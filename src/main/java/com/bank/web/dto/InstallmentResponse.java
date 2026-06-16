package com.bank.web.dto;

import com.bank.domain.model.Installment;

public record InstallmentResponse(
        int index, String dueDate, long amount, long principalPart, long interestPart, boolean paid) {
    public static InstallmentResponse from(Installment installment) {
        return new InstallmentResponse(
                installment.index(),
                installment.dueDate().toString(),
                installment.amount().amount(),
                installment.principalPart().amount(),
                installment.interestPart().amount(),
                installment.paid());
    }
}
