package com.unikorn.campus.event.api;

public record EventReconciliationSnapshot(
        String eventId,
        int databaseAvailableStock,
        int cacheAvailableStock,
        boolean consistent) {
}
