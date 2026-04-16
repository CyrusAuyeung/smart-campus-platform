package com.unikorn.campus.booking.api;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record AcademicBookingRequest(
        @NotNull UUID userId,
        @NotNull UUID spaceId,
        @NotNull @Future LocalDateTime startAt,
        @NotNull @Future LocalDateTime endAt) {
}
