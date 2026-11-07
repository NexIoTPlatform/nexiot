INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time)
SELECT 7,'微信授权回调地址','oauthCallbackUrl','notice_wechat_field','input|optional|公网地址，以 /open-api/v1/notice/wechat/callback 结尾','','N','0','system',CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='notice_wechat_field' AND dict_value='oauthCallbackUrl');
