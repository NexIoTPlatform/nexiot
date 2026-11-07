-- Unified alarm lifecycle; all timestamps are UTC epoch milliseconds.
CREATE TABLE IF NOT EXISTS iot_alarm_type (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(128) NOT NULL,
 name VARCHAR(256) NOT NULL, status VARCHAR(32) NOT NULL,
 category VARCHAR(128) NOT NULL, owner_id VARCHAR(128) NOT NULL,
 reference_id VARCHAR(64) NOT NULL, correlation_key VARCHAR(64),
 source_type VARCHAR(32) NOT NULL, device_key VARCHAR(256) NOT NULL,
 severity VARCHAR(32) NOT NULL, signal_state VARCHAR(32) NOT NULL,
 revision BIGINT NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL,
 due_at BIGINT NOT NULL, body TEXT NOT NULL,
 CONSTRAINT uk_alarm_type_key UNIQUE (tenant_id, correlation_key)
);
CREATE INDEX idx_alarm_type_list ON iot_alarm_type (tenant_id, status, updated_at);
CREATE INDEX idx_alarm_type_ref ON iot_alarm_type (tenant_id, reference_id);
CREATE TABLE IF NOT EXISTS iot_alarm_policy (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(128) NOT NULL,
 name VARCHAR(256) NOT NULL, status VARCHAR(32) NOT NULL,
 category VARCHAR(128) NOT NULL, owner_id VARCHAR(128) NOT NULL,
 reference_id VARCHAR(64) NOT NULL, correlation_key VARCHAR(64),
 source_type VARCHAR(32) NOT NULL, device_key VARCHAR(256) NOT NULL,
 severity VARCHAR(32) NOT NULL, signal_state VARCHAR(32) NOT NULL,
 revision BIGINT NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL,
 due_at BIGINT NOT NULL, body TEXT NOT NULL,
 CONSTRAINT uk_alarm_policy_key UNIQUE (tenant_id, correlation_key)
);
CREATE INDEX idx_alarm_policy_list ON iot_alarm_policy (tenant_id, status, updated_at);
CREATE INDEX idx_alarm_policy_ref ON iot_alarm_policy (tenant_id, reference_id);
CREATE TABLE IF NOT EXISTS iot_alarm_record (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(128) NOT NULL,
 name VARCHAR(256) NOT NULL, status VARCHAR(32) NOT NULL,
 category VARCHAR(128) NOT NULL, owner_id VARCHAR(128) NOT NULL,
 reference_id VARCHAR(64) NOT NULL, correlation_key VARCHAR(64),
 source_type VARCHAR(32) NOT NULL, device_key VARCHAR(256) NOT NULL,
 severity VARCHAR(32) NOT NULL, signal_state VARCHAR(32) NOT NULL,
 revision BIGINT NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL,
 due_at BIGINT NOT NULL, body TEXT NOT NULL,
 CONSTRAINT uk_alarm_record_key UNIQUE (tenant_id, correlation_key)
);
CREATE INDEX idx_alarm_record_list ON iot_alarm_record (tenant_id, status, updated_at);
CREATE INDEX idx_alarm_record_ref ON iot_alarm_record (tenant_id, reference_id);
CREATE TABLE IF NOT EXISTS iot_alarm_work_order (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(128) NOT NULL,
 name VARCHAR(256) NOT NULL, status VARCHAR(32) NOT NULL,
 category VARCHAR(128) NOT NULL, owner_id VARCHAR(128) NOT NULL,
 reference_id VARCHAR(64) NOT NULL, correlation_key VARCHAR(64),
 source_type VARCHAR(32) NOT NULL, device_key VARCHAR(256) NOT NULL,
 severity VARCHAR(32) NOT NULL, signal_state VARCHAR(32) NOT NULL,
 revision BIGINT NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL,
 due_at BIGINT NOT NULL, body TEXT NOT NULL,
 CONSTRAINT uk_alarm_work_order_key UNIQUE (tenant_id, correlation_key)
);
CREATE INDEX idx_alarm_work_order_list ON iot_alarm_work_order (tenant_id, status, updated_at);
CREATE INDEX idx_alarm_work_order_ref ON iot_alarm_work_order (tenant_id, reference_id);
CREATE TABLE IF NOT EXISTS iot_alarm_timeline (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(128) NOT NULL,
 name VARCHAR(256) NOT NULL, status VARCHAR(32) NOT NULL,
 category VARCHAR(128) NOT NULL, owner_id VARCHAR(128) NOT NULL,
 reference_id VARCHAR(64) NOT NULL, correlation_key VARCHAR(64),
 source_type VARCHAR(32) NOT NULL, device_key VARCHAR(256) NOT NULL,
 severity VARCHAR(32) NOT NULL, signal_state VARCHAR(32) NOT NULL,
 revision BIGINT NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL,
 due_at BIGINT NOT NULL, body TEXT NOT NULL,
 CONSTRAINT uk_alarm_timeline_key UNIQUE (tenant_id, correlation_key)
);
CREATE INDEX idx_alarm_timeline_list ON iot_alarm_timeline (tenant_id, status, updated_at);
CREATE INDEX idx_alarm_timeline_ref ON iot_alarm_timeline (tenant_id, reference_id);
CREATE TABLE IF NOT EXISTS iot_alarm_evidence (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(128) NOT NULL,
 name VARCHAR(256) NOT NULL, status VARCHAR(32) NOT NULL,
 category VARCHAR(128) NOT NULL, owner_id VARCHAR(128) NOT NULL,
 reference_id VARCHAR(64) NOT NULL, correlation_key VARCHAR(64),
 source_type VARCHAR(32) NOT NULL, device_key VARCHAR(256) NOT NULL,
 severity VARCHAR(32) NOT NULL, signal_state VARCHAR(32) NOT NULL,
 revision BIGINT NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL,
 due_at BIGINT NOT NULL, body TEXT NOT NULL,
 CONSTRAINT uk_alarm_evidence_key UNIQUE (tenant_id, correlation_key)
);
CREATE INDEX idx_alarm_evidence_list ON iot_alarm_evidence (tenant_id, status, updated_at);
CREATE INDEX idx_alarm_evidence_ref ON iot_alarm_evidence (tenant_id, reference_id);
CREATE TABLE IF NOT EXISTS iot_alarm_inbox (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(128) NOT NULL,
 name VARCHAR(256) NOT NULL, status VARCHAR(32) NOT NULL,
 category VARCHAR(128) NOT NULL, owner_id VARCHAR(128) NOT NULL,
 reference_id VARCHAR(64) NOT NULL, correlation_key VARCHAR(64),
 source_type VARCHAR(32) NOT NULL, device_key VARCHAR(256) NOT NULL,
 severity VARCHAR(32) NOT NULL, signal_state VARCHAR(32) NOT NULL,
 revision BIGINT NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL,
 due_at BIGINT NOT NULL, body TEXT NOT NULL,
 CONSTRAINT uk_alarm_inbox_key UNIQUE (tenant_id, correlation_key)
);
CREATE INDEX idx_alarm_inbox_list ON iot_alarm_inbox (tenant_id, status, updated_at);
CREATE INDEX idx_alarm_inbox_ref ON iot_alarm_inbox (tenant_id, reference_id);
CREATE TABLE IF NOT EXISTS iot_alarm_task (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(128) NOT NULL,
 name VARCHAR(256) NOT NULL, status VARCHAR(32) NOT NULL,
 category VARCHAR(128) NOT NULL, owner_id VARCHAR(128) NOT NULL,
 reference_id VARCHAR(64) NOT NULL, correlation_key VARCHAR(64),
 source_type VARCHAR(32) NOT NULL, device_key VARCHAR(256) NOT NULL,
 severity VARCHAR(32) NOT NULL, signal_state VARCHAR(32) NOT NULL,
 revision BIGINT NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL,
 due_at BIGINT NOT NULL, body TEXT NOT NULL,
 CONSTRAINT uk_alarm_task_key UNIQUE (tenant_id, correlation_key)
);
CREATE INDEX idx_alarm_task_list ON iot_alarm_task (tenant_id, status, updated_at);
CREATE INDEX idx_alarm_task_ref ON iot_alarm_task (tenant_id, reference_id);
CREATE TABLE IF NOT EXISTS iot_alarm_condition_state (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(128) NOT NULL,
 name VARCHAR(256) NOT NULL, status VARCHAR(32) NOT NULL,
 category VARCHAR(128) NOT NULL, owner_id VARCHAR(128) NOT NULL,
 reference_id VARCHAR(64) NOT NULL, correlation_key VARCHAR(64),
 source_type VARCHAR(32) NOT NULL, device_key VARCHAR(256) NOT NULL,
 severity VARCHAR(32) NOT NULL, signal_state VARCHAR(32) NOT NULL,
 revision BIGINT NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL,
 due_at BIGINT NOT NULL, body TEXT NOT NULL,
 CONSTRAINT uk_alarm_condition_state_key UNIQUE (tenant_id, correlation_key)
);
CREATE INDEX idx_alarm_condition_state_list ON iot_alarm_condition_state (tenant_id, status, updated_at);
CREATE INDEX idx_alarm_condition_state_ref ON iot_alarm_condition_state (tenant_id, reference_id);
CREATE TABLE IF NOT EXISTS iot_alarm_key_lock (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(128) NOT NULL,
 name VARCHAR(256) NOT NULL, status VARCHAR(32) NOT NULL,
 category VARCHAR(128) NOT NULL, owner_id VARCHAR(128) NOT NULL,
 reference_id VARCHAR(64) NOT NULL, correlation_key VARCHAR(64),
 source_type VARCHAR(32) NOT NULL, device_key VARCHAR(256) NOT NULL,
 severity VARCHAR(32) NOT NULL, signal_state VARCHAR(32) NOT NULL,
 revision BIGINT NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL,
 due_at BIGINT NOT NULL, body TEXT NOT NULL,
 CONSTRAINT uk_alarm_key_lock_key UNIQUE (tenant_id, correlation_key)
);
CREATE INDEX idx_alarm_key_lock_list ON iot_alarm_key_lock (tenant_id, status, updated_at);
CREATE INDEX idx_alarm_key_lock_ref ON iot_alarm_key_lock (tenant_id, reference_id);
