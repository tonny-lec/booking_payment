-- =============================================================================
-- V5__create_payments.sql
-- Payment payments table
-- (docs/design/usecases/payment-create.md section 7)
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
    idempotency_key UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_payments_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    CONSTRAINT chk_payments_captured_amount CHECK (captured_amount IS NULL OR captured_amount <= amount),
    CONSTRAINT chk_payments_refunded_amount CHECK (refunded_amount IS NULL OR refunded_amount <= captured_amount),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'AUTHORIZED', 'CAPTURED', 'REFUNDED', 'FAILED'))
);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status_created ON payments(status, created_at);
-- Note: uq_payments_idempotency_key implicitly creates the unique index used
-- for the idempotency replay lookup (no separate index needed).

COMMENT ON TABLE payments IS 'Payment aggregate persistence table';
COMMENT ON COLUMN payments.booking_id IS 'Booking the payment belongs to';
COMMENT ON COLUMN payments.user_id IS 'Payer user identifier (booking owner)';
COMMENT ON COLUMN payments.amount IS 'Authorized amount in currency minor units (> 0)';
COMMENT ON COLUMN payments.captured_amount IS 'Captured amount (<= amount)';
COMMENT ON COLUMN payments.refunded_amount IS 'Refunded amount (<= captured_amount)';
COMMENT ON COLUMN payments.currency IS 'ISO 4217 currency code';
COMMENT ON COLUMN payments.status IS 'PENDING, AUTHORIZED, CAPTURED, REFUNDED, FAILED';
COMMENT ON COLUMN payments.description IS 'Optional payment description (max 200 chars)';
COMMENT ON COLUMN payments.gateway_transaction_id IS 'External gateway transaction identifier';
COMMENT ON COLUMN payments.failure_reason IS 'Failure reason (FAILED status only)';
COMMENT ON COLUMN payments.idempotency_key IS 'Client-generated idempotency key (unique, replay guard)';
COMMENT ON COLUMN payments.created_at IS 'Creation timestamp (also idempotency-key TTL reference)';
COMMENT ON COLUMN payments.updated_at IS 'Last update timestamp';
