-- Keep the original applied V2026091401 checksum; label changes belong in a new migration.
UPDATE sys_dict_data SET dict_label = '微信服务号' WHERE dict_type = 'notice_channel_type' AND dict_value = 'wechat' AND dict_label = '微信服务号模板消息';
