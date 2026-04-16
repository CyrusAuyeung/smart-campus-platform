package com.unikorn.campus.event.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EventReserveRequest(
        @NotNull UUID userId,
        @NotNull UUID eventId) {
}
