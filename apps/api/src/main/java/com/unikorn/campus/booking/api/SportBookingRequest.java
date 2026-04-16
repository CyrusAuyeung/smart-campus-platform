package com.unikorn.campus.booking.api;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SportBookingRequest(
        @NotNull UUID userId,
        @NotNull UUID facilityId,
        @NotEmpty List<UUID> unitIds,
        @NotNull @FutureOrPresent LocalDate bookingDate,
        @NotEmpty List<Integer> slotIndices) {
}
