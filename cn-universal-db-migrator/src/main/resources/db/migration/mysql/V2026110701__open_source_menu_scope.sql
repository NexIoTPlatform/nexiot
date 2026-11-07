-- MySQL 8: prune menus for excluded OSS modules; preserve all business data and credentials.
DROP PROCEDURE IF EXISTS nexiot_oss_menu_scope_2026110701;
DELIMITER $$
CREATE PROCEDURE nexiot_oss_menu_scope_2026110701()
BEGIN
    CREATE TEMPORARY TABLE nexiot_oss_retired_menu_ids (menu_id BIGINT PRIMARY KEY);
    INSERT INTO nexiot_oss_retired_menu_ids WITH RECURSIVE retired(menu_id) AS (SELECT menu_id FROM sys_menu WHERE menu_id IN (1700,1283,1440,2043977798387777600,2071107100000000009,1230,2058515756470165504,2042241329026695168,2064800000000001000,2071107100000000016,1196,2043977798387777537) OR path IN ('device-template','videoCenter','scada-iot','videoai-factory','nodered','edge','analysis','platform-ai','alarm') OR component='flow/index' OR (COALESCE(perms,'') LIKE 'industrial:%' OR COALESCE(perms,'') LIKE 'video:%' OR COALESCE(perms,'') LIKE 'videoai:%' OR COALESCE(perms,'') LIKE 'scada:%' OR COALESCE(perms,'') LIKE 'flow:%' OR COALESCE(perms,'') LIKE 'alarm:%' OR COALESCE(perms,'') LIKE 'node-red:%' OR COALESCE(perms,'') LIKE 'nodered:%' OR COALESCE(perms,'') LIKE 'analysis:%' OR COALESCE(perms,'') LIKE 'visualization:%' OR COALESCE(perms,'') LIKE 'ai:%' OR COALESCE(perms,'') LIKE 'iot-card:%' OR COALESCE(perms,'') LIKE 'tool:tcp:%' OR COALESCE(perms,'') LIKE 'tool:udp:%') UNION SELECT child.menu_id FROM sys_menu child JOIN retired parent ON child.parent_id=parent.menu_id) SELECT menu_id FROM retired;
    DELETE FROM sys_role_menu WHERE menu_id IN (SELECT menu_id FROM nexiot_oss_retired_menu_ids);
    DELETE FROM iot_tenant_package_menu WHERE menu_id IN (SELECT menu_id FROM nexiot_oss_retired_menu_ids);
    DELETE FROM sys_menu WHERE menu_id IN (SELECT menu_id FROM nexiot_oss_retired_menu_ids);
    DROP TEMPORARY TABLE nexiot_oss_retired_menu_ids;
END$$
DELIMITER ;
START TRANSACTION;
CALL nexiot_oss_menu_scope_2026110701();
COMMIT;
DROP PROCEDURE nexiot_oss_menu_scope_2026110701;
