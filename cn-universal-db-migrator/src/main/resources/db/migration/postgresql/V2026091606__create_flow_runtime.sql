-- 草稿、不可变发布快照与运行租约分开，禁止读取旧场景联动数据。
CREATE TABLE iot_flow_version (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, definition_id VARCHAR(64) NOT NULL,
 version_no BIGINT NOT NULL, actor_id VARCHAR(64) NOT NULL, graph_json TEXT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE (tenant_id, definition_id, version_no)
);
CREATE TABLE iot_flow_deployment (
 tenant_id VARCHAR(64) NOT NULL, definition_id VARCHAR(64) NOT NULL,
 version_id VARCHAR(64) NOT NULL, active_version_id VARCHAR(64), enabled INTEGER NOT NULL DEFAULT 1,
 owner_id VARCHAR(64), lease_until TIMESTAMP, state VARCHAR(16) NOT NULL DEFAULT 'PUBLISHING', error VARCHAR(2000),
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY (tenant_id, definition_id)
);
CREATE INDEX idx_flow_deployment_enabled ON iot_flow_deployment (enabled, lease_until);
CREATE TABLE iot_flow_execution (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, definition_id VARCHAR(64) NOT NULL,
 version_id VARCHAR(64), source VARCHAR(512), status VARCHAR(16) NOT NULL,
 elapsed_ms BIGINT NOT NULL, trace_json TEXT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_flow_execution_tenant ON iot_flow_execution (tenant_id, definition_id, created_at);
