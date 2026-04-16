package com.unikorn.campus.rule.engine;

import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class UndergraduateDurationRule implements BookingRule {

    private final RuleConfigRepository ruleConfigRepository;

    public UndergraduateDurationRule(RuleConfigRepository ruleConfigRepository) {
        this.ruleConfigRepository = ruleConfigRepository;
    }

    @Override
    public RuleResult evaluate(RuleContext context) {
        if (!"UNDERGRADUATE".equalsIgnoreCase(context.userRole())) {
            return RuleResult.pass("undergraduate-duration", "当前用户不受本科生时长规则限制");
        }

        UndergraduateDurationRuleConfig config = ruleConfigRepository.findUndergraduateDurationRuleConfig()
                .orElse(UndergraduateDurationRuleConfig.defaults());

        if (context.requestedDuration().compareTo(Duration.ofMinutes(config.maxDurationMinutes())) > 0) {
            return RuleResult.reject("undergraduate-duration", "本科生单次预约时长不能超过 2 小时");
        }

        return RuleResult.pass("undergraduate-duration", "本科生预约时长符合规则");
    }
}
