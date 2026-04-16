package com.unikorn.campus.payment.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingTimeoutCommand(
        UUID orderId,
        LocalDateTime paymentDeadline) implements Serializable {
}
