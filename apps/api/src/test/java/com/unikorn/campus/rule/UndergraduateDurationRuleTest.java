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
class UndergraduateDurationRuleTest {

    @Mock
    private RuleConfigRepository ruleConfigRepository;

    @Test
    void shouldPassNonUndergraduateUsers() {
        UndergraduateDurationRule rule = new UndergraduateDurationRule(ruleConfigRepository);

        RuleResult result = rule
                .evaluate(new RuleContext("POSTGRADUATE", 100, "ACADEMIC_SPACE", Duration.ofHours(3), 0));

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void shouldRejectWhenConfiguredLimitExceeded() {
        when(ruleConfigRepository.findUndergraduateDurationRuleConfig())
                .thenReturn(Optional.of(new UndergraduateDurationRuleConfig(90)));
        UndergraduateDurationRule rule = new UndergraduateDurationRule(ruleConfigRepository);

        RuleResult result = rule
                .evaluate(new RuleContext("UNDERGRADUATE", 100, "ACADEMIC_SPACE", Duration.ofMinutes(100), 0));

        assertThat(result.allowed()).isFalse();
        assertThat(result.message()).contains("不能超过 2 小时");
    }
}