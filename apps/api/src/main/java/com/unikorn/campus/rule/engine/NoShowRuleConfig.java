package com.unikorn.campus.rule.engine;

public record NoShowRuleConfig(
        int banThreshold,
        int durationLimitThreshold,
        int maxDurationMinutes) {

    public static NoShowRuleConfig defaults() {
        return new NoShowRuleConfig(3, 2, 60);
    }
}
