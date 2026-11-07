-- Product detail -> 端云配置 fields used by the EZVIZ and Imou hosting providers.
-- Values are stored in iot_product.configuration using the dict_value as the JSON key.

INSERT INTO sys_dict_type
  (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT '萤石云托管配置', 'product_configuration_ezviz', '0', 'migration', NOW(), 'migration', NOW(),
       '萤石云设备托管所需的产品级配置'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'product_configuration_ezviz');

INSERT INTO sys_dict_type
  (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT '乐橙云托管配置', 'product_configuration_imoulife', '0', 'migration', NOW(), 'migration', NOW(),
       '乐橙云设备托管所需的产品级配置'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'product_configuration_imoulife');

INSERT INTO sys_dict_data
  (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time,
   update_by, update_time, remark)
SELECT 1, '托管授权地址', 'hostingUrl', 'product_configuration_ezviz', 'N', '0', 'migration', NOW(),
       'migration', NOW(), '{"remark":"萤石开放平台生成的设备托管授权地址，用于生成扫码二维码","type":"string","hide":"false","default":""}'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'product_configuration_ezviz' AND dict_value = 'hostingUrl');

INSERT INTO sys_dict_data
  (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time,
   update_by, update_time, remark)
SELECT 2, '托管回调地址', 'hostingCallbackUrl', 'product_configuration_ezviz', 'N', '0', 'migration', NOW(),
       'migration', NOW(), '{"remark":"配置到萤石开放平台的回调地址；会话标识会自动绑定当前租户","type":"string","hide":"false","default":""}'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'product_configuration_ezviz' AND dict_value = 'hostingCallbackUrl');

INSERT INTO sys_dict_data
  (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time,
   update_by, update_time, remark)
SELECT 3, '自动接受托管', 'hostingAutoAccept', 'product_configuration_ezviz', 'Y', '0', 'migration', NOW(),
       'migration', NOW(), '{"remark":"收到萤石授权回调后自动接受并入库设备","type":"boolean","hide":"false","default":"true"}'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'product_configuration_ezviz' AND dict_value = 'hostingAutoAccept');

INSERT INTO sys_dict_data
  (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time,
   update_by, update_time, remark)
SELECT 4, '托管权限', 'hostingPermissions', 'product_configuration_ezviz', 'N', '0', 'migration', NOW(),
       'migration', NOW(), '{"remark":"可选，JSON格式的设备托管权限列表，留空使用开放平台默认权限","type":"json","hide":"false","default":""}'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'product_configuration_ezviz' AND dict_value = 'hostingPermissions');

INSERT INTO sys_dict_data
  (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time,
   update_by, update_time, remark)
SELECT 1, '托管授权地址', 'hostingUrl', 'product_configuration_imoulife', 'N', '0', 'migration', NOW(),
       'migration', NOW(), '{"remark":"乐橙开放平台配置的设备托管授权地址，用于生成扫码二维码","type":"string","hide":"false","default":""}'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'product_configuration_imoulife' AND dict_value = 'hostingUrl');

INSERT INTO sys_dict_data
  (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time,
   update_by, update_time, remark)
SELECT 2, '托管回调地址', 'hostingCallbackUrl', 'product_configuration_imoulife', 'N', '0', 'migration', NOW(),
       'migration', NOW(), '{"remark":"配置到乐橙开放平台的回调地址；会话标识会自动绑定当前租户","type":"string","hide":"false","default":""}'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'product_configuration_imoulife' AND dict_value = 'hostingCallbackUrl');

INSERT INTO sys_dict_data
  (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time,
   update_by, update_time, remark)
SELECT 3, '自动接受托管', 'hostingAutoAccept', 'product_configuration_imoulife', 'Y', '0', 'migration', NOW(),
       'migration', NOW(), '{"remark":"收到乐橙授权回调后自动接受并入库设备","type":"boolean","hide":"false","default":"true"}'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'product_configuration_imoulife' AND dict_value = 'hostingAutoAccept');

INSERT INTO sys_dict_data
  (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time,
   update_by, update_time, remark)
SELECT 4, '托管权限', 'hostingPermissions', 'product_configuration_imoulife', 'N', '0', 'migration', NOW(),
       'migration', NOW(), '{"remark":"可选，JSON格式的设备托管权限列表，留空使用开放平台默认权限","type":"json","hide":"false","default":""}'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'product_configuration_imoulife' AND dict_value = 'hostingPermissions');
