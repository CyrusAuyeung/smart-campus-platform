package com.unikorn.campus.booking.domain;

import java.util.UUID;

public record UserBookingProfile(
        UUID userId,
        String roleCode,
        int creditScore,
        int recentNoShowCount) {
}
