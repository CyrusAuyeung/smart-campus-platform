package com.unikorn.campus.event.domain;

import java.util.UUID;

public record EventReserveCommand(
        UUID orderId,
        String orderNo,
        UUID eventId,
        UUID userId,
        String requestId) {
}
