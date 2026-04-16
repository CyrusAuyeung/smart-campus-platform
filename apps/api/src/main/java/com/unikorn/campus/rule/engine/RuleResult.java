package com.unikorn.campus.rule.engine;

public record RuleResult(
    boolean allowed,
    String ruleCode,
    String message
) {
    public static RuleResult pass(String ruleCode, String message) {
        return new RuleResult(true, ruleCode, message);
    }

    public static RuleResult reject(String ruleCode, String message) {
        return new RuleResult(false, ruleCode, message);
    }
}
