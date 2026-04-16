package com.unikorn.campus.event.api;

import java.time.LocalDateTime;

public record EventRepairAction(
        String eventId,
        String actionType,
        Integer previousCacheStock,
        int databaseStock,
        String operator,
        LocalDateTime createdAt) {
}
