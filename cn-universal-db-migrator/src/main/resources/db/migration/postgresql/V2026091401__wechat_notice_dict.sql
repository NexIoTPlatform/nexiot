-- 微信服务号通知渠道及字段元数据（幂等，可重复执行）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '通知渠道类型', 'notice_channel_type', '0', 'system', CURRENT_TIMESTAMP, '通知渠道类型'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'notice_channel_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT 90, '微信服务号模板消息', 'wechat', 'notice_channel_type', '', 'primary', 'N', '0', 'system', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='notice_channel_type' AND dict_value='wechat');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '微信模板渠道字段', 'notice_wechat_field', '0', 'system', CURRENT_TIMESTAMP, 'key 字段，css_class 存储 type|required|secret|placeholder 元数据'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'notice_wechat_field');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT v.sort, v.label, v.key, 'notice_wechat_field', v.meta, '', 'N', '0', 'system', CURRENT_TIMESTAMP
FROM (VALUES (1,'公众号 AppID','appId','input|required|公众号 AppID'),(2,'公众号 AppSecret','appSecret','password|required|公众号 AppSecret'),(3,'模板 ID','templateId','input|required|平台申请的模板 ID'),(4,'跳转外链','url','input|optional|可选外链'),(5,'小程序 AppID','miniProgramAppId','input|optional|可选小程序 AppID'),(6,'小程序页面','miniProgramPage','input|optional|可选页面路径')) v(sort,label,key,meta)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type='notice_wechat_field' AND d.dict_value=v.key);
