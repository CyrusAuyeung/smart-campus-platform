package com.unikorn.campus.rule.engine;

import java.util.Optional;

public interface RuleConfigRepository {

    Optional<NoShowRuleConfig> findNoShowRuleConfig();

    Optional<UndergraduateDurationRuleConfig> findUndergraduateDurationRuleConfig();

    Optional<CreditScoreRuleConfig> findCreditScoreRuleConfig();

    java.util.List<RuleConfigView> findAllRuleConfigs();
}
