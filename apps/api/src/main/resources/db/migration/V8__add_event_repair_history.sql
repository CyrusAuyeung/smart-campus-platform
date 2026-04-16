CREATE TABLE IF NOT EXISTS event_repair_action (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES campus_event(id),
    action_type VARCHAR(32) NOT NULL,
    previous_cache_stock INTEGER,
    database_stock INTEGER NOT NULL,
    operator VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
