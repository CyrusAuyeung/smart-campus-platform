package com.unikorn.campus.rule.engine;

import org.springframework.stereotype.Component;

@Component
public class CreditScoreRule implements BookingRule {

    private final RuleConfigRepository ruleConfigRepository;

    public CreditScoreRule(RuleConfigRepository ruleConfigRepository) {
        this.ruleConfigRepository = ruleConfigRepository;
    }

    @Override
    public RuleResult evaluate(RuleContext context) {
        CreditScoreRuleConfig config = ruleConfigRepository.findCreditScoreRuleConfig()
                .orElse(CreditScoreRuleConfig.defaults());

        if (context.creditScore() < config.restrictedThreshold()) {
            return RuleResult.reject("credit-score", "当前信用分过低，暂不允许发起预约");
        }

        if (context.creditScore() < config.warningThreshold()) {
            return RuleResult.pass("credit-score", "当前信用分偏低，预约能力受关注");
        }

        return RuleResult.pass("credit-score", "当前信用分满足预约要求");
    }
}
