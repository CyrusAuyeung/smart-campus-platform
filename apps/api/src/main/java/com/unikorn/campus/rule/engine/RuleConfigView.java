package com.unikorn.campus.rule.engine;

public record RuleConfigView(
        String ruleCode,
        boolean enabled,
        int priority,
        String configJson) {
}