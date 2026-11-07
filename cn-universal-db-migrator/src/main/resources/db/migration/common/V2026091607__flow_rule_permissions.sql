-- 新规则引擎拥有独立权限，不继承旧场景联动授权。
INSERT INTO sys_menu
(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT COALESCE(MAX(menu_id),0)+1,'规则引擎',0,35,'flow','flow/index',1,0,'C','0','0','flow:rule:list','branches','migration',CURRENT_TIMESTAMP,'独立规则工作台'
FROM sys_menu WHERE NOT EXISTS (SELECT 1 FROM sys_menu existing WHERE existing.perms='flow:rule:list')
HAVING COUNT(*) > 0;

INSERT INTO sys_menu
(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT (SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu),'查看规则',parent.menu_id,1,'','',1,0,'F','0','0','flow:rule:query','','migration',CURRENT_TIMESTAMP,'规则引擎独立操作权限'
FROM sys_menu parent WHERE parent.perms='flow:rule:list'
AND NOT EXISTS(SELECT 1 FROM sys_menu existing WHERE existing.perms='flow:rule:query');

INSERT INTO sys_menu
(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT (SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu),'创建规则',parent.menu_id,2,'','',1,0,'F','0','0','flow:rule:add','','migration',CURRENT_TIMESTAMP,'规则引擎独立操作权限'
FROM sys_menu parent WHERE parent.perms='flow:rule:list'
AND NOT EXISTS(SELECT 1 FROM sys_menu existing WHERE existing.perms='flow:rule:add');

INSERT INTO sys_menu
(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT (SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu),'编辑规则',parent.menu_id,3,'','',1,0,'F','0','0','flow:rule:edit','','migration',CURRENT_TIMESTAMP,'规则引擎独立操作权限'
FROM sys_menu parent WHERE parent.perms='flow:rule:list'
AND NOT EXISTS(SELECT 1 FROM sys_menu existing WHERE existing.perms='flow:rule:edit');

INSERT INTO sys_menu
(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT (SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu),'删除规则',parent.menu_id,4,'','',1,0,'F','0','0','flow:rule:remove','','migration',CURRENT_TIMESTAMP,'规则引擎独立操作权限'
FROM sys_menu parent WHERE parent.perms='flow:rule:list'
AND NOT EXISTS(SELECT 1 FROM sys_menu existing WHERE existing.perms='flow:rule:remove');

INSERT INTO sys_menu
(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT (SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu),'发布停用规则',parent.menu_id,5,'','',1,0,'F','0','0','flow:rule:publish','','migration',CURRENT_TIMESTAMP,'规则引擎独立操作权限'
FROM sys_menu parent WHERE parent.perms='flow:rule:list'
AND NOT EXISTS(SELECT 1 FROM sys_menu existing WHERE existing.perms='flow:rule:publish');

INSERT INTO sys_menu
(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT (SELECT COALESCE(MAX(menu_id),0)+1 FROM sys_menu),'调试规则',parent.menu_id,6,'','',1,0,'F','0','0','flow:rule:debug','','migration',CURRENT_TIMESTAMP,'规则引擎独立操作权限'
FROM sys_menu parent WHERE parent.perms='flow:rule:list'
AND NOT EXISTS(SELECT 1 FROM sys_menu existing WHERE existing.perms='flow:rule:debug');
