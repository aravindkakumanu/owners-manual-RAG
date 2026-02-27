package com.rag.ownermanual.repository;

import com.rag.ownermanual.domain.IngestionJob;
import com.rag.ownermanual.domain.IngestionJobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of IngestionJobRepository for the ingestion_jobs table.
 */
@Component
public class JdbcIngestionJobRepository implements IngestionJobRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcIngestionJobRepository.class);

    private static final String TABLE = "ingestion_jobs";

    private final JdbcTemplate jdbcTemplate;

    public JdbcIngestionJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Persist a new ingestion job.
     * @param job the job to save (must have non-null id, manualId, status, createdAt, updatedAt)
     * @return the same job
     */
    @Override
    public IngestionJob save(IngestionJob job) {
        String sql = """
            INSERT INTO %s (id, manual_id, status, error_message, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """.formatted(TABLE);

        jdbcTemplate.update(
                sql,
                job.id(),
                job.manualId(),
                job.status().name(),
                job.errorMessage(),
                toTimestamp(job.createdAt()),
                toTimestamp(job.updatedAt())
        );
        log.debug("Saved ingestion job id={} manualId={} status={}", job.id(), job.manualId(), job.status());
        return job;
    }

    /**
     * Load a job by primary key.
     * @param id job UUID
     * @return the job if found, otherwise Optional.empty()
     */
    @Override
    public Optional<IngestionJob> findById(UUID id) {
        String sql = """
            SELECT id, manual_id, status, error_message, created_at, updated_at
            FROM %s WHERE id = ?
            """.formatted(TABLE);

        return jdbcTemplate.query(sql, JdbcIngestionJobRepository::mapRow, id)
                .stream()
                .findFirst();
    }

    /**
     * Update status and optional error message for an existing job. Also sets updated_at to the
     * current time so polling and audits reflect when the state changed.
     * @param id           job UUID (must exist; no-op if row not found)
     * @param status       new status
     * @param errorMessage set when status is FAILED; null otherwise (DB column is nullable)
     */
    @Override
    public void updateStatus(UUID id, IngestionJobStatus status, String errorMessage) {
        String sql = """
            UPDATE %s SET status = ?, error_message = ?, updated_at = ?
            WHERE id = ?
            """.formatted(TABLE);

        Instant now = Instant.now();
        int updated = jdbcTemplate.update(sql, status.name(), errorMessage, toTimestamp(now), id);
        if (updated == 0) {
            log.warn("updateStatus: no row updated for id={}", id);
        } else {
            log.debug("Updated job id={} to status={}", id, status);
        }
    }


    private static IngestionJob mapRow(ResultSet rs, int rowNum) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        String manualId = rs.getString("manual_id");
        IngestionJobStatus status = IngestionJobStatus.valueOf(rs.getString("status"));
        String errorMessage = rs.getString("error_message"); // nullable
        Instant createdAt = toInstant(rs.getTimestamp("created_at"));
        Instant updatedAt = toInstant(rs.getTimestamp("updated_at"));
        return new IngestionJob(id, manualId, status, errorMessage, createdAt, updatedAt);
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant toInstant(java.sql.Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
