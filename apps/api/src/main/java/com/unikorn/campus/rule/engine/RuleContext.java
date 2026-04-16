package com.unikorn.campus.rule.engine;

import java.time.Duration;

public record RuleContext(
    String userRole,
    int creditScore,
    String resourceType,
    Duration requestedDuration,
    int recentNoShowCount
) {
}
