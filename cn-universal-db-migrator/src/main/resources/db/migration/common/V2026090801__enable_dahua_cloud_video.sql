-- Enable the existing DAHUA CLOUD menu and create it for installations where the legacy hidden rows are absent.
UPDATE sys_menu
SET menu_name = '大华云',
    parent_id = (SELECT parent_lookup.menu_id
                 FROM (SELECT parent.menu_id
                       FROM sys_menu parent
                       WHERE parent.path = 'videoCenter' AND parent.menu_type = 'M'
                       ORDER BY parent.menu_id
                       LIMIT 1) parent_lookup),
    order_num = 2,
    path = 'dahua-cloud',
    component = 'DahuaCloud',
    menu_type = 'C',
    visible = '0',
    status = '0',
    icon = 'video-camera',
    update_by = 'migration',
    update_time = CURRENT_TIMESTAMP,
    remark = '大华云设备列表'
WHERE perms = 'video:dahua_cloud:list';

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(existing.menu_id), 0) + 1 FROM sys_menu existing),
       '大华云', parent.menu_id, 3, 'dahua-cloud', 'DahuaCloud', 1, 0,
       'C', '0', '0', 'video:dahua_cloud:list', 'video-camera', 'migration', CURRENT_TIMESTAMP,
       'migration', CURRENT_TIMESTAMP, '大华云设备列表'
FROM sys_menu parent
WHERE parent.path = 'videoCenter'
  AND parent.menu_type = 'M'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'video:dahua_cloud:list')
ORDER BY parent.menu_id
LIMIT 1;

UPDATE sys_menu
SET menu_name = '预览', order_num = 1, menu_type = 'F', visible = '0', status = '0',
    parent_id = (SELECT parent_lookup.menu_id
                 FROM (SELECT parent.menu_id FROM sys_menu parent
                       WHERE parent.perms = 'video:dahua_cloud:list'
                       ORDER BY parent.menu_id LIMIT 1) parent_lookup),
    update_by = 'migration', update_time = CURRENT_TIMESTAMP
WHERE perms = 'video:dahua_cloud:live';

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(existing.menu_id), 0) + 1 FROM sys_menu existing),
       '预览', parent.menu_id, 1, '', '', 1, 0,
       'F', '0', '0', 'video:dahua_cloud:live', '', 'migration', CURRENT_TIMESTAMP,
       'migration', CURRENT_TIMESTAMP, '大华实时预览权限'
FROM sys_menu parent
WHERE parent.perms = 'video:dahua_cloud:list'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'video:dahua_cloud:live')
ORDER BY parent.menu_id
LIMIT 1;

UPDATE sys_menu
SET menu_name = '回放', order_num = 2, menu_type = 'F', visible = '0', status = '0',
    parent_id = (SELECT parent_lookup.menu_id
                 FROM (SELECT parent.menu_id FROM sys_menu parent
                       WHERE parent.perms = 'video:dahua_cloud:list'
                       ORDER BY parent.menu_id LIMIT 1) parent_lookup),
    update_by = 'migration', update_time = CURRENT_TIMESTAMP
WHERE perms = 'video:dahua_cloud:playback';

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(existing.menu_id), 0) + 1 FROM sys_menu existing),
       '回放', parent.menu_id, 2, '', '', 1, 0,
       'F', '0', '0', 'video:dahua_cloud:playback', '', 'migration', CURRENT_TIMESTAMP,
       'migration', CURRENT_TIMESTAMP, '大华录像回放权限'
FROM sys_menu parent
WHERE parent.perms = 'video:dahua_cloud:list'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'video:dahua_cloud:playback')
ORDER BY parent.menu_id
LIMIT 1;

-- Match the role grants of the corresponding ImouLife menu and operation permissions.
INSERT INTO sys_role_menu (role_id, menu_id, uuid)
SELECT candidate.role_id,
       candidate.menu_id,
       ids.max_uuid + ROW_NUMBER() OVER (ORDER BY candidate.role_id, candidate.menu_id)
FROM (
  SELECT DISTINCT source_grant.role_id, target_menu.menu_id
  FROM sys_role_menu source_grant
  JOIN sys_menu source_menu ON source_menu.menu_id = source_grant.menu_id
  JOIN sys_menu target_menu
    ON target_menu.perms = CASE source_menu.perms
      WHEN 'video:imoulife:list' THEN 'video:dahua_cloud:list'
      WHEN 'video:imoulife:live' THEN 'video:dahua_cloud:live'
      WHEN 'video:imoulife:playback' THEN 'video:dahua_cloud:playback'
    END
  WHERE source_menu.perms IN ('video:imoulife:list', 'video:imoulife:live', 'video:imoulife:playback')
) candidate
CROSS JOIN (SELECT COALESCE(MAX(uuid), 0) AS max_uuid FROM sys_role_menu) ids
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_menu existing
  WHERE existing.role_id = candidate.role_id AND existing.menu_id = candidate.menu_id
);

-- Make the menu available in every tenant package that already includes the equivalent ImouLife capability.
INSERT INTO iot_tenant_package_menu (id, package_id, menu_id, create_time)
SELECT ids.max_id + ROW_NUMBER() OVER (ORDER BY candidate.package_id, candidate.menu_id),
       candidate.package_id,
       candidate.menu_id,
       CURRENT_TIMESTAMP
FROM (
  SELECT DISTINCT source_grant.package_id, target_menu.menu_id
  FROM iot_tenant_package_menu source_grant
  JOIN sys_menu source_menu ON source_menu.menu_id = source_grant.menu_id
  JOIN sys_menu target_menu
    ON target_menu.perms = CASE source_menu.perms
      WHEN 'video:imoulife:list' THEN 'video:dahua_cloud:list'
      WHEN 'video:imoulife:live' THEN 'video:dahua_cloud:live'
      WHEN 'video:imoulife:playback' THEN 'video:dahua_cloud:playback'
    END
  WHERE source_menu.perms IN ('video:imoulife:list', 'video:imoulife:live', 'video:imoulife:playback')
) candidate
CROSS JOIN (SELECT COALESCE(MAX(id), 0) AS max_id FROM iot_tenant_package_menu) ids
WHERE NOT EXISTS (
  SELECT 1 FROM iot_tenant_package_menu existing
  WHERE existing.package_id = candidate.package_id AND existing.menu_id = candidate.menu_id
);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT '大华云视频配置','product_configuration_dahua_cloud','0','migration',CURRENT_TIMESTAMP,'migration',CURRENT_TIMESTAMP,'大华云产品级凭据配置'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='product_configuration_dahua_cloud');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT 1, '大华云 ProductId', 'cloudProductId', 'product_configuration_dahua_cloud', 'N', '0', 'migration', CURRENT_TIMESTAMP, 'migration', CURRENT_TIMESTAMP, '{"remark": "开发者产品 ID，区别于 NexIoT 产品编号", "type": "string", "hide": "false", "default": ""}'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='product_configuration_dahua_cloud' AND dict_value='cloudProductId');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT 2, 'Access Key', 'accessKey', 'product_configuration_dahua_cloud', 'N', '0', 'migration', CURRENT_TIMESTAMP, 'migration', CURRENT_TIMESTAMP, '{"remark": "开发者产品 Access Key", "type": "string", "hide": "false", "default": ""}'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='product_configuration_dahua_cloud' AND dict_value='accessKey');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT 3, 'Secret Access Key', 'secretAccessKey', 'product_configuration_dahua_cloud', 'N', '0', 'migration', CURRENT_TIMESTAMP, 'migration', CURRENT_TIMESTAMP, '{"remark": "后端签名密钥，留空保留已保存值", "type": "string", "hide": "true", "default": ""}'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='product_configuration_dahua_cloud' AND dict_value='secretAccessKey');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT 4, '启用视频托管', 'videoHosting', 'product_configuration_dahua_cloud', 'N', '0', 'migration', CURRENT_TIMESTAMP, 'migration', CURRENT_TIMESTAMP, '{"remark": "是否支持云联分享设备导入", "type": "boolean", "hide": "false", "default": "true"}'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='product_configuration_dahua_cloud' AND dict_value='videoHosting');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT 35, '大华云', 'dahua_cloud', 'third_platform', 'N', '0', 'migration', CURRENT_TIMESTAMP, 'migration', CURRENT_TIMESTAMP, '大华云视频设备'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='third_platform' AND dict_value='dahua_cloud');
