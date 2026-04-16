package com.unikorn.campus.event.api;

public record EventReserveReceipt(
        String requestId,
        String status,
        String message,
        int remainingStock) {
}
