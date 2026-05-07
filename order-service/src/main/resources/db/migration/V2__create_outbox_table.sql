-- =============================================
-- V2__create_outbox_table.sql
-- Transactional Outbox for reliable event publishing
-- =============================================

CREATE TABLE outbox_events (
    id              UUID            PRIMARY KEY,
    aggregate_id    VARCHAR(100)    NOT NULL,
    aggregate_type  VARCHAR(100)    NOT NULL,
    event_type      VARCHAR(100)    NOT NULL,
    payload         JSONB           NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    retry_count     INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at    TIMESTAMP,
    error_message   TEXT
);

-- Index for the scheduler to pick up pending events quickly
CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at) WHERE status = 'PENDING';

COMMENT ON TABLE outbox_events IS 'Stores events to be published to Kafka to ensure atomicity with DB changes';
