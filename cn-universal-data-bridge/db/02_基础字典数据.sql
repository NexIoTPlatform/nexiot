-- 基础字典数据
-- 资源类型
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
                             `css_class`,
                             `list_class`, `is_default`, `status`, `create_by`, `create_time`,
                             `update_by`,
                             `update_time`, `remark`)
VALUES (609, 1, 'MySQL数据库', 'MYSQL', 'databridge_resource_type', '', 'success', 'Y', '0',
        'admin', NOW(), '', NULL,
        '支持双向数据流转'),
       (610, 2, 'Kafka消息队列', 'KAFKA', 'databridge_resource_type', '', 'processing', 'Y', '0',
        'admin', NOW(), '',
        NULL, '支持双向数据流转'),
       (611, 3, 'MQTT消息代理', 'MQTT', 'databridge_resource_type', '', 'processing', 'Y', '0',
        'admin', NOW(), '',
        NULL, '支持双向数据流转'),
       (612, 4, 'HTTP接口', 'HTTP', 'databridge_resource_type', '', 'warning', 'Y', '0', 'admin',
        NOW(), '', NULL,
        '支持双向数据流转'),
       (613, 5, 'IoTDB时序数据库', 'IOTDB', 'databridge_resource_type', '', 'default', 'Y', '0',
        'admin', NOW(), '',
        NULL, '仅支持数据输出'),
       (614, 6, 'InfluxDB时序数据库', 'INFLUXDB', 'databridge_resource_type', '', 'default', 'Y',
        '0', 'admin', NOW(),
        '', NULL, '仅支持数据输出'),
       (615, 7, 'Elasticsearch搜索引擎', 'ELASTICSEARCH', 'databridge_resource_type', '', 'default',
        'Y', '0', 'admin',
        NOW(), '', NULL, '仅支持数据输出'),
       (750, 8, 'Redis缓存', 'REDIS', 'databridge_resource_type', '', 'danger', 'Y', '0', 'admin',
        NOW(), '', NULL,
        '支持双向数据流转');

-- 资源能力
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
                             `css_class`,
                             `list_class`, `is_default`, `status`, `create_by`, `create_time`,
                             `update_by`,
                             `update_time`, `remark`)
VALUES (616, 1, '双向流转', 'BIDIRECTIONAL', 'databridge_resource_capability', '', 'success', 'Y',
        '0', 'admin', NOW(),
        '', NULL, '支持输入和输出'),
       (617, 2, '仅输出', 'OUTPUT_ONLY', 'databridge_resource_capability', '', 'warning', 'Y', '0',
        'admin', NOW(), '',
        NULL, '仅支持数据输出');

-- 云平台类型
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
                             `css_class`,
                             `list_class`, `is_default`, `status`, `create_by`, `create_time`,
                             `update_by`,
                             `update_time`, `remark`)
VALUES (618, 1, '阿里云IoT平台', 'ALIYUN_IOT', 'databridge_cloud_platform', '', 'success', 'Y', '0',
        'admin', NOW(), '',
        NULL, '阿里云物联网平台'),
       (619, 2, '腾讯云IoT平台', 'TENCENT_IOT', 'databridge_cloud_platform', '', 'processing', 'Y',
        '0', 'admin', NOW(),
        '', NULL, '腾讯云物联网平台'),
       (620, 3, '华为云IoT平台', 'HUAWEI_IOT', 'databridge_cloud_platform', '', 'warning', 'Y', '0',
        'admin', NOW(), '',
        NULL, '华为云物联网平台');

-- 数据流向
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
                             `css_class`,
                             `list_class`, `is_default`, `status`, `create_by`, `create_time`,
                             `update_by`,
                             `update_time`, `remark`)
VALUES (634, 1, '数据输出', 'OUT', 'databridge_direction', '', 'success', 'Y', '0', 'admin', NOW(),
        '', NULL,
        'IoT平台数据输出到外部系统'),
       (635, 2, '数据输入', 'IN', 'databridge_direction', '', 'processing', 'Y', '0', 'admin',
        NOW(), '', NULL,
        '外部系统数据输入到IoT平台'),
       (636, 3, '双向流转', 'BOTH', 'databridge_direction', '', 'warning', 'Y', '0', 'admin', NOW(),
        '', NULL,
        '支持双向数据流转');

-- 桥接状态
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
                             `css_class`,
                             `list_class`, `is_default`, `status`, `create_by`, `create_time`,
                             `update_by`,
                             `update_time`, `remark`)
VALUES (720, 1, '已停用', '0', 'databridge_status', '', 'danger', 'Y', '0', 'admin', NOW(), '',
        NULL, '桥接配置已停用'),
       (721, 2, '已启用', '1', 'databridge_status', '', 'success', 'Y', '0', 'admin', NOW(), '',
        NULL,
        '桥接配置已启用'),
       (722, 3, '运行中', '2', 'databridge_status', '', 'processing', 'Y', '0', 'admin', NOW(), '',
        NULL,
        '桥接任务运行中'),
       (723, 4, '已暂停', '3', 'databridge_status', '', 'warning', 'Y', '0', 'admin', NOW(), '',
        NULL,
        '桥接任务已暂停'),
       (724, 5, '错误', '4', 'databridge_status', '', 'danger', 'Y', '0', 'admin', NOW(), '', NULL,
        '桥接任务出错');

-- 桥接类型
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
                             `css_class`,
                             `list_class`, `is_default`, `status`, `create_by`, `create_time`,
                             `update_by`,
                             `update_time`, `remark`)
VALUES (730, 1, '实时桥接', 'REALTIME', 'databridge_type', '', 'success', 'Y', '0', 'admin', NOW(),
        '', NULL,
        '实时数据桥接'),
       (731, 2, '定时桥接', 'SCHEDULED', 'databridge_type', '', 'processing', 'Y', '0', 'admin',
        NOW(), '', NULL,
        '定时数据桥接'),
       (732, 3, '批量桥接', 'BATCH', 'databridge_type', '', 'warning', 'Y', '0', 'admin', NOW(), '',
        NULL,
        '批量数据桥接'),
       (733, 4, '事件触发', 'EVENT_DRIVEN', 'databridge_type', '', 'default', 'Y', '0', 'admin',
        NOW(), '', NULL,
        '事件驱动桥接');

-- 消息类型
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
                             `css_class`,
                             `list_class`, `is_default`, `status`, `create_by`, `create_time`,
                             `update_by`,
                             `update_time`, `remark`)
VALUES (740, 1, '属性上报', 'PROPERTIES', 'databridge_message_type', '', 'success', 'Y', '0',
        'admin', NOW(), '', NULL,
        '设备属性数据上报'),
       (741, 2, '事件上报', 'EVENT', 'databridge_message_type', '', 'processing', 'Y', '0', 'admin',
        NOW(), '', NULL,
        '设备事件数据上报'),
       (742, 3, '功能调用', 'FUNCTIONS', 'databridge_message_type', '', 'warning', 'Y', '0',
        'admin', NOW(), '', NULL,
        '设备功能调用'),
       (743, 4, '设备上线', 'DEVICE_ONLINE', 'databridge_message_type', '', 'default', 'Y', '0',
        'admin', NOW(), '',
        NULL, '设备上线消息'),
       (744, 5, '设备离线', 'DEVICE_OFFLINE', 'databridge_message_type', '', 'default', 'Y', '0',
        'admin', NOW(), '',
        NULL, '设备离线消息');
