-- =============================================================================
-- V5__create_payments.sql
-- Payment aggregate persistence table
-- =============================================================================

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    user_id UUID NOT NULL,
    amount INTEGER NOT NULL,
    captured_amount INTEGER NULL,
    refunded_amount INTEGER NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description VARCHAR(200) NULL,
    gateway_transaction_id VARCHAR(255) NULL,
    failure_reason VARCHAR(500) NULL,
    idempotency_key UUID NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_payments_captured_amount CHECK (captured_amount IS NULL OR captured_amount <= amount),
    CONSTRAINT chk_payments_refunded_amount CHECK (
        refunded_amount IS NULL OR (captured_amount IS NOT NULL AND refunded_amount <= captured_amount)
    ),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'AUTHORIZED', 'CAPTURED', 'REFUNDED', 'FAILED'))
);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_idempotency_key ON payments(idempotency_key);
CREATE INDEX idx_payments_status_created ON payments(status, created_at);

COMMENT ON TABLE payments IS 'Payment aggregate persistence table';
COMMENT ON COLUMN payments.booking_id IS 'Booking identifier associated with payment';
COMMENT ON COLUMN payments.user_id IS 'Payment owner user identifier';
COMMENT ON COLUMN payments.amount IS 'Authorized amount in minor units';
COMMENT ON COLUMN payments.captured_amount IS 'Captured amount in minor units';
COMMENT ON COLUMN payments.refunded_amount IS 'Refunded amount in minor units';
COMMENT ON COLUMN payments.currency IS 'ISO 4217 currency code';
COMMENT ON COLUMN payments.status IS 'PENDING, AUTHORIZED, CAPTURED, REFUNDED, FAILED';
COMMENT ON COLUMN payments.description IS 'Optional payment description';
COMMENT ON COLUMN payments.gateway_transaction_id IS 'External payment gateway transaction identifier';
COMMENT ON COLUMN payments.failure_reason IS 'Authorization failure reason';
COMMENT ON COLUMN payments.idempotency_key IS 'Idempotency key used for create payment';
COMMENT ON COLUMN payments.created_at IS 'Creation timestamp';
COMMENT ON COLUMN payments.updated_at IS 'Last update timestamp';
