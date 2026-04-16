package com.unikorn.campus.booking.domain;

import com.unikorn.campus.booking.api.AcademicBookingRequest;
import java.time.LocalDateTime;
import java.util.UUID;

public record AcademicBookingCommand(
        UUID orderId,
        String orderNo,
        UUID userId,
        UUID spaceId,
        LocalDateTime displayStartAt,
        LocalDateTime displayEndAt,
        LocalDateTime effectiveStartAt,
        LocalDateTime effectiveEndAt,
        LocalDateTime paymentDeadline) {
    public static AcademicBookingCommand of(
            UUID orderId,
            String orderNo,
            AcademicBookingRequest request,
            LocalDateTime effectiveStartAt,
            LocalDateTime effectiveEndAt,
            LocalDateTime paymentDeadline) {
        return new AcademicBookingCommand(
                orderId,
                orderNo,
                request.userId(),
                request.spaceId(),
                request.startAt(),
                request.endAt(),
                effectiveStartAt,
                effectiveEndAt,
                paymentDeadline);
    }
}
