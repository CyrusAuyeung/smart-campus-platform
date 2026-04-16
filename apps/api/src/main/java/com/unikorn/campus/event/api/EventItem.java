package com.unikorn.campus.event.api;

import java.time.LocalDateTime;

public record EventItem(
        String id,
        String eventCode,
        String title,
        String status,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        int totalStock,
        int availableStock,
        int limitPerUser) {
}
