-- 数据桥接数据库字段字典配置
-- 为 PostgreSQL, Oracle, SQL Server, H2 创建字典类型和字典数据

-- ============================================
-- 1. PostgreSQL 数据库字段配置
-- ============================================
-- 创建字典类型
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`,
                             `create_time`, `remark`)
VALUES (219, 'PostgreSQL数据库字段', 'databridge_postgresql_fields', '0', 'admin', NOW(),
        'PostgreSQL数据库连接配置字段');

-- 创建字典数据
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
                             `css_class`, `list_class`, `is_default`, `status`, `create_by`,
                             `create_time`, `update_by`, `update_time`, `remark`)
VALUES (814, 1, '主机地址', 'host', 'databridge_postgresql_fields', '', '', 'Y', '0', 'admin',
        NOW(), '', NULL,
        '{"type":"input","required":true,"placeholder":"请输入PostgreSQL主机地址"}'),
       (815, 2, '端口', 'port', 'databridge_postgresql_fields', '', '', 'Y', '0', 'admin', NOW(),
        '', NULL, '{"type":"number","required":true,"default":5432,"min":1,"max":65535}'),
       (816, 3, '用户名', 'username', 'databridge_postgresql_fields', '', '', 'Y', '0', 'admin',
        NOW(), '', NULL, '{"type":"input","required":true,"placeholder":"请输入数据库用户名"}'),
       (817, 4, '密码', 'password', 'databridge_postgresql_fields', '', '', 'Y', '0', 'admin',
        NOW(), '', NULL, '{"type":"password","required":true,"placeholder":"请输入数据库密码"}'),
       (818, 5, '数据库名', 'databaseName', 'databridge_postgresql_fields', '', '', 'Y', '0',
        'admin', NOW(), '', NULL,
        '{"type":"input","required":true,"placeholder":"请输入数据库名称"}'),
       (819, 6, 'Schema', 'schema', 'databridge_postgresql_fields', '', '', 'N', '0', 'admin',
        NOW(), '', NULL,
        '{"type":"input","required":false,"placeholder":"请输入Schema名称（默认public）","default":"public"}'),
       (820, 7, '连接超时', 'connectionTimeout', 'databridge_postgresql_fields', '', '', 'N', '0',
        'admin', NOW(), '', NULL,
        '{"type":"number","required":false,"default":30000,"min":1000,"max":300000}'),
       (821, 8, '最大连接数', 'maxConnections', 'databridge_postgresql_fields', '', '', 'N', '0',
        'admin', NOW(), '', NULL,
        '{"type":"number","required":false,"default":10,"min":1,"max":100}'),
       (822, 9, 'SSL模式', 'sslMode', 'databridge_postgresql_fields', '', '', 'N', '0', 'admin',
        NOW(), '', NULL,
        '{"type":"select","required":false,"options":["disable","require","verify-ca","verify-full"],"default":"disable"}'),
       (823, 10, '批量大小', 'batchSize', 'databridge_postgresql_fields', '', '', 'N', '0', 'admin',
        NOW(), '', NULL, '{"type":"number","required":false,"default":1000,"min":1,"max":10000}');

-- ============================================
-- 2. Oracle 数据库字段配置
-- ============================================
-- 创建字典类型
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`,
                             `create_time`, `remark`)
VALUES (220, 'Oracle数据库字段', 'databridge_oracle_fields', '0', 'admin', NOW(),
        'Oracle数据库连接配置字段');

-- 创建字典数据
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
                             `css_class`, `list_class`, `is_default`, `status`, `create_by`,
                             `create_time`, `update_by`, `update_time`, `remark`)
VALUES (824, 1, '主机地址', 'host', 'databridge_oracle_fields', '', '', 'Y', '0', 'admin', NOW(),
        '', NULL, '{"type":"input","required":true,"placeholder":"请输入Oracle主机地址"}'),
       (825, 2, '端口', 'port', 'databridge_oracle_fields', '', '', 'Y', '0', 'admin', NOW(), '',
        NULL, '{"type":"number","required":true,"default":1521,"min":1,"max":65535}'),
       (826, 3, '用户名', 'username', 'databridge_oracle_fields', '', '', 'Y', '0', 'admin', NOW(),
        '', NULL, '{"type":"input","required":true,"placeholder":"请输入数据库用户名"}'),
       (827, 4, '密码', 'password', 'databridge_oracle_fields', '', '', 'Y', '0', 'admin', NOW(),
        '', NULL, '{"type":"password","required":true,"placeholder":"请输入数据库密码"}'),
       (828, 5, 'SID/Service Name', 'databaseName', 'databridge_oracle_fields', '', '', 'Y', '0',
        'admin', NOW(), '', NULL,
        '{"type":"input","required":true,"placeholder":"请输入SID或Service Name"}'),
       (829, 6, '连接类型', 'connectionType', 'databridge_oracle_fields', '', '', 'N', '0', 'admin',
        NOW(), '', NULL,
        '{"type":"select","required":false,"options":["SID","ServiceName"],"default":"SID"}'),
       (830, 7, '连接超时', 'connectionTimeout', 'databridge_oracle_fields', '', '', 'N', '0',
        'admin', NOW(), '', NULL,
        '{"type":"number","required":false,"default":30000,"min":1000,"max":300000}'),
       (831, 8, '最大连接数', 'maxConnections', 'databridge_oracle_fields', '', '', 'N', '0',
        'admin', NOW(), '', NULL,
        '{"type":"number","required":false,"default":10,"min":1,"max":100}'),
       (832, 9, '字符集', 'charset', 'databridge_oracle_fields', '', '', 'N', '0', 'admin', NOW(),
        '', NULL,
        '{"type":"select","required":false,"options":["AL32UTF8","UTF8","ZHS16GBK"],"default":"AL32UTF8"}'),
       (833, 10, '批量大小', 'batchSize', 'databridge_oracle_fields', '', '', 'N', '0', 'admin',
        NOW(), '', NULL, '{"type":"number","required":false,"default":1000,"min":1,"max":10000}');

-- ============================================
-- 3. SQL Server 数据库字段配置
-- ============================================
-- 创建字典类型
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`,
                             `create_time`, `remark`)
VALUES (221, 'SQL Server数据库字段', 'databridge_sqlserver_fields', '0', 'admin', NOW(),
        'SQL Server数据库连接配置字段');

-- 创建字典数据
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
                             `css_class`, `list_class`, `is_default`, `status`, `create_by`,
                             `create_time`, `update_by`, `update_time`, `remark`)
VALUES (834, 1, '主机地址', 'host', 'databridge_sqlserver_fields', '', '', 'Y', '0', 'admin', NOW(),
        '', NULL, '{"type":"input","required":true,"placeholder":"请输入SQL Server主机地址"}'),
       (835, 2, '端口', 'port', 'databridge_sqlserver_fields', '', '', 'Y', '0', 'admin', NOW(), '',
        NULL, '{"type":"number","required":true,"default":1433,"min":1,"max":65535}'),
       (836, 3, '用户名', 'username', 'databridge_sqlserver_fields', '', '', 'Y', '0', 'admin',
        NOW(), '', NULL, '{"type":"input","required":true,"placeholder":"请输入数据库用户名"}'),
       (837, 4, '密码', 'password', 'databridge_sqlserver_fields', '', '', 'Y', '0', 'admin', NOW(),
        '', NULL, '{"type":"password","required":true,"placeholder":"请输入数据库密码"}'),
       (838, 5, '数据库名', 'databaseName', 'databridge_sqlserver_fields', '', '', 'Y', '0',
        'admin', NOW(), '', NULL,
        '{"type":"input","required":true,"placeholder":"请输入数据库名称"}'),
       (839, 6, '实例名', 'instanceName', 'databridge_sqlserver_fields', '', '', 'N', '0', 'admin',
        NOW(), '', NULL, '{"type":"input","required":false,"placeholder":"请输入实例名称（可选）"}'),
       (840, 7, '连接超时', 'connectionTimeout', 'databridge_sqlserver_fields', '', '', 'N', '0',
        'admin', NOW(), '', NULL,
        '{"type":"number","required":false,"default":30000,"min":1000,"max":300000}'),
       (841, 8, '最大连接数', 'maxConnections', 'databridge_sqlserver_fields', '', '', 'N', '0',
        'admin', NOW(), '', NULL,
        '{"type":"number","required":false,"default":10,"min":1,"max":100}'),
       (842, 9, '加密连接', 'encrypt', 'databridge_sqlserver_fields', '', '', 'N', '0', 'admin',
        NOW(), '', NULL,
        '{"type":"select","required":false,"options":["true","false"],"default":"false"}'),
       (843, 10, '信任服务器证书', 'trustServerCertificate', 'databridge_sqlserver_fields', '', '',
        'N', '0', 'admin', NOW(), '', NULL,
        '{"type":"select","required":false,"options":["true","false"],"default":"true"}'),
       (844, 11, '批量大小', 'batchSize', 'databridge_sqlserver_fields', '', '', 'N', '0', 'admin',
        NOW(), '', NULL, '{"type":"number","required":false,"default":1000,"min":1,"max":10000}');

-- ============================================
-- 4. H2 数据库字段配置
-- ============================================
-- 创建字典类型
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`,
                             `create_time`, `remark`)
VALUES (222, 'H2数据库字段', 'databridge_h2_fields', '0', 'admin', NOW(), 'H2数据库连接配置字段');

-- 创建字典数据
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
                             `css_class`, `list_class`, `is_default`, `status`, `create_by`,
                             `create_time`, `update_by`, `update_time`, `remark`)
VALUES (845, 1, '主机地址', 'host', 'databridge_h2_fields', '', '', 'Y', '0', 'admin', NOW(), '',
        NULL, '{"type":"input","required":true,"placeholder":"请输入H2主机地址（TCP模式）"}'),
       (846, 2, '端口', 'port', 'databridge_h2_fields', '', '', 'Y', '0', 'admin', NOW(), '', NULL,
        '{"type":"number","required":true,"default":9092,"min":1,"max":65535}'),
       (847, 3, '用户名', 'username', 'databridge_h2_fields', '', '', 'Y', '0', 'admin', NOW(), '',
        NULL,
        '{"type":"input","required":true,"placeholder":"请输入数据库用户名（默认sa）","default":"sa"}'),
       (848, 4, '密码', 'password', 'databridge_h2_fields', '', '', 'Y', '0', 'admin', NOW(), '',
        NULL, '{"type":"password","required":true,"placeholder":"请输入数据库密码"}'),
       (849, 5, '数据库名', 'databaseName', 'databridge_h2_fields', '', '', 'Y', '0', 'admin',
        NOW(), '', NULL,
        '{"type":"input","required":true,"placeholder":"请输入数据库名称或文件路径"}'),
       (850, 6, '数据库模式', 'dbMode', 'databridge_h2_fields', '', '', 'N', '0', 'admin', NOW(),
        '', NULL,
        '{"type":"select","required":false,"options":["tcp","file","mem"],"default":"tcp","placeholder":"tcp:TCP模式,file:文件模式,mem:内存模式"}'),
       (851, 7, '连接超时', 'connectionTimeout', 'databridge_h2_fields', '', '', 'N', '0', 'admin',
        NOW(), '', NULL,
        '{"type":"number","required":false,"default":30000,"min":1000,"max":300000}'),
       (852, 8, '最大连接数', 'maxConnections', 'databridge_h2_fields', '', '', 'N', '0', 'admin',
        NOW(), '', NULL, '{"type":"number","required":false,"default":10,"min":1,"max":100}'),
       (853, 9, '自动服务器', 'autoServer', 'databridge_h2_fields', '', '', 'N', '0', 'admin',
        NOW(), '', NULL,
        '{"type":"select","required":false,"options":["true","false"],"default":"true","placeholder":"是否启用自动服务器模式"}'),
       (854, 10, '批量大小', 'batchSize', 'databridge_h2_fields', '', '', 'N', '0', 'admin', NOW(),
        '', NULL, '{"type":"number","required":false,"default":1000,"min":1,"max":10000}');

