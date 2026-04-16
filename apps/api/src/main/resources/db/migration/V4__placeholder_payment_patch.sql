CREATE TABLE IF NOT EXISTS payment_record (
	id UUID PRIMARY KEY,
	order_id UUID NOT NULL REFERENCES booking_order(id),
	user_id UUID NOT NULL REFERENCES app_user(id),
	transaction_no VARCHAR(64) UNIQUE NOT NULL,
	status VARCHAR(32) NOT NULL,
	callback_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
