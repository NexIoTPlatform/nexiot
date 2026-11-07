-- Enable the existing EZVIZ menu and create it for installations where the legacy hidden rows are absent.
UPDATE sys_menu
SET menu_name = '萤石云',
    parent_id = (SELECT parent_lookup.menu_id
                 FROM (SELECT parent.menu_id
                       FROM sys_menu parent
                       WHERE parent.path = 'videoCenter' AND parent.menu_type = 'M'
                       ORDER BY parent.menu_id
                       LIMIT 1) parent_lookup),
    order_num = 2,
    path = 'ezviz',
    component = 'VideoEzviz',
    menu_type = 'C',
    visible = '0',
    status = '0',
    icon = 'ezviz',
    update_by = 'migration',
    update_time = CURRENT_TIMESTAMP,
    remark = '萤石云设备列表'
WHERE perms = 'video:ezviz:list';

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(existing.menu_id), 0) + 1 FROM sys_menu existing),
       '萤石云', parent.menu_id, 2, 'ezviz', 'VideoEzviz', 1, 0,
       'C', '0', '0', 'video:ezviz:list', 'ezviz', 'migration', CURRENT_TIMESTAMP,
       'migration', CURRENT_TIMESTAMP, '萤石云设备列表'
FROM sys_menu parent
WHERE parent.path = 'videoCenter'
  AND parent.menu_type = 'M'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'video:ezviz:list')
ORDER BY parent.menu_id
LIMIT 1;

UPDATE sys_menu
SET menu_name = '预览', order_num = 1, menu_type = 'F', visible = '0', status = '0',
    parent_id = (SELECT parent_lookup.menu_id
                 FROM (SELECT parent.menu_id FROM sys_menu parent
                       WHERE parent.perms = 'video:ezviz:list'
                       ORDER BY parent.menu_id LIMIT 1) parent_lookup),
    update_by = 'migration', update_time = CURRENT_TIMESTAMP
WHERE perms = 'video:ezviz:live';

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(existing.menu_id), 0) + 1 FROM sys_menu existing),
       '预览', parent.menu_id, 1, '', '', 1, 0,
       'F', '0', '0', 'video:ezviz:live', '', 'migration', CURRENT_TIMESTAMP,
       'migration', CURRENT_TIMESTAMP, '萤石实时预览权限'
FROM sys_menu parent
WHERE parent.perms = 'video:ezviz:list'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'video:ezviz:live')
ORDER BY parent.menu_id
LIMIT 1;

UPDATE sys_menu
SET menu_name = '回放', order_num = 2, menu_type = 'F', visible = '0', status = '0',
    parent_id = (SELECT parent_lookup.menu_id
                 FROM (SELECT parent.menu_id FROM sys_menu parent
                       WHERE parent.perms = 'video:ezviz:list'
                       ORDER BY parent.menu_id LIMIT 1) parent_lookup),
    update_by = 'migration', update_time = CURRENT_TIMESTAMP
WHERE perms = 'video:ezviz:playback';

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(existing.menu_id), 0) + 1 FROM sys_menu existing),
       '回放', parent.menu_id, 2, '', '', 1, 0,
       'F', '0', '0', 'video:ezviz:playback', '', 'migration', CURRENT_TIMESTAMP,
       'migration', CURRENT_TIMESTAMP, '萤石录像回放权限'
FROM sys_menu parent
WHERE parent.perms = 'video:ezviz:list'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'video:ezviz:playback')
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
      WHEN 'video:imoulife:list' THEN 'video:ezviz:list'
      WHEN 'video:imoulife:live' THEN 'video:ezviz:live'
      WHEN 'video:imoulife:playback' THEN 'video:ezviz:playback'
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
      WHEN 'video:imoulife:list' THEN 'video:ezviz:list'
      WHEN 'video:imoulife:live' THEN 'video:ezviz:live'
      WHEN 'video:imoulife:playback' THEN 'video:ezviz:playback'
    END
  WHERE source_menu.perms IN ('video:imoulife:list', 'video:imoulife:live', 'video:imoulife:playback')
) candidate
CROSS JOIN (SELECT COALESCE(MAX(id), 0) AS max_id FROM iot_tenant_package_menu) ids
WHERE NOT EXISTS (
  SELECT 1 FROM iot_tenant_package_menu existing
  WHERE existing.package_id = candidate.package_id AND existing.menu_id = candidate.menu_id
);
