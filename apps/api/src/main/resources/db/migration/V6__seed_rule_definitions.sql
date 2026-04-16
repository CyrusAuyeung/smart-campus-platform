INSERT INTO rule_definition (id, rule_code, rule_type, priority, enabled, config_json)
VALUES (
  '99999999-9999-9999-9999-999999999999',
  'no-show-penalty',
  'BOOKING',
  100,
  TRUE,
  '{"banThreshold": 3, "durationLimitThreshold": 2, "maxDurationMinutes": 60}'::jsonb
)
ON CONFLICT (rule_code) DO NOTHING;

INSERT INTO rule_definition (id, rule_code, rule_type, priority, enabled, config_json)
VALUES (
  '88888888-8888-8888-8888-888888888888',
  'undergraduate-duration',
  'BOOKING',
  90,
  TRUE,
  '{"maxDurationMinutes": 120}'::jsonb
)
ON CONFLICT (rule_code) DO NOTHING;

INSERT INTO rule_definition (id, rule_code, rule_type, priority, enabled, config_json)
VALUES (
  '77777777-7777-7777-7777-777777777777',
  'credit-score',
  'BOOKING',
  80,
  TRUE,
  '{"restrictedThreshold": 80, "warningThreshold": 90}'::jsonb
)
ON CONFLICT (rule_code) DO NOTHING;
