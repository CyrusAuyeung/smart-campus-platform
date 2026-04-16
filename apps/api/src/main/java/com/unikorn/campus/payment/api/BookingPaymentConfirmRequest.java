package com.unikorn.campus.payment.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BookingPaymentConfirmRequest(
        @NotNull UUID userId,
        @NotBlank String transactionNo) {
}
