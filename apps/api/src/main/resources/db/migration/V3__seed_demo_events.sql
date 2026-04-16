INSERT INTO campus_event (id, event_code, title, status, starts_at, ends_at, total_stock, available_stock, limit_per_user)
VALUES
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'LECTURE-AI-2026', '名人讲座：AI 与校园创新', 'PUBLISHED', CURRENT_TIMESTAMP + INTERVAL '2 days', CURRENT_TIMESTAMP + INTERVAL '2 days 2 hours', 120, 120, 1),
  ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'SINGER-FINAL-2026', '十佳歌手决赛抢票', 'PUBLISHED', CURRENT_TIMESTAMP + INTERVAL '3 days', CURRENT_TIMESTAMP + INTERVAL '3 days 3 hours', 80, 80, 1)
ON CONFLICT (id) DO NOTHING;
