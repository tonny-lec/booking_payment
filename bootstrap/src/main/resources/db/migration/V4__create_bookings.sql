-- =============================================================================
-- V4__create_bookings.sql
-- Booking bookings table
-- =============================================================================

CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    note VARCHAR(500) NULL,
    version INTEGER NOT NULL DEFAULT 1,
    cancelled_at TIMESTAMP NULL,
    cancel_reason VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_bookings_time_range CHECK (start_at < end_at),
    CONSTRAINT chk_bookings_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'))
);

CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_resource_time ON bookings(resource_id, start_at, end_at);
CREATE INDEX idx_bookings_status_start ON bookings(status, start_at);

COMMENT ON TABLE bookings IS 'Booking aggregate persistence table';
COMMENT ON COLUMN bookings.user_id IS 'Booking owner user identifier';
COMMENT ON COLUMN bookings.resource_id IS 'Reservable resource identifier';
COMMENT ON COLUMN bookings.start_at IS 'Booking start timestamp';
COMMENT ON COLUMN bookings.end_at IS 'Booking end timestamp';
COMMENT ON COLUMN bookings.status IS 'PENDING, CONFIRMED, CANCELLED';
COMMENT ON COLUMN bookings.note IS 'Optional booking note (max 500 chars)';
COMMENT ON COLUMN bookings.version IS 'Optimistic-lock version';
COMMENT ON COLUMN bookings.cancelled_at IS 'Cancellation timestamp';
COMMENT ON COLUMN bookings.cancel_reason IS 'Optional cancellation reason';
COMMENT ON COLUMN bookings.created_at IS 'Creation timestamp';
COMMENT ON COLUMN bookings.updated_at IS 'Last update timestamp';
