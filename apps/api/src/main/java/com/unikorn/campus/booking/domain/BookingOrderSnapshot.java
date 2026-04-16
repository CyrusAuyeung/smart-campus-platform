package com.unikorn.campus.booking.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingOrderSnapshot(
        UUID orderId,
        UUID userId,
        String orderNo,
        String status,
        String businessType,
        String resourceType,
        long version,
        LocalDateTime paymentDeadline) {
}
