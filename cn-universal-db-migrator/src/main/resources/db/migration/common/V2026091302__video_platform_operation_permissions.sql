-- 为视频平台设备编辑、删除和告警订阅补齐独立操作权限。

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
       '编辑视频设备', parent.menu_id, 20, '', '', 1, 0,
       'F', '0', '0', 'video:platform:device:edit', '', 'migration', CURRENT_TIMESTAMP,
       'migration', CURRENT_TIMESTAMP, '编辑视频设备，前后端统一校验'
FROM sys_menu parent
WHERE parent.perms IN ('video:platform:view', 'video:platform:list')
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'video:platform:device:edit')
ORDER BY parent.menu_id LIMIT 1;

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
       '删除视频设备', parent.menu_id, 21, '', '', 1, 0,
       'F', '0', '0', 'video:platform:device:remove', '', 'migration', CURRENT_TIMESTAMP,
       'migration', CURRENT_TIMESTAMP, '删除视频设备，前后端统一校验'
FROM sys_menu parent
WHERE parent.perms IN ('video:platform:view', 'video:platform:list')
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'video:platform:device:remove')
ORDER BY parent.menu_id LIMIT 1;

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
       '管理告警订阅', parent.menu_id, 22, '', '', 1, 0,
       'F', '0', '0', 'video:platform:subscribe', '', 'migration', CURRENT_TIMESTAMP,
       'migration', CURRENT_TIMESTAMP, '管理告警订阅，前后端统一校验'
FROM sys_menu parent
WHERE parent.perms IN ('video:platform:view', 'video:platform:list')
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'video:platform:subscribe')
ORDER BY parent.menu_id LIMIT 1;

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
       '导入视频设备', parent.menu_id, 23, '', '', 1, 0,
       'F', '0', '0', 'video:platform:device:import', '', 'migration', CURRENT_TIMESTAMP,
       'migration', CURRENT_TIMESTAMP, '将视频设备导入 IoT，前后端统一校验'
FROM sys_menu parent
WHERE parent.perms IN ('video:platform:view', 'video:platform:list')
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'video:platform:device:import')
ORDER BY parent.menu_id LIMIT 1;

INSERT INTO sys_role_menu (role_id, menu_id, uuid)
SELECT candidate.role_id, candidate.menu_id,
       ids.max_id + ROW_NUMBER() OVER (ORDER BY candidate.role_id, candidate.menu_id)
FROM (
  SELECT DISTINCT source_grant.role_id, target_menu.menu_id
  FROM sys_role_menu source_grant
  JOIN sys_menu source_menu ON source_menu.menu_id = source_grant.menu_id
  JOIN sys_menu target_menu ON target_menu.perms = CASE source_menu.perms
      WHEN 'video:platform:edit' THEN 'video:platform:device:edit'
      WHEN 'video:platform:remove' THEN 'video:platform:device:remove'
      WHEN 'video:platform:test' THEN 'video:platform:subscribe'
      WHEN 'video:platform:sync' THEN 'video:platform:device:import'
    END
) candidate
CROSS JOIN (SELECT COALESCE(MAX(uuid), 0) AS max_id FROM sys_role_menu) ids
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_menu existing
  WHERE existing.role_id = candidate.role_id AND existing.menu_id = candidate.menu_id
);

INSERT INTO iot_tenant_package_menu (package_id, menu_id, id, create_time)
SELECT candidate.package_id, candidate.menu_id,
       ids.max_id + ROW_NUMBER() OVER (ORDER BY candidate.package_id, candidate.menu_id), CURRENT_TIMESTAMP
FROM (
  SELECT DISTINCT source_grant.package_id, target_menu.menu_id
  FROM iot_tenant_package_menu source_grant
  JOIN sys_menu source_menu ON source_menu.menu_id = source_grant.menu_id
  JOIN sys_menu target_menu ON target_menu.perms = CASE source_menu.perms
      WHEN 'video:platform:edit' THEN 'video:platform:device:edit'
      WHEN 'video:platform:remove' THEN 'video:platform:device:remove'
      WHEN 'video:platform:test' THEN 'video:platform:subscribe'
      WHEN 'video:platform:sync' THEN 'video:platform:device:import'
    END
) candidate
CROSS JOIN (SELECT COALESCE(MAX(id), 0) AS max_id FROM iot_tenant_package_menu) ids
WHERE NOT EXISTS (
  SELECT 1 FROM iot_tenant_package_menu existing
  WHERE existing.package_id = candidate.package_id AND existing.menu_id = candidate.menu_id
);
