package com.unikorn.campus.event.domain;

import java.io.Serializable;
import java.util.UUID;

public record EventReserveCommand(
        UUID orderId,
        String orderNo,
        UUID eventId,
        UUID userId,
        String requestId) implements Serializable {
}
