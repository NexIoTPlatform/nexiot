-- NexIoT OSS: remove only menus/permissions for excluded modules, preserving business tables/data.
-- Installation credentials are intentionally not changed by an existing-environment migration.
SET LOCAL lock_timeout = '10s';
LOCK TABLE sys_menu, sys_role_menu, iot_tenant_package_menu IN SHARE ROW EXCLUSIVE MODE;
DO $oss_scope$
DECLARE retired_ids bigint[];
BEGIN
    SELECT ARRAY_AGG(menu_id) INTO retired_ids FROM (WITH RECURSIVE retired(menu_id) AS (SELECT menu_id FROM sys_menu WHERE menu_id IN (1700,1283,1440,2043977798387777600,2071107100000000009,1230,2058515756470165504,2042241329026695168,2064800000000001000,2071107100000000016,1196,2043977798387777537) OR path IN ('device-template','videoCenter','scada-iot','videoai-factory','nodered','edge','analysis','platform-ai','alarm') OR component='flow/index' OR (COALESCE(perms,'') LIKE 'industrial:%' OR COALESCE(perms,'') LIKE 'video:%' OR COALESCE(perms,'') LIKE 'videoai:%' OR COALESCE(perms,'') LIKE 'scada:%' OR COALESCE(perms,'') LIKE 'flow:%' OR COALESCE(perms,'') LIKE 'alarm:%' OR COALESCE(perms,'') LIKE 'node-red:%' OR COALESCE(perms,'') LIKE 'nodered:%' OR COALESCE(perms,'') LIKE 'analysis:%' OR COALESCE(perms,'') LIKE 'visualization:%' OR COALESCE(perms,'') LIKE 'ai:%' OR COALESCE(perms,'') LIKE 'iot-card:%' OR COALESCE(perms,'') LIKE 'tool:tcp:%' OR COALESCE(perms,'') LIKE 'tool:udp:%') UNION SELECT child.menu_id FROM sys_menu child JOIN retired parent ON child.parent_id=parent.menu_id) SELECT menu_id FROM retired) retired_scope;
    DELETE FROM sys_role_menu WHERE menu_id=ANY(retired_ids);
    DELETE FROM iot_tenant_package_menu WHERE menu_id=ANY(retired_ids);
    DELETE FROM sys_menu WHERE menu_id=ANY(retired_ids);
END $oss_scope$;
