-- =============================================================================
-- V6__create_idempotency_records.sql
-- Idempotency request/result persistence table
-- =============================================================================

CREATE TABLE idempotency_records (
    idempotency_key UUID PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_idempotency_records_expires_at ON idempotency_records(expires_at);

COMMENT ON TABLE idempotency_records IS 'Idempotency key records for payment API';
COMMENT ON COLUMN idempotency_records.idempotency_key IS 'Client supplied idempotency key';
COMMENT ON COLUMN idempotency_records.request_hash IS 'Hash of canonical request content';
COMMENT ON COLUMN idempotency_records.response_status IS 'HTTP response status for stored response';
COMMENT ON COLUMN idempotency_records.response_body IS 'Stored response body payload';
COMMENT ON COLUMN idempotency_records.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN idempotency_records.expires_at IS 'Record expiration timestamp';
