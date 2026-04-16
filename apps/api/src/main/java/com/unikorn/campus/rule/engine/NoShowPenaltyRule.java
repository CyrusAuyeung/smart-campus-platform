package com.unikorn.campus.rule.engine;

import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class NoShowPenaltyRule implements BookingRule {

    private final RuleConfigRepository ruleConfigRepository;

    public NoShowPenaltyRule(RuleConfigRepository ruleConfigRepository) {
        this.ruleConfigRepository = ruleConfigRepository;
    }

    @Override
    public RuleResult evaluate(RuleContext context) {
        NoShowRuleConfig config = ruleConfigRepository.findNoShowRuleConfig()
                .orElse(NoShowRuleConfig.defaults());

        if (context.recentNoShowCount() <= 0) {
            return RuleResult.pass("no-show-penalty", "当前用户近期无爽约记录");
        }

        if (context.recentNoShowCount() >= config.banThreshold()) {
            return RuleResult.reject("no-show-penalty", "近期爽约次数过多，当前暂停预约资格");
        }

        if (context.recentNoShowCount() >= config.durationLimitThreshold()
                && context.requestedDuration().compareTo(Duration.ofMinutes(config.maxDurationMinutes())) > 0) {
            return RuleResult.reject("no-show-penalty", "近期爽约次数较多，单次预约时长不得超过 1 小时");
        }

        return RuleResult.pass("no-show-penalty", "历史爽约将影响后续信用分与额度");
    }
}
