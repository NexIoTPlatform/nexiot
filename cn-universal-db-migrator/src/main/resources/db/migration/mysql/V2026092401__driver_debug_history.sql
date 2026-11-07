CREATE TABLE IF NOT EXISTS iot_protocol_revision (
    id VARCHAR(36) PRIMARY KEY,
    protocol_id VARCHAR(64) NOT NULL,
    source MEDIUMTEXT NOT NULL,
    source_hash VARCHAR(64) NOT NULL,
    version VARCHAR(128),
    saved_by VARCHAR(128),
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);
CREATE INDEX idx_protocol_revision_history ON iot_protocol_revision (protocol_id, created_at);
