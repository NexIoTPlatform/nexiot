-- =============================================
-- NexIoT 微信小程序模块数据库表结构
-- 支持多小程序APPID隔离架构
-- =============================================

-- 1. 小程序应用表（扩展版，用于后台列表展示）
CREATE TABLE IF NOT EXISTS `mp_app`
(
    `id`
    BIGINT
    PRIMARY
    KEY
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `app_id`
    VARCHAR
(
    64
) NOT NULL UNIQUE COMMENT '小程序APPID',
    `app_secret` VARCHAR
(
    256
) NOT NULL COMMENT '小程序密钥（加密存储）',
    `name` VARCHAR
(
    64
) NOT NULL COMMENT '应用名称',
    `logo` VARCHAR
(
    512
) DEFAULT NULL COMMENT '应用Logo图片URL',
    `qrcode_image` VARCHAR
(
    512
) DEFAULT NULL COMMENT '小程序访问二维码图片URL',
    `access_url` VARCHAR
(
    512
) DEFAULT NULL COMMENT '小程序访问URL（二维码链接）',
    `description` VARCHAR
(
    512
) DEFAULT NULL COMMENT '应用描述',

    -- 数据源配置
    `data_source_type` TINYINT DEFAULT 1 COMMENT '数据源类型：1-关联已有应用 2-AK/SK自定义',
    `linked_app_id` VARCHAR
(
    64
) DEFAULT NULL COMMENT '关联的IoT应用ID（data_source_type=1时使用）',
    `ak` VARCHAR
(
    128
) DEFAULT NULL COMMENT 'Access Key（data_source_type=2时使用）',
    `sk` VARCHAR
(
    256
) DEFAULT NULL COMMENT 'Secret Key（data_source_type=2时使用，加密存储）',

    -- 小程序配置
    `primary_color` VARCHAR
(
    16
) DEFAULT '#1890ff' COMMENT '主题色',
    `welcome_text` VARCHAR
(
    128
) DEFAULT NULL COMMENT '欢迎语',
    `contact_phone` VARCHAR
(
    20
) DEFAULT NULL COMMENT '客服电话',
    `contact_email` VARCHAR
(
    64
) DEFAULT NULL COMMENT '客服邮箱',

    -- 后台列表展示冗余字段
    `user_count` INT DEFAULT 0 COMMENT '用户数量（冗余）',
    `device_count` INT DEFAULT 0 COMMENT '设备数量（冗余）',
    `online_device_count` INT DEFAULT 0 COMMENT '在线设备数（冗余）',

    -- 状态与时间
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `creator_id` VARCHAR
(
    64
) DEFAULT NULL COMMENT '创建者ID',
    `tenant_id` VARCHAR
(
    12
) DEFAULT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记：0-正常 1-已删除',
    INDEX `idx_status`
(
    `status`
),
    INDEX `idx_create_time`
(
    `create_time`
),
    INDEX `idx_mp_app_tenant`
(
    `tenant_id`,
    `status`,
    `deleted`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='小程序应用表';

-- 2. 小程序用户表
CREATE TABLE IF NOT EXISTS `mp_user`
(
    `id`
    BIGINT
    PRIMARY
    KEY
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `app_id`
    VARCHAR
(
    64
) NOT NULL COMMENT '所属小程序APPID',

    -- 账号密码登录
    `username` VARCHAR
(
    64
) DEFAULT NULL COMMENT '用户名（账号密码登录）',
    `password` VARCHAR
(
    256
) DEFAULT NULL COMMENT '密码（BCrypt加密）',

    -- 微信登录
    `open_id` VARCHAR
(
    128
) DEFAULT NULL COMMENT '微信OpenID',
    `union_id` VARCHAR
(
    128
) DEFAULT NULL COMMENT '微信UnionID',
    `phone` VARCHAR
(
    20
) DEFAULT NULL COMMENT '手机号',

    -- 用户信息
    `nickname` VARCHAR
(
    64
) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR
(
    512
) DEFAULT NULL COMMENT '头像URL',
    `real_name` VARCHAR
(
    32
) DEFAULT NULL COMMENT '真实姓名',
    `remark` VARCHAR
(
    256
) DEFAULT NULL COMMENT '备注（后台创建时填写）',

    -- 用户状态
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待绑定 1-已绑定 2-禁用',
    `bind_time` DATETIME DEFAULT NULL COMMENT '绑定时间',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR
(
    64
) DEFAULT NULL COMMENT '最后登录IP',

    -- 时间字段
    `creator_id` VARCHAR
(
    64
) DEFAULT NULL COMMENT '创建者ID（后台用户）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记：0-正常 1-已删除',
    UNIQUE KEY `uk_app_username`
(
    `app_id`,
    `username`
),
    INDEX `idx_app_openid`
(
    `app_id`,
    `open_id`
),
    INDEX `idx_app_phone`
(
    `app_id`,
    `phone`
),
    INDEX `idx_app_status`
(
    `app_id`,
    `status`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='小程序用户表';

-- 3. 用户权限表
CREATE TABLE IF NOT EXISTS `mp_user_permission`
(
    `id`
    BIGINT
    PRIMARY
    KEY
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `user_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '用户ID',
    `app_id`
    VARCHAR
(
    64
) NOT NULL COMMENT '小程序APPID',
    `permission_code` VARCHAR
(
    64
) NOT NULL COMMENT '权限码：DEVICE_ADD/DEVICE_DELETE/FUNCTION_CALL/DEVICE_UNBIND',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_user_perm`
(
    `user_id`,
    `app_id`,
    `permission_code`
),
    INDEX `idx_user_app`
(
    `user_id`,
    `app_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='小程序用户权限表';

-- 4. 设备绑定表（已废弃，设备通过 mp_app.linked_app_id ↔ iot_device.application 关联）
-- 注释掉以保留历史记录，新系统不再使用此表
-- CREATE TABLE IF NOT EXISTS `mp_device_binding` (
--     `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
--     `app_id` VARCHAR(64) NOT NULL COMMENT '小程序APPID',
--     `iot_id` VARCHAR(128) NOT NULL COMMENT 'IoT设备唯一标识',
--     ...
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小程序设备绑定表';

-- 4. 用户设备绑定表（用于按用户对设备进行授权，可选启用）
CREATE TABLE IF NOT EXISTS `mp_user_device`
(
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `app_id` VARCHAR(64) NOT NULL COMMENT '小程序APPID',
    `user_id` BIGINT NOT NULL COMMENT '小程序用户ID',
    `iot_id` VARCHAR(128) NOT NULL COMMENT 'IoT设备唯一标识',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_app_user_iot` (`app_id`, `user_id`, `iot_id`),
    KEY `idx_app_user` (`app_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='小程序用户设备绑定表';

-- 5. 绑定二维码表
CREATE TABLE IF NOT EXISTS `mp_bind_qrcode`
(
    `id`
    BIGINT
    PRIMARY
    KEY
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `code`
    VARCHAR
(
    64
) NOT NULL UNIQUE COMMENT '二维码唯一码',
    `app_id` VARCHAR
(
    64
) NOT NULL COMMENT '小程序APPID',
    `target_user_id` BIGINT DEFAULT NULL COMMENT '目标绑定用户ID',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待使用 1-已使用 2-已过期',
    `used_open_id` VARCHAR
(
    128
) DEFAULT NULL COMMENT '使用者OpenID',
    `used_time` DATETIME DEFAULT NULL COMMENT '使用时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_code`
(
    `code`
),
    INDEX `idx_app_status`
(
    `app_id`,
    `status`
),
    INDEX `idx_expire`
(
    `expire_time`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='小程序绑定二维码表';

-- 6. 设备详情装修配置表
CREATE TABLE IF NOT EXISTS `mp_detail_layout`
(
    `id`
    BIGINT
    PRIMARY
    KEY
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `app_id`
    VARCHAR
(
    64
) NOT NULL COMMENT '小程序APPID',
    `product_key` VARCHAR
(
    64
) NOT NULL COMMENT '产品Key',
    `layout_name` VARCHAR
(
    64
) DEFAULT NULL COMMENT '布局名称',
    `layout_config` JSON COMMENT '布局配置JSON',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `creator_id` VARCHAR
(
    64
) DEFAULT NULL COMMENT '创建者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_app_product`
(
    `app_id`,
    `product_key`
),
    INDEX `idx_app_id`
(
    `app_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='设备详情装修配置表';

-- 7. 组件库表
CREATE TABLE IF NOT EXISTS `mp_component_library`
(
    `id`
    BIGINT
    PRIMARY
    KEY
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `component_type`
    VARCHAR
(
    32
) NOT NULL UNIQUE COMMENT '组件类型：text/gauge/chart/switch/slider/image/video',
    `component_name` VARCHAR
(
    64
) NOT NULL COMMENT '组件名称',
    `component_icon` VARCHAR
(
    128
) DEFAULT NULL COMMENT '组件图标',
    `config_schema` JSON COMMENT '配置项schema定义',
    `preview_image` VARCHAR
(
    512
) DEFAULT NULL COMMENT '预览图URL',
    `description` VARCHAR
(
    256
) DEFAULT NULL COMMENT '组件描述',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_status`
(
    `status`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='装修组件库表';

-- 初始化组件库数据
INSERT INTO `mp_component_library` (`component_type`, `component_name`, `component_icon`,
                                    `config_schema`, `description`, `sort_order`)
VALUES ('text', '文本展示', 'icon-text',
        '{"properties":{"label":"属性标签","unit":"单位","format":"格式化"}}',
        '展示设备属性的文本值', 1),
       ('gauge', '仪表盘', 'icon-gauge',
        '{"properties":{"label":"标签","unit":"单位","min":"最小值","max":"最大值","thresholds":"阈值配置"}}',
        '以仪表盘形式展示数值', 2),
       ('chart', '趋势图表', 'icon-chart',
        '{"properties":{"label":"标签","unit":"单位","timeRange":"时间范围","chartType":"图表类型"}}',
        '展示属性历史趋势', 3),
       ('switch', '开关控制', 'icon-switch',
        '{"properties":{"label":"标签","functionId":"功能ID","onValue":"开启值","offValue":"关闭值"}}',
        '开关类型的功能控制', 4),
       ('slider', '滑块控制', 'icon-slider',
        '{"properties":{"label":"标签","functionId":"功能ID","min":"最小值","max":"最大值","step":"步进值"}}',
        '滑块类型的功能控制', 5),
       ('image', '图片展示', 'icon-image', '{"properties":{"label":"标签","aspectRatio":"宽高比"}}',
        '展示图片类型属性', 6),
       ('video', '视频播放', 'icon-video',
        '{"properties":{"label":"标签","playerType":"播放器类型"}}', '视频监控播放', 7);
