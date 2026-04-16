package com.unikorn.campus.event.api;

public record EventHealthSnapshot(
        long pendingOrders,
        long confirmedOrders,
        long failedOrders) {
}
