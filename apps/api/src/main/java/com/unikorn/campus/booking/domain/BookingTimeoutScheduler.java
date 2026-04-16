package com.unikorn.campus.booking.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public interface BookingTimeoutScheduler {

    void scheduleTimeout(UUID orderId, LocalDateTime paymentDeadline);
}
