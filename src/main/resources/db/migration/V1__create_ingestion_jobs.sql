CREATE TABLE ingestion_jobs (
    id              UUID PRIMARY KEY,
    manual_id       VARCHAR(255) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    error_message   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('CREATED', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

COMMENT ON TABLE ingestion_jobs IS 'Async ingestion job state; status lifecycle CREATED -> PROCESSING -> COMPLETED|FAILED';
COMMENT ON COLUMN ingestion_jobs.manual_id IS 'Identifier of the manual being ingested';
COMMENT ON COLUMN ingestion_jobs.status IS 'CREATED, PROCESSING, COMPLETED, or FAILED';
COMMENT ON COLUMN ingestion_jobs.error_message IS 'Set when status is FAILED (or for logging)';
