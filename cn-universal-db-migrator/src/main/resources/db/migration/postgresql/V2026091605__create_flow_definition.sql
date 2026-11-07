-- 独立规则草稿存储，不读取或改写场景联动数据。
CREATE TABLE IF NOT EXISTS iot_flow_definition (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(2000),
    category VARCHAR(128),
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    revision BIGINT NOT NULL DEFAULT 0,
    graph_json TEXT NOT NULL,
    deleted INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_flow_definition_tenant ON iot_flow_definition (tenant_id, deleted, updated_at);
