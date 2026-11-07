-- ClickHouse 设备日志表结构
-- 使用说明：在 ClickHouse 中执行此 SQL 创建表

-- =================================================================
-- 表1: 设备主日志表 (iot_device_log)
-- =================================================================
CREATE TABLE IF NOT EXISTS iot_device_log
(
    `id`
    Int64
    COMMENT
    'ID，使用雪花算法生成',
    `iot_id`
    String
    COMMENT
    '设备唯一编码',
    `device_id`
    String
    COMMENT
    '设备自身序号',
    `product_key`
    String
    COMMENT
    '产品标识',
    `device_name`
    String
    COMMENT
    '设备名称',
    `message_type`
    String
    COMMENT
    '消息类型：PROPERTIES/EVENT/FUNCTIONS/REPLY',
    `command_id`
    Nullable
(
    String
) COMMENT '指令ID',
    `command_status` Nullable
(
    Int32
) COMMENT '指令状态',
    `event` Nullable
(
    String
) COMMENT '事件标识',
    `content` String COMMENT '消息内容（JSON格式）',
    `point` Nullable
(
    String
) COMMENT '地理位置坐标',
    `create_time` DateTime64
(
    3
) COMMENT '创建时间（毫秒精度）'
    )
    ENGINE = MergeTree
(
)
    PARTITION BY toYYYYMM
(
    create_time
) -- 按月分区
    ORDER BY
(
    iot_id,
    create_time
) -- 按 iot_id 和时间排序（提升查询性能）
    SETTINGS index_granularity = 8192;

-- 创建索引以提升查询性能
-- ClickHouse 会自动为 ORDER BY 字段创建索引，这里可以添加额外的跳表索引
ALTER TABLE iot_device_log
    ADD INDEX idx_message_type message_type TYPE set(0) GRANULARITY 4;
ALTER TABLE iot_device_log
    ADD INDEX idx_event event TYPE set(0) GRANULARITY 4;


-- =================================================================
-- 表2: 设备元数据日志表 (iot_device_log_metadata)
-- =================================================================
CREATE TABLE IF NOT EXISTS iot_device_log_metadata
(
    `id`
    Int64
    COMMENT
    'ID，使用雪花算法生成',
    `iot_id`
    String
    COMMENT
    '设备唯一编码',
    `product_key`
    String
    COMMENT
    '产品标识',
    `device_name`
    String
    COMMENT
    '设备名称',
    `device_id`
    String
    COMMENT
    '设备ID',
    `message_type`
    String
    COMMENT
    '消息类型：PROPERTIES/EVENT',
    `event`
    Nullable
(
    String
) COMMENT '事件标识',
    `property` Nullable
(
    String
) COMMENT '属性标识',
    `content` String COMMENT '内容数据',
    `ext1` Nullable
(
    String
) COMMENT '扩展字段1：属性名称或事件JSON数据',
    `ext2` Nullable
(
    String
) COMMENT '扩展字段2：属性格式化值',
    `ext3` Nullable
(
    String
) COMMENT '扩展字段3：属性单位符号',
    `create_time` DateTime64
(
    3
) COMMENT '创建时间（毫秒精度）'
    )
    ENGINE = MergeTree
(
)
    PARTITION BY toYYYYMM
(
    create_time
) -- 按月分区
    ORDER BY
(
    iot_id,
    create_time
) -- 按 iot_id 和时间排序
    SETTINGS index_granularity = 8192;

-- 创建索引以提升查询性能
ALTER TABLE iot_device_log_metadata
    ADD INDEX idx_property property TYPE set(0) GRANULARITY 4;
ALTER TABLE iot_device_log_metadata
    ADD INDEX idx_event event TYPE set(0) GRANULARITY 4;


-- =================================================================
-- 数据保留策略（可选）
-- =================================================================
-- ClickHouse 通过 TTL 实现自动清理过期数据
-- 示例：自动删除 30 天前的数据

-- 修改主日志表：添加 30 天 TTL
ALTER TABLE iot_device_log
    MODIFY TTL create_time + INTERVAL 30 DAY;

-- 修改元数据表：添加 90 天 TTL
ALTER TABLE iot_device_log_metadata
    MODIFY TTL create_time + INTERVAL 90 DAY;


-- =================================================================
-- 查询示例
-- =================================================================

-- 1. 查询指定设备的日志（分页）
SELECT *
FROM iot_device_log
WHERE iot_id = 'device_001'
  AND create_time >= now() - INTERVAL 7 DAY
ORDER BY create_time DESC
    LIMIT 10
OFFSET 0;

-- 2. 查询指定设备的事件统计
SELECT event, count(*) as qty, max(create_time) as last_time
FROM iot_device_log
WHERE iot_id = 'device_001'
  AND message_type = 'EVENT'
  AND event = 'alarm_event'
GROUP BY event;

-- 3. 查询属性元数据
SELECT *
FROM iot_device_log_metadata
WHERE iot_id = 'device_001'
  AND property = 'temperature'
ORDER BY create_time DESC LIMIT 10;


-- =================================================================
-- 性能优化建议
-- =================================================================

-- 1. 使用物化视图加速聚合查询（可选）
CREATE
MATERIALIZED VIEW IF NOT EXISTS iot_device_event_stats
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(create_time)
ORDER BY (iot_id, event, toDate(create_time))
AS
SELECT iot_id,
       event,
       toDate(create_time) as date,
    count() as event_count,
    max(create_time) as last_time
FROM iot_device_log
WHERE message_type = 'EVENT'
GROUP BY iot_id, event, toDate(create_time);

-- 2. 查看表信息
SELECT
    table, formatReadableSize(sum (bytes)) as size, sum (rows) as rows, max (modification_time) as latest_modification
FROM system.parts
WHERE database = currentDatabase()
  AND table IN ('iot_device_log'
    , 'iot_device_log_metadata')
  AND active
GROUP BY table;


-- =================================================================
-- 注意事项
-- =================================================================
/*
1. **分区策略**: 
   - 使用 toYYYYMM(create_time) 按月分区，适合时序数据
   - 可根据数据量调整为按天分区: toYYYYMMDD(create_time)

2. **排序键 (ORDER BY)**:
   - 必须包含高频查询字段
   - 推荐: (iot_id, create_time) 适合按设备查询

3. **索引策略**:
   - ClickHouse 自动为 ORDER BY 字段创建索引
   - 跳表索引 (skip index) 适合过滤低基数字段

4. **数据类型选择**:
   - String: 变长字符串，适合设备ID、产品Key
   - DateTime64(3): 毫秒精度时间戳
   - Nullable: 可空字段

5. **TTL 自动清理**:
   - 通过 TTL 实现数据自动清理，无需应用层定时任务
   - 可针对不同表设置不同的保留周期

6. **性能优化**:
   - 物化视图：预聚合统计数据，加速查询
   - 合理使用分区：避免全表扫描
   - 控制分区数量：建议不超过 1000 个活动分区
*/
