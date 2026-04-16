INSERT INTO app_user (id, student_no, display_name, role_code, credit_score)
VALUES
  ('11111111-1111-1111-1111-111111111111', '20240001', '林知远', 'UNDERGRADUATE', 100),
  ('22222222-2222-2222-2222-222222222222', '20240002', '沈语禾', 'POSTGRADUATE', 92),
  ('33333333-3333-3333-3333-333333333333', '20240003', '周听南', 'UNDERGRADUATE', 88),
  ('44444444-4444-4444-4444-444444444444', '20240004', '许照临', 'POSTGRADUATE', 95),
  ('55555555-5555-5555-5555-555555555555', '20240005', '顾行之', 'UNDERGRADUATE', 76)
ON CONFLICT (id) DO NOTHING;

INSERT INTO resource_space (id, resource_code, resource_type, campus, building, name, capacity, status)
VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'RM-A-601', 'ACADEMIC_SPACE', '主校区', '图文信息楼', '601 讨论室', 8, 'ACTIVE'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'GYM-BASKETBALL', 'SPORT_FACILITY', '主校区', '综合体育馆', '篮球训练场', 20, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO academic_space (id, equipment_json, max_duration_minutes, min_duration_minutes)
VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '{"projector": true, "whiteboard": true}'::jsonb, 240, 30)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sport_facility (id, booking_mode, slot_minutes)
VALUES
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'COMBINED', 60)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sport_unit (id, facility_id, unit_code, name, status)
VALUES
  ('cccccccc-cccc-cccc-cccc-ccccccccccc1', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'HALF-A', '篮球半场 A', 'ACTIVE'),
  ('cccccccc-cccc-cccc-cccc-ccccccccccc2', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'HALF-B', '篮球半场 B', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO sport_unit_group (id, facility_id, group_code, name)
VALUES
  ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'FULL', '整场篮球场')
ON CONFLICT (id) DO NOTHING;

INSERT INTO sport_unit_group_member (group_id, unit_id)
VALUES
  ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'cccccccc-cccc-cccc-cccc-ccccccccccc1'),
  ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'cccccccc-cccc-cccc-cccc-ccccccccccc2')
ON CONFLICT DO NOTHING;
