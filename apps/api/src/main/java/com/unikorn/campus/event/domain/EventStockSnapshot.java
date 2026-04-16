package com.unikorn.campus.event.domain;

import java.util.UUID;

public record EventStockSnapshot(
        UUID eventId,
        String title,
        int availableStock,
        int limitPerUser,
        String status) {
}
