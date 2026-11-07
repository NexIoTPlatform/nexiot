-- 更新资源连接表，添加数据流向字段
-- 使用现有的direction和extra_config字段，只添加data_direction字段

ALTER TABLE `iot_resource_connection`
    ADD COLUMN `data_direction` VARCHAR(20) DEFAULT 'OUTPUT' COMMENT '数据流向：INPUT-输入，OUTPUT-输出，BIDIRECTIONAL-双向';

-- 添加索引
ALTER TABLE `iot_resource_connection`
    ADD KEY `idx_data_direction` (`data_direction`);

-- 更新现有数据的data_direction字段
-- 根据direction字段的值来设置data_direction
UPDATE `iot_resource_connection`
SET `data_direction` = CASE
                           WHEN `direction` = 'IN' THEN 'INPUT'
                           WHEN `direction` = 'OUT' THEN 'OUTPUT'
                           WHEN `direction` = 'BOTH' THEN 'BIDIRECTIONAL'
                           ELSE 'OUTPUT'
    END;
