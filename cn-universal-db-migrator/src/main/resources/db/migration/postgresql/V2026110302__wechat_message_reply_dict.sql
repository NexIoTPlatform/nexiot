-- 公众号回复字典：remark 为正文，保留真实换行和 emoji；仅补缺失项，不覆盖现场文案。
-- wechat_keyword_reply 使用 dict_label 完整匹配消息或事件键，dict_sort 越小越优先。
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
SELECT 2096110302000000001, '微信公众号回复', 'wechat_reply', '0', 'migration', CURRENT_TIMESTAMP, 'dict_value 为场景键，remark 为回复正文；留空或禁用使用内置文案。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'wechat_reply');

INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
SELECT 2096110302000000002, '微信公众号关键词回复', 'wechat_keyword_reply', '0', 'migration', CURRENT_TIMESTAMP, 'dict_label 为完整匹配关键词，dict_value 为管理标识，remark 为回复正文。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'wechat_keyword_reply');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000001, 1, '默认回复', 'default', 'wechat_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '你好，欢迎关注 NexIoT 👋
如需登录或绑定，请在页面扫码，或发送页面显示的验证码。
回复“帮助”查看使用说明。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_reply' AND dict_value = 'default');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000002, 2, '关注回复', 'subscribe', 'wechat_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '你好，欢迎关注 NexIoT 👋
如需登录或绑定，请在页面扫码，或发送页面显示的验证码。
回复“帮助”查看使用说明。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_reply' AND dict_value = 'subscribe');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000003, 3, '扫码回复', 'scan', 'wechat_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '你好，欢迎关注 NexIoT 👋
如需登录或绑定，请在页面扫码，或发送页面显示的验证码。
回复“帮助”查看使用说明。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_reply' AND dict_value = 'scan');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000004, 4, '事件回复', 'event', 'wechat_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '你好，欢迎关注 NexIoT 👋
如需登录或绑定，请在页面扫码，或发送页面显示的验证码。
回复“帮助”查看使用说明。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_reply' AND dict_value = 'event');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000005, 5, '登录成功', 'login_success', 'wechat_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '登陆成功。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_reply' AND dict_value = 'login_success');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000006, 6, '验证码无效', 'code_invalid', 'wechat_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '验证码无效或已过期，请在 NexIoT 页面刷新后重新发送。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_reply' AND dict_value = 'code_invalid');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000007, 7, '会话过期', 'session_expired', 'wechat_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '二维码或验证码已过期，请回到 NexIoT 页面刷新。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_reply' AND dict_value = 'session_expired');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000008, 8, '公众号不匹配', 'account_mismatch', 'wechat_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '当前验证码不属于此公众号，请核对登录页面。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_reply' AND dict_value = 'account_mismatch');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000009, 9, '会话已处理', 'session_processed', 'wechat_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '当前扫码会话已处理，请回到 NexIoT 页面继续操作。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_reply' AND dict_value = 'session_processed');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000010, 10, '配置变更', 'config_changed', 'wechat_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '公众号设置已变更，请重新生成二维码。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_reply' AND dict_value = 'config_changed');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000011, 11, '绑定已关闭', 'binding_disabled', 'wechat_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '当前租户已关闭微信绑定。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_reply' AND dict_value = 'binding_disabled');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000012, 12, '等待确认绑定', 'binding_pending', 'wechat_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '已识别本次绑定请求，请回到发起操作的 NexIoT 个人中心确认绑定。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_reply' AND dict_value = 'binding_pending');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000013, 13, '微信未绑定', 'unbound', 'wechat_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '该微信尚未绑定当前租户账号，请先使用账号密码登录，在个人中心绑定微信。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_reply' AND dict_value = 'unbound');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000014, 14, '账号不可用', 'account_unavailable', 'wechat_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '当前账号或租户暂不可用，请联系管理员。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_reply' AND dict_value = 'account_unavailable');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000015, 1, '帮助', 'help', 'wechat_keyword_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '使用帮助 📖
登录：在 NexIoT 登录页扫码，或发送页面显示的验证码。
绑定：使用账号密码登录后，在个人中心发起微信绑定，并返回页面确认。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_keyword_reply' AND dict_value = 'help');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000016, 2, '登录', 'login', 'wechat_keyword_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '登录指引 🔐
请打开 NexIoT 登录页，选择微信登录并扫码，或发送页面显示的验证码。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_keyword_reply' AND dict_value = 'login');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
SELECT 2096110302000000017, 3, '绑定', 'bind', 'wechat_keyword_reply', 'N', '0', 'migration', CURRENT_TIMESTAMP, '绑定指引 🔗
请先使用账号密码登录，在个人中心发起微信绑定，按页面提示操作并确认。'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wechat_keyword_reply' AND dict_value = 'bind');
