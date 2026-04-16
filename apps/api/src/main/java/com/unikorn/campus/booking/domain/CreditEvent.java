package com.unikorn.campus.booking.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreditEvent(
        UUID id,
        UUID userId,
        String eventType,
        int scoreDelta,
        String reason,
        LocalDateTime createdAt) {
}
