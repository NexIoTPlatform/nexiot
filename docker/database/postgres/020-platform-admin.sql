-- Explicit core seed allowlist. All other tables remain empty.
SET standard_conforming_strings = on;

-- iot_user: 1 rows; source filter: id = 1 AND tenant_role = 'PLATFORM_ADMIN' AND deleted = 0
INSERT INTO "unix_tenant_202605"."iot_user" ("id", "username", "password", "email", "mobile", "alias", "org_id", "union_id", "salt", "status", "avatar", "parent_union_id", "identity", "login_ip", "login_date", "create_by", "create_date", "update_by", "update_date", "cfg", "remark", "deleted", "tenant_id", "tenant_role", "dept_id") VALUES (1, 'admin', '$2a$12$P6NdJRJChU.hHkTArW5sI.AM.NSb4/8Ote8qvVXemm9y73ZP09e4W', NULL, NULL, 'NexIoT', NULL, 'admin', NULL, 0, NULL, NULL, 0, NULL, NULL, 'admin', '2026-11-07T00:00:00', NULL, NULL, NULL, NULL, 0, '0', 'PLATFORM_ADMIN', NULL);

-- sys_role: 1 rows; source filter: role_id = 1 AND role_key = 'admin'
INSERT INTO "unix_tenant_202605"."sys_role" ("role_id", "role_name", "role_key", "role_sort", "data_scope", "menu_check_strictly", "dept_check_strictly", "status", "create_by", "create_time", "update_by", "update_time", "remark", "tenant_id", "role_type") VALUES (1, '超级管理员', 'admin', 0, '4', 0, 0, '0', 'nexiot', '2021-12-27T10:28:13', NULL, NULL, NULL, NULL, 'PLATFORM_ADMIN');

-- sys_role_menu: 0 rows; source filter: role_id = 1 AND menu_id IN (SELECT menu_id FROM sys_menu WHERE COALESCE(path,'') !~* '^https?://' AND menu_id NOT IN (WITH RECURSIVE retired(menu_id) AS (SELECT menu_id FROM sys_menu WHERE menu_id IN (1700,1283,1440,2043977798387777600,2071107100000000009,1230,2058515756470165504,2042241329026695168,2064800000000001000,2071107100000000016,1196,2043977798387777537) OR path IN ('device-template','videoCenter','scada-iot','videoai-factory','nodered','edge','analysis','platform-ai','alarm') OR component='flow/index' OR (COALESCE(perms,'') LIKE 'industrial:%' OR COALESCE(perms,'') LIKE 'video:%' OR COALESCE(perms,'') LIKE 'videoai:%' OR COALESCE(perms,'') LIKE 'scada:%' OR COALESCE(perms,'') LIKE 'flow:%' OR COALESCE(perms,'') LIKE 'alarm:%' OR COALESCE(perms,'') LIKE 'node-red:%' OR COALESCE(perms,'') LIKE 'nodered:%' OR COALESCE(perms,'') LIKE 'analysis:%' OR COALESCE(perms,'') LIKE 'visualization:%' OR COALESCE(perms,'') LIKE 'ai:%' OR COALESCE(perms,'') LIKE 'iot-card:%' OR COALESCE(perms,'') LIKE 'tool:tcp:%' OR COALESCE(perms,'') LIKE 'tool:udp:%') UNION SELECT child.menu_id FROM sys_menu child JOIN retired parent ON child.parent_id=parent.menu_id) SELECT menu_id FROM retired))

-- sys_user_role: 1 rows; source filter: role_id = 1 AND union_id IN (SELECT union_id FROM iot_user WHERE id = 1 AND tenant_role = 'PLATFORM_ADMIN' AND deleted = 0)
INSERT INTO "unix_tenant_202605"."sys_user_role" ("uuid", "union_id", "role_id") VALUES (2062600000000200001, 'admin', 1);

-- iot_tenant_package: 1 rows; source filter: package_id = 900000000000000001 AND deleted = 0
INSERT INTO "unix_tenant_202605"."iot_tenant_package" ("package_id", "package_name", "status", "remark", "create_by", "create_time", "update_by", "update_time", "deleted") VALUES (900000000000000001, '默认租户套餐', 0, 'NexIoT 默认租户功能套餐（不含任何租户）', 'nexiot', '2026-04-19T13:28:19', 'nexiot', '2026-09-17T09:49:03.854', 0);

-- iot_tenant_package_menu: 205 rows; source filter: package_id = 900000000000000001 AND menu_id IN (SELECT menu_id FROM sys_menu WHERE COALESCE(path,'') !~* '^https?://' AND menu_id NOT IN (WITH RECURSIVE retired(menu_id) AS (SELECT menu_id FROM sys_menu WHERE menu_id IN (1700,1283,1440,2043977798387777600,2071107100000000009,1230,2058515756470165504,2042241329026695168,2064800000000001000,2071107100000000016,1196,2043977798387777537) OR path IN ('device-template','videoCenter','scada-iot','videoai-factory','nodered','edge','analysis','platform-ai','alarm') OR component='flow/index' OR (COALESCE(perms,'') LIKE 'industrial:%' OR COALESCE(perms,'') LIKE 'video:%' OR COALESCE(perms,'') LIKE 'videoai:%' OR COALESCE(perms,'') LIKE 'scada:%' OR COALESCE(perms,'') LIKE 'flow:%' OR COALESCE(perms,'') LIKE 'alarm:%' OR COALESCE(perms,'') LIKE 'node-red:%' OR COALESCE(perms,'') LIKE 'nodered:%' OR COALESCE(perms,'') LIKE 'analysis:%' OR COALESCE(perms,'') LIKE 'visualization:%' OR COALESCE(perms,'') LIKE 'ai:%' OR COALESCE(perms,'') LIKE 'iot-card:%' OR COALESCE(perms,'') LIKE 'tool:tcp:%' OR COALESCE(perms,'') LIKE 'tool:udp:%') UNION SELECT child.menu_id FROM sys_menu child JOIN retired parent ON child.parent_id=parent.menu_id) SELECT menu_id FROM retired))
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610432, 900000000000000001, 1193, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610434, 900000000000000001, 1093, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610435, 900000000000000001, 1095, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610436, 900000000000000001, 1116, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610437, 900000000000000001, 1117, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610438, 900000000000000001, 1118, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610439, 900000000000000001, 1119, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610440, 900000000000000001, 1120, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610441, 900000000000000001, 1125, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610442, 900000000000000001, 1126, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610443, 900000000000000001, 1127, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610444, 900000000000000001, 1128, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610445, 900000000000000001, 1176, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610446, 900000000000000001, 1177, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610447, 900000000000000001, 1178, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610448, 900000000000000001, 1430, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610449, 900000000000000001, 1431, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610450, 900000000000000001, 1432, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610451, 900000000000000001, 1433, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610452, 900000000000000001, 1100, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610453, 900000000000000001, 1101, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610454, 900000000000000001, 1102, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610455, 900000000000000001, 1103, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610456, 900000000000000001, 1104, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610457, 900000000000000001, 1105, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610458, 900000000000000001, 1106, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610459, 900000000000000001, 1096, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610460, 900000000000000001, 1541, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610461, 900000000000000001, 1542, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610462, 900000000000000001, 1543, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610463, 900000000000000001, 1544, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610464, 900000000000000001, 1400, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610465, 900000000000000001, 1401, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610466, 900000000000000001, 1402, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610467, 900000000000000001, 1403, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610468, 900000000000000001, 1404, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610469, 900000000000000001, 1405, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610470, 900000000000000001, 1553, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610471, 900000000000000001, 1552, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610472, 900000000000000001, 1551, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610473, 900000000000000001, 1550, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610474, 900000000000000001, 1420, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610475, 900000000000000001, 1421, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610476, 900000000000000001, 1422, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610477, 900000000000000001, 1423, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610478, 900000000000000001, 1424, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610479, 900000000000000001, 1425, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610480, 900000000000000001, 1098, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610481, 900000000000000001, 1121, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610482, 900000000000000001, 1122, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610483, 900000000000000001, 1123, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610484, 900000000000000001, 1124, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610485, 900000000000000001, 1129, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610486, 900000000000000001, 1099, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610487, 900000000000000001, 1545, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610488, 900000000000000001, 1470, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610489, 900000000000000001, 1471, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610490, 900000000000000001, 1472, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610491, 900000000000000001, 1473, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610492, 900000000000000001, 1474, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610493, 900000000000000001, 1475, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610494, 900000000000000001, 1476, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610495, 900000000000000001, 1477, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610496, 900000000000000001, 1478, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610497, 900000000000000001, 1479, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610498, 900000000000000001, 1480, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610499, 900000000000000001, 1481, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610500, 900000000000000001, 1482, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610501, 900000000000000001, 1483, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610502, 900000000000000001, 1484, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610503, 900000000000000001, 1485, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610504, 900000000000000001, 1486, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610505, 900000000000000001, 1487, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610536, 900000000000000001, 1194, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610537, 900000000000000001, 1195, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610538, 900000000000000001, 1197, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610539, 900000000000000001, 1198, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610540, 900000000000000001, 1199, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610541, 900000000000000001, 1200, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610542, 900000000000000001, 1205, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610543, 900000000000000001, 1206, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610544, 900000000000000001, 1207, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610561, 900000000000000001, 2071107100000000001, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610562, 900000000000000001, 1247, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610563, 900000000000000001, 1248, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610564, 900000000000000001, 1249, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610565, 900000000000000001, 1250, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610566, 900000000000000001, 1140, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610567, 900000000000000001, 1141, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610568, 900000000000000001, 1142, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610569, 900000000000000001, 1143, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610570, 900000000000000001, 1144, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610571, 900000000000000001, 1227, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610572, 900000000000000001, 1145, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610573, 900000000000000001, 1228, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610574, 900000000000000001, 1158, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610575, 900000000000000001, 1229, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610576, 900000000000000001, 1281, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610584, 900000000000000001, 1156, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610585, 900000000000000001, 1107, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610586, 900000000000000001, 1108, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610587, 900000000000000001, 1109, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610588, 900000000000000001, 1110, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610589, 900000000000000001, 1111, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610590, 900000000000000001, 1157, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610591, 900000000000000001, 1112, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610592, 900000000000000001, 1113, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610593, 900000000000000001, 1114, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610594, 900000000000000001, 1115, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610595, 900000000000000001, 1153, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610596, 900000000000000001, 1159, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610597, 900000000000000001, 1160, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610598, 900000000000000001, 1161, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610599, 900000000000000001, 1162, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610600, 900000000000000001, 1163, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610601, 900000000000000001, 1164, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610602, 900000000000000001, 1297, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610603, 900000000000000001, 1298, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610604, 900000000000000001, 1299, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610605, 900000000000000001, 1434, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610606, 900000000000000001, 1435, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610607, 900000000000000001, 1300, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610608, 900000000000000001, 1301, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610609, 900000000000000001, 1302, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610610, 900000000000000001, 1436, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610611, 900000000000000001, 1303, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610612, 900000000000000001, 1211, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610613, 900000000000000001, 1212, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610614, 900000000000000001, 1213, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610615, 900000000000000001, 1214, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610616, 900000000000000001, 1215, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610617, 900000000000000001, 1216, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610618, 900000000000000001, 1217, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610619, 900000000000000001, 1218, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610620, 900000000000000001, 1219, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610621, 900000000000000001, 1220, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610622, 900000000000000001, 1221, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610682, 900000000000000001, 1490, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610683, 900000000000000001, 1495, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610684, 900000000000000001, 1492, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610685, 900000000000000001, 1493, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610686, 900000000000000001, 1494, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610687, 900000000000000001, 1496, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610688, 900000000000000001, 1497, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610689, 900000000000000001, 1498, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610690, 900000000000000001, 1546, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610691, 900000000000000001, 1547, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610692, 900000000000000001, 1548, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610693, 900000000000000001, 1549, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610694, 900000000000000001, 1491, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610695, 900000000000000001, 1499, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610696, 900000000000000001, 1500, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610697, 900000000000000001, 1501, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610720, 900000000000000001, 1, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610721, 900000000000000001, 100, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610722, 900000000000000001, 1001, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610723, 900000000000000001, 1002, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610724, 900000000000000001, 1003, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610725, 900000000000000001, 1004, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610726, 900000000000000001, 1005, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610727, 900000000000000001, 1006, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610728, 900000000000000001, 1007, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610729, 900000000000000001, 101, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610730, 900000000000000001, 1008, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610731, 900000000000000001, 1009, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610732, 900000000000000001, 1010, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610733, 900000000000000001, 1011, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610734, 900000000000000001, 1012, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610735, 900000000000000001, 115, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610736, 900000000000000001, 1055, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610737, 900000000000000001, 1056, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610738, 900000000000000001, 1058, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610739, 900000000000000001, 1057, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610740, 900000000000000001, 1059, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610741, 900000000000000001, 1060, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610742, 900000000000000001, 102, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610743, 900000000000000001, 1013, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610744, 900000000000000001, 1014, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610745, 900000000000000001, 1015, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610746, 900000000000000001, 1016, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610747, 900000000000000001, 1520, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610748, 900000000000000001, 1523, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610749, 900000000000000001, 1524, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610750, 900000000000000001, 1525, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610751, 900000000000000001, 1526, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610752, 900000000000000001, 105, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610753, 900000000000000001, 1026, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610754, 900000000000000001, 1027, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610755, 900000000000000001, 1028, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610756, 900000000000000001, 1029, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610757, 900000000000000001, 1030, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610758, 900000000000000001, 106, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610759, 900000000000000001, 1031, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610760, 900000000000000001, 1032, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610761, 900000000000000001, 1033, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610762, 900000000000000001, 1034, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610763, 900000000000000001, 1035, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610764, 900000000000000001, 2049000000000000003, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610765, 900000000000000001, 1488, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610766, 900000000000000001, 1489, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610772, 900000000000000001, 1172, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610773, 900000000000000001, 1174, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610774, 900000000000000001, 1175, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610775, 900000000000000001, 1097, '2026-09-17T09:49:03.886');
INSERT INTO "unix_tenant_202605"."iot_tenant_package_menu" ("id", "package_id", "menu_id", "create_time") VALUES (2100401617678610777, 900000000000000001, 1081, '2026-09-17T09:49:03.886');

-- Point-template permissions for the platform administrator and default tenant package.

-- Default platform administrator can manage the four new operation families.





-- Default tenant package exposes the same editable video operations.





-- The default administrator/package already has the legacy video read boundary.


