package com.unikorn.campus.rule.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoShowPenaltyRuleTest {

    @Mock
    private RuleConfigRepository ruleConfigRepository;

    @Test
    void shouldPassWhenNoRecentNoShow() {
        when(ruleConfigRepository.findNoShowRuleConfig())
                .thenReturn(java.util.Optional.of(new NoShowRuleConfig(3, 2, 60)));
        NoShowPenaltyRule rule = new NoShowPenaltyRule(ruleConfigRepository);

        RuleResult result = rule
                .evaluate(new RuleContext("UNDERGRADUATE", 100, "ACADEMIC_SPACE", Duration.ofHours(2), 0));

        assertThat(result.allowed()).isTrue();
        assertThat(result.message()).contains("无爽约记录");
    }

    @Test
    void shouldRejectWhenTooManyNoShows() {
        when(ruleConfigRepository.findNoShowRuleConfig())
                .thenReturn(java.util.Optional.of(new NoShowRuleConfig(3, 2, 60)));
        NoShowPenaltyRule rule = new NoShowPenaltyRule(ruleConfigRepository);

        RuleResult result = rule
                .evaluate(new RuleContext("UNDERGRADUATE", 100, "ACADEMIC_SPACE", Duration.ofMinutes(30), 3));

        assertThat(result.allowed()).isFalse();
        assertThat(result.message()).contains("暂停预约资格");
    }

    @Test
    void shouldRejectLongDurationWhenNoShowCountIsTwo() {
        when(ruleConfigRepository.findNoShowRuleConfig())
                .thenReturn(java.util.Optional.of(new NoShowRuleConfig(3, 2, 60)));
        NoShowPenaltyRule rule = new NoShowPenaltyRule(ruleConfigRepository);

        RuleResult result = rule
                .evaluate(new RuleContext("UNDERGRADUATE", 100, "ACADEMIC_SPACE", Duration.ofHours(2), 2));

        assertThat(result.allowed()).isFalse();
        assertThat(result.message()).contains("不得超过 1 小时");
    }

    @Test
    void shouldUseConfiguredThresholds() {
        when(ruleConfigRepository.findNoShowRuleConfig())
                .thenReturn(java.util.Optional.of(new NoShowRuleConfig(2, 1, 30)));
        NoShowPenaltyRule rule = new NoShowPenaltyRule(ruleConfigRepository);

        RuleResult result = rule
                .evaluate(new RuleContext("UNDERGRADUATE", 100, "ACADEMIC_SPACE", Duration.ofMinutes(40), 1));

        assertThat(result.allowed()).isFalse();
        assertThat(result.message()).contains("不得超过 1 小时");
    }
}
