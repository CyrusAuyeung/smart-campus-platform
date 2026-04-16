package com.unikorn.campus.rule.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditScoreRuleTest {

    @Mock
    private RuleConfigRepository ruleConfigRepository;

    @Test
    void shouldRejectWhenCreditScoreTooLow() {
        when(ruleConfigRepository.findCreditScoreRuleConfig())
                .thenReturn(Optional.of(new CreditScoreRuleConfig(80, 90)));
        CreditScoreRule rule = new CreditScoreRule(ruleConfigRepository);

        RuleResult result = rule
                .evaluate(new RuleContext("UNDERGRADUATE", 70, "ACADEMIC_SPACE", Duration.ofHours(1), 0));

        assertThat(result.allowed()).isFalse();
        assertThat(result.message()).contains("暂不允许");
    }

    @Test
    void shouldWarnWhenCreditScoreInWarningRange() {
        when(ruleConfigRepository.findCreditScoreRuleConfig())
                .thenReturn(Optional.of(new CreditScoreRuleConfig(80, 90)));
        CreditScoreRule rule = new CreditScoreRule(ruleConfigRepository);

        RuleResult result = rule
                .evaluate(new RuleContext("UNDERGRADUATE", 85, "ACADEMIC_SPACE", Duration.ofHours(1), 0));

        assertThat(result.allowed()).isTrue();
        assertThat(result.message()).contains("受关注");
    }
}
