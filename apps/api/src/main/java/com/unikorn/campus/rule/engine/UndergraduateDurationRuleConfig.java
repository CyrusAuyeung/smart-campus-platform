package com.unikorn.campus.rule.engine;

public record UndergraduateDurationRuleConfig(int maxDurationMinutes) {

    public static UndergraduateDurationRuleConfig defaults() {
        return new UndergraduateDurationRuleConfig(120);
    }
}