CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    student_no VARCHAR(32) UNIQUE NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    credit_score INTEGER NOT NULL DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE resource_space (
    id UUID PRIMARY KEY,
    resource_code VARCHAR(64) UNIQUE NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    campus VARCHAR(64) NOT NULL,
    building VARCHAR(64),
    name VARCHAR(128) NOT NULL,
    capacity INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE academic_space (
    id UUID PRIMARY KEY REFERENCES resource_space(id),
    equipment_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    max_duration_minutes INTEGER NOT NULL,
    min_duration_minutes INTEGER NOT NULL
);

CREATE TABLE sport_facility (
    id UUID PRIMARY KEY REFERENCES resource_space(id),
    booking_mode VARCHAR(32) NOT NULL,
    slot_minutes INTEGER NOT NULL DEFAULT 60
);

CREATE TABLE sport_unit (
    id UUID PRIMARY KEY,
    facility_id UUID NOT NULL REFERENCES sport_facility(id),
    unit_code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    UNIQUE (facility_id, unit_code)
);

CREATE TABLE sport_unit_group (
    id UUID PRIMARY KEY,
    facility_id UUID NOT NULL REFERENCES sport_facility(id),
    group_code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    UNIQUE (facility_id, group_code)
);

CREATE TABLE sport_unit_group_member (
    group_id UUID NOT NULL REFERENCES sport_unit_group(id),
    unit_id UUID NOT NULL REFERENCES sport_unit(id),
    PRIMARY KEY (group_id, unit_id)
);

CREATE TABLE booking_order (
    id UUID PRIMARY KEY,
    order_no VARCHAR(64) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES app_user(id),
    business_type VARCHAR(32) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount_cent INTEGER NOT NULL DEFAULT 0,
    payment_deadline TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE academic_booking_occupancy (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE REFERENCES booking_order(id),
    space_id UUID NOT NULL REFERENCES academic_space(id),
    display_start_at TIMESTAMP NOT NULL,
    display_end_at TIMESTAMP NOT NULL,
    effective_range TSRANGE NOT NULL
);

ALTER TABLE academic_booking_occupancy
ADD CONSTRAINT academic_booking_no_overlap
EXCLUDE USING gist (
    space_id WITH =,
    effective_range WITH &&
);

CREATE TABLE sport_booking_occupancy (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES booking_order(id),
    facility_id UUID NOT NULL REFERENCES sport_facility(id),
    unit_id UUID NOT NULL REFERENCES sport_unit(id),
    booking_date DATE NOT NULL,
    slot_index INTEGER NOT NULL,
    UNIQUE (unit_id, booking_date, slot_index)
);

CREATE TABLE campus_event (
    id UUID PRIMARY KEY,
    event_code VARCHAR(64) UNIQUE NOT NULL,
    title VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    total_stock INTEGER NOT NULL,
    available_stock INTEGER NOT NULL,
    limit_per_user INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE event_order (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES campus_event(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    order_no VARCHAR(64) UNIQUE NOT NULL,
    status VARCHAR(32) NOT NULL,
    request_id VARCHAR(64) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rule_definition (
    id UUID PRIMARY KEY,
    rule_code VARCHAR(64) UNIQUE NOT NULL,
    rule_type VARCHAR(32) NOT NULL,
    priority INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_status_log (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES booking_order(id),
    previous_status VARCHAR(32),
    current_status VARCHAR(32) NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    trigger_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
