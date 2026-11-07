-- Dameng 8 standalone increment: remove only excluded OSS menus and related grants.
-- The current Java migrator executes PostgreSQL/MySQL migrations only.
BEGIN
    LOCK TABLE sys_menu IN EXCLUSIVE MODE;
    LOCK TABLE sys_role_menu IN EXCLUSIVE MODE;
    LOCK TABLE iot_tenant_package_menu IN EXCLUSIVE MODE;
    DELETE FROM sys_role_menu WHERE menu_id IN (
        SELECT menu_id FROM sys_menu START WITH menu_id IN (1700,1283,1440,2043977798387777600,2071107100000000009,1230,2058515756470165504,2042241329026695168,2064800000000001000,2071107100000000016,1196,2043977798387777537) OR path IN ('device-template','videoCenter','scada-iot','videoai-factory','nodered','edge','analysis','platform-ai','alarm') OR component='flow/index' OR (COALESCE(perms,'') LIKE 'industrial:%' OR COALESCE(perms,'') LIKE 'video:%' OR COALESCE(perms,'') LIKE 'videoai:%' OR COALESCE(perms,'') LIKE 'scada:%' OR COALESCE(perms,'') LIKE 'flow:%' OR COALESCE(perms,'') LIKE 'alarm:%' OR COALESCE(perms,'') LIKE 'node-red:%' OR COALESCE(perms,'') LIKE 'nodered:%' OR COALESCE(perms,'') LIKE 'analysis:%' OR COALESCE(perms,'') LIKE 'visualization:%' OR COALESCE(perms,'') LIKE 'ai:%' OR COALESCE(perms,'') LIKE 'iot-card:%' OR COALESCE(perms,'') LIKE 'tool:tcp:%' OR COALESCE(perms,'') LIKE 'tool:udp:%')
        CONNECT BY NOCYCLE PRIOR menu_id=parent_id);
    DELETE FROM iot_tenant_package_menu WHERE menu_id IN (
        SELECT menu_id FROM sys_menu START WITH menu_id IN (1700,1283,1440,2043977798387777600,2071107100000000009,1230,2058515756470165504,2042241329026695168,2064800000000001000,2071107100000000016,1196,2043977798387777537) OR path IN ('device-template','videoCenter','scada-iot','videoai-factory','nodered','edge','analysis','platform-ai','alarm') OR component='flow/index' OR (COALESCE(perms,'') LIKE 'industrial:%' OR COALESCE(perms,'') LIKE 'video:%' OR COALESCE(perms,'') LIKE 'videoai:%' OR COALESCE(perms,'') LIKE 'scada:%' OR COALESCE(perms,'') LIKE 'flow:%' OR COALESCE(perms,'') LIKE 'alarm:%' OR COALESCE(perms,'') LIKE 'node-red:%' OR COALESCE(perms,'') LIKE 'nodered:%' OR COALESCE(perms,'') LIKE 'analysis:%' OR COALESCE(perms,'') LIKE 'visualization:%' OR COALESCE(perms,'') LIKE 'ai:%' OR COALESCE(perms,'') LIKE 'iot-card:%' OR COALESCE(perms,'') LIKE 'tool:tcp:%' OR COALESCE(perms,'') LIKE 'tool:udp:%')
        CONNECT BY NOCYCLE PRIOR menu_id=parent_id);
    DELETE FROM sys_menu WHERE menu_id IN (
        SELECT menu_id FROM sys_menu START WITH menu_id IN (1700,1283,1440,2043977798387777600,2071107100000000009,1230,2058515756470165504,2042241329026695168,2064800000000001000,2071107100000000016,1196,2043977798387777537) OR path IN ('device-template','videoCenter','scada-iot','videoai-factory','nodered','edge','analysis','platform-ai','alarm') OR component='flow/index' OR (COALESCE(perms,'') LIKE 'industrial:%' OR COALESCE(perms,'') LIKE 'video:%' OR COALESCE(perms,'') LIKE 'videoai:%' OR COALESCE(perms,'') LIKE 'scada:%' OR COALESCE(perms,'') LIKE 'flow:%' OR COALESCE(perms,'') LIKE 'alarm:%' OR COALESCE(perms,'') LIKE 'node-red:%' OR COALESCE(perms,'') LIKE 'nodered:%' OR COALESCE(perms,'') LIKE 'analysis:%' OR COALESCE(perms,'') LIKE 'visualization:%' OR COALESCE(perms,'') LIKE 'ai:%' OR COALESCE(perms,'') LIKE 'iot-card:%' OR COALESCE(perms,'') LIKE 'tool:tcp:%' OR COALESCE(perms,'') LIKE 'tool:udp:%')
        CONNECT BY NOCYCLE PRIOR menu_id=parent_id);
    COMMIT;
EXCEPTION WHEN OTHERS THEN
    ROLLBACK;
    RAISE;
END;
/
