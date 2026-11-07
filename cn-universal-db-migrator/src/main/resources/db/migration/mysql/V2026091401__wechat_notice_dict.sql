-- MySQL 字典数据请按部署库执行；字段定义与 PostgreSQL 脚本一致。
INSERT INTO sys_dict_type (dict_name,dict_type,status,create_by,create_time,remark)
SELECT '通知渠道类型','notice_channel_type','0','system',NOW(),'通知渠道类型' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='notice_channel_type');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time)
SELECT 90,'微信服务号','wechat','notice_channel_type','','primary','N','0','system',NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='notice_channel_type' AND dict_value='wechat');
INSERT INTO sys_dict_type (dict_name,dict_type,status,create_by,create_time,remark)
SELECT '微信模板渠道字段','notice_wechat_field','0','system',NOW(),'微信模板渠道字段' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='notice_wechat_field');
INSERT IGNORE INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time) VALUES
(1,'公众号 AppID','appId','notice_wechat_field','input|required|公众号 AppID','','N','0','system',NOW()),(2,'公众号 AppSecret','appSecret','notice_wechat_field','password|required|公众号 AppSecret','','N','0','system',NOW()),(3,'模板 ID','templateId','notice_wechat_field','input|required|平台申请的模板 ID','','N','0','system',NOW()),(4,'跳转外链','url','notice_wechat_field','input|optional|可选外链','','N','0','system',NOW()),(5,'小程序 AppID','miniProgramAppId','notice_wechat_field','input|optional|可选小程序 AppID','','N','0','system',NOW()),(6,'小程序页面','miniProgramPage','notice_wechat_field','input|optional|可选页面路径','','N','0','system',NOW());
