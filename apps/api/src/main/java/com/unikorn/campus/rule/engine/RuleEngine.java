package com.unikorn.campus.rule.engine;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuleEngine {

    private final List<BookingRule> bookingRules;

    public RuleEngine(List<BookingRule> bookingRules) {
        this.bookingRules = bookingRules;
    }

    public List<RuleResult> evaluateAll(RuleContext context) {
        return bookingRules.stream()
            .map(rule -> rule.evaluate(context))
            .toList();
    }
}
