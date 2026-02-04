-- =============================================================================
-- V2__create_users.sql
-- IAM users table
-- =============================================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    last_failed_login_at TIMESTAMP NULL,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);

COMMENT ON TABLE users IS 'IAM users';
COMMENT ON COLUMN users.email IS 'Unique email address (normalized to lowercase)';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password';
COMMENT ON COLUMN users.status IS 'ACTIVE, LOCKED, SUSPENDED';
COMMENT ON COLUMN users.failed_login_attempts IS 'Consecutive failed login attempts';
COMMENT ON COLUMN users.last_failed_login_at IS 'Timestamp of last failed login';
COMMENT ON COLUMN users.locked_until IS 'Lock expiration timestamp (null for indefinite)';
COMMENT ON COLUMN users.created_at IS 'Creation timestamp';
COMMENT ON COLUMN users.updated_at IS 'Last update timestamp';
