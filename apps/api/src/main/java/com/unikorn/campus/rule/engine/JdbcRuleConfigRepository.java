package com.unikorn.campus.rule.engine;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRuleConfigRepository implements RuleConfigRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcRuleConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<NoShowRuleConfig> findNoShowRuleConfig() {
        return jdbcTemplate.query(
                """
                        SELECT
                          COALESCE((config_json ->> 'banThreshold')::int, 3) AS ban_threshold,
                          COALESCE((config_json ->> 'durationLimitThreshold')::int, 2) AS duration_limit_threshold,
                          COALESCE((config_json ->> 'maxDurationMinutes')::int, 60) AS max_duration_minutes
                        FROM rule_definition
                        WHERE rule_code = 'no-show-penalty'
                          AND enabled = TRUE
                        ORDER BY priority DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new NoShowRuleConfig(
                        rs.getInt("ban_threshold"),
                        rs.getInt("duration_limit_threshold"),
                        rs.getInt("max_duration_minutes")))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<UndergraduateDurationRuleConfig> findUndergraduateDurationRuleConfig() {
        return jdbcTemplate.query(
                """
                        SELECT
                          COALESCE((config_json ->> 'maxDurationMinutes')::int, 120) AS max_duration_minutes
                        FROM rule_definition
                        WHERE rule_code = 'undergraduate-duration'
                          AND enabled = TRUE
                        ORDER BY priority DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new UndergraduateDurationRuleConfig(rs.getInt("max_duration_minutes")))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<CreditScoreRuleConfig> findCreditScoreRuleConfig() {
        return jdbcTemplate.query(
                """
                        SELECT
                          COALESCE((config_json ->> 'restrictedThreshold')::int, 80) AS restricted_threshold,
                          COALESCE((config_json ->> 'warningThreshold')::int, 90) AS warning_threshold
                        FROM rule_definition
                        WHERE rule_code = 'credit-score'
                          AND enabled = TRUE
                        ORDER BY priority DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new CreditScoreRuleConfig(
                        rs.getInt("restricted_threshold"),
                        rs.getInt("warning_threshold")))
                .stream()
                .findFirst();
    }

    @Override
    public java.util.List<RuleConfigView> findAllRuleConfigs() {
        return jdbcTemplate.query(
                """
                        SELECT rule_code, enabled, priority, config_json::text AS config_json
                        FROM rule_definition
                        ORDER BY priority DESC, rule_code
                        """,
                (rs, rowNum) -> new RuleConfigView(
                        rs.getString("rule_code"),
                        rs.getBoolean("enabled"),
                        rs.getInt("priority"),
                        rs.getString("config_json")));
    }
}
