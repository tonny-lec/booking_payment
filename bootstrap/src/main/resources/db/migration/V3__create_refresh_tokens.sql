-- =============================================================================
-- V3__create_refresh_tokens.sql
-- IAM refresh tokens table
-- =============================================================================

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user_valid ON refresh_tokens(user_id, revoked_at) WHERE revoked_at IS NULL;

COMMENT ON TABLE refresh_tokens IS 'IAM refresh tokens';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hash of refresh token';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Token expiration timestamp';
COMMENT ON COLUMN refresh_tokens.revoked_at IS 'Revocation timestamp';
COMMENT ON COLUMN refresh_tokens.created_at IS 'Creation timestamp';
