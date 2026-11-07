-- 宇视 NETDEVSDK 设备直连：平台类型、连接字段和已解码的基础告警。
-- NETDEV_ALARM_TYPE_E：1/2 移动侦测，3/4 视频丢失，5/6 遮挡，7/8 报警输入。
-- 奇数为产生，偶数为恢复；其它事件保留 univiewSdkEvent 原始事件，不推定语义。
-- 幂等追加并保留用户已有字段、停用状态及事件映射。仅用于已有环境增量升级。

INSERT INTO sys_dict_data
  (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
   is_default, status, create_by, create_time, remark)
SELECT (SELECT COALESCE(MAX(dict_code), 0) + 1 FROM sys_dict_data),
       9, '宇视设备直连SDK', 'uniview_sdk', 'video_platform_type', '', 'primary',
       'N', '0', 'migration', CURRENT_TIMESTAMP, '通过独立SDK网关直连宇视设备'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data
                  WHERE dict_type = 'video_platform_type' AND dict_value = 'uniview_sdk');

WITH seed(name, type, remark) AS (VALUES
  ('宇视直连SDK配置字段', 'video_uniview_sdk_fields', '宇视 NETDEVSDK 网关配置字段'),
  ('宇视SDK告警事件', 'video_sdk_event_uniview_sdk', 'NETDEV_ALARM_TYPE_E 原始报警类型，起止共用稳定物模型标识')
)
INSERT INTO sys_dict_type
  (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
SELECT ids.base + ROW_NUMBER() OVER (ORDER BY s.type), s.name, s.type, '0',
       'migration', CURRENT_TIMESTAMP, s.remark
FROM seed s CROSS JOIN (SELECT COALESCE(MAX(dict_id), 0) AS base FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = s.type);

WITH seed(sort, label, value, required, config) AS (VALUES
  (1, 'SDK网关地址', 'endpoint', 'Y', '{"type":"input","required":true,"placeholder":"http://127.0.0.1:5102"}'),
  (2, '网关Token', 'token', 'N', '{"type":"password","required":false,"placeholder":"SDK网关鉴权Token，可选"}'),
  (3, '默认用户名', 'defaultUsername', 'N', '{"type":"input","required":false,"placeholder":"设备未单独配置时使用"}'),
  (4, '默认密码', 'defaultPassword', 'N', '{"type":"password","required":false,"placeholder":"设备未单独配置时使用"}'),
  (5, 'SDK端口', 'sdkPortDefault', 'N', '{"type":"number","required":false,"min":1,"max":65535,"placeholder":"默认 80"}'),
  (6, 'RTSP端口', 'rtspPortDefault', 'N', '{"type":"number","required":false,"min":1,"max":65535,"placeholder":"默认 554"}'),
  (7, '取流策略', 'streamMode', 'N', '{"type":"select","required":false,"options":["rtsp_first","sdk_fallback","sdk_only"],"placeholder":"默认 rtsp_first"}'),
  (8, 'ZLM节点ID', 'zlmMediaServerId', 'N', '{"type":"input","required":false,"placeholder":"留空则自动选择可用ZLM节点"}')
)
INSERT INTO sys_dict_data
  (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
   is_default, status, create_by, create_time, remark)
SELECT ids.base + ROW_NUMBER() OVER (ORDER BY s.sort), s.sort, s.label, s.value,
       'video_uniview_sdk_fields', '', '', s.required, '0', 'migration', CURRENT_TIMESTAMP, s.config
FROM seed s CROSS JOIN (SELECT COALESCE(MAX(dict_code), 0) AS base FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data
                  WHERE dict_type = 'video_uniview_sdk_fields' AND dict_value = s.value);

WITH seed(sort, label, value, config) AS (VALUES
  (1, '移动侦测', '0x00000001', '{"type":"motion","eventId":"motionDetection","sdkCode":"NETDEV_ALARM_TYPE_E (1)","aliases":["uniview_alarm_1"]}'),
  (2, '移动侦测', '0x00000002', '{"type":"motion","eventId":"motionDetection","sdkCode":"NETDEV_ALARM_TYPE_E (2)","aliases":["uniview_alarm_2"]}'),
  (3, '视频丢失', '0x00000003', '{"type":"video_lost","eventId":"videoLoss","sdkCode":"NETDEV_ALARM_TYPE_E (3)","aliases":["uniview_alarm_3"]}'),
  (4, '视频丢失', '0x00000004', '{"type":"video_lost","eventId":"videoLoss","sdkCode":"NETDEV_ALARM_TYPE_E (4)","aliases":["uniview_alarm_4"]}'),
  (5, '视频遮挡', '0x00000005', '{"type":"shelter","eventId":"videoTamper","sdkCode":"NETDEV_ALARM_TYPE_E (5)","aliases":["uniview_alarm_5"]}'),
  (6, '视频遮挡', '0x00000006', '{"type":"shelter","eventId":"videoTamper","sdkCode":"NETDEV_ALARM_TYPE_E (6)","aliases":["uniview_alarm_6"]}'),
  (7, '报警输入', '0x00000007', '{"type":"alarm_input","eventId":"alarmInput","sdkCode":"NETDEV_ALARM_TYPE_E (7)","aliases":["uniview_alarm_7"]}'),
  (8, '报警输入', '0x00000008', '{"type":"alarm_input","eventId":"alarmInput","sdkCode":"NETDEV_ALARM_TYPE_E (8)","aliases":["uniview_alarm_8"]}')
)
INSERT INTO sys_dict_data
  (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status,
   create_by, create_time, remark)
SELECT ids.base + ROW_NUMBER() OVER (ORDER BY s.sort), s.sort, s.label, s.value,
       'video_sdk_event_uniview_sdk', 'N', '0', 'migration', CURRENT_TIMESTAMP, s.config
FROM seed s CROSS JOIN (SELECT COALESCE(MAX(dict_code), 0) AS base FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data
                  WHERE dict_type = 'video_sdk_event_uniview_sdk' AND dict_value = s.value);
