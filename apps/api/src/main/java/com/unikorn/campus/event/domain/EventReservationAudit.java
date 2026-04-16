package com.unikorn.campus.event.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventReservationAudit(
        String requestId,
        UUID eventId,
        UUID userId,
        String status,
        String failureReason,
        LocalDateTime createdAt) {
}
