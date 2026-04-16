package com.unikorn.campus.payment.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewCase(
        String transactionNo,
        UUID orderId,
        UUID userId,
        String status,
        String callbackPayload,
        LocalDateTime createdAt) {
}