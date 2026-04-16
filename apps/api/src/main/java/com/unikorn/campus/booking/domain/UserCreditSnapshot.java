package com.unikorn.campus.booking.domain;

import java.util.UUID;

public record UserCreditSnapshot(
        UUID userId,
        int creditScore,
        int recentNoShowCount) {
}
