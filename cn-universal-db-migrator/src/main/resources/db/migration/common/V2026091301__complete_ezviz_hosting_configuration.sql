-- Product-scoped EZVIZ credentials and the actual callback contract.
INSERT INTO sys_dict_data
  (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time,
   update_by, update_time, remark)
SELECT 0, 'AppKey', 'appKey', 'product_configuration_ezviz', 'N', '0', 'migration', CURRENT_TIMESTAMP,
       'migration', CURRENT_TIMESTAMP, '{"remark":"与萤石托管中间页属于同一应用的 AppKey","type":"string","hide":"false","default":""}'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'product_configuration_ezviz' AND dict_value = 'appKey');

INSERT INTO sys_dict_data
  (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time,
   update_by, update_time, remark)
SELECT 1, 'AppSecret', 'appSecret', 'product_configuration_ezviz', 'N', '0', 'migration', CURRENT_TIMESTAMP,
       'migration', CURRENT_TIMESTAMP, '{"remark":"萤石应用 AppSecret，仅用于服务端获取令牌","type":"password","hide":"false","default":""}'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'product_configuration_ezviz' AND dict_value = 'appSecret');

UPDATE sys_dict_data
SET remark = '{"remark":"在萤石控制台填写 https://域名/api/iot/ezviz/events，GET 成功响应为 code 字符串 200","type":"string","hide":"false","default":""}',
    update_by = 'migration', update_time = CURRENT_TIMESTAMP
WHERE dict_type = 'product_configuration_ezviz' AND dict_value = 'hostingCallbackUrl';

-- EZVIZ has no LeChange-style accept API or local permission-setting API.
-- Keep existing stored product values, but hide these nonfunctional form controls.
UPDATE sys_dict_data
SET status = '1', update_by = 'migration', update_time = CURRENT_TIMESTAMP
WHERE dict_type = 'product_configuration_ezviz' AND dict_value IN ('hostingAutoAccept', 'hostingPermissions');
