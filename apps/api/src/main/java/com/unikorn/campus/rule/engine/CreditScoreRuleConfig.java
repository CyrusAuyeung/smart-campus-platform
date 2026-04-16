package com.unikorn.campus.rule.engine;

public record CreditScoreRuleConfig(
        int restrictedThreshold,
        int warningThreshold) {

    public static CreditScoreRuleConfig defaults() {
        return new CreditScoreRuleConfig(80, 90);
    }
}
