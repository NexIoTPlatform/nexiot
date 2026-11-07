-- 天地伟业 CH-NetSDK V5.5 设备直连：平台类型、连接字段、基础告警物模型。
-- 来源：官方头文件 NetClientTypes.h:4576 的 ALARM_* 报警类型（ALARM_NOTIFY_V4）。
-- 复合 VCA/设备特有事件不猜测子类型，由 tiandySdkEvent 保留原始事件。
-- 幂等追加，保留用户已有字段配置和事件映射；psql 请使用 --single-transaction。

INSERT INTO sys_dict_data
  (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
   is_default, status, create_by, create_time, remark)
SELECT (SELECT COALESCE(MAX(dict_code), 0) + 1 FROM sys_dict_data),
       8, '天地伟业设备直连SDK', 'tiandy_sdk', 'video_platform_type', '', 'primary',
       'N', '0', 'migration', CURRENT_TIMESTAMP, '通过独立SDK网关直连天地伟业设备'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data
                  WHERE dict_type = 'video_platform_type' AND dict_value = 'tiandy_sdk');

WITH seed(name, type, remark) AS (VALUES
  ('天地伟业直连SDK配置字段', 'video_tiandy_sdk_fields', '天地伟业 CH-NetSDK 网关配置字段'),
  ('天地伟业SDK告警事件', 'video_sdk_event_tiandy_sdk', 'ALARM_NOTIFY_V4 原始报警类型，备注JSON定义稳定物模型标识')
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
  (5, 'SDK端口', 'sdkPortDefault', 'N', '{"type":"number","required":false,"min":1,"max":65535,"placeholder":"默认 3000"}'),
  (6, 'RTSP端口', 'rtspPortDefault', 'N', '{"type":"number","required":false,"min":1,"max":65535,"placeholder":"默认 554"}'),
  (7, '取流策略', 'streamMode', 'N', '{"type":"select","required":false,"options":["rtsp_first","sdk_fallback","sdk_only"],"placeholder":"默认 rtsp_first"}'),
  (8, 'ZLM节点ID', 'zlmMediaServerId', 'N', '{"type":"input","required":false,"placeholder":"留空则自动选择可用ZLM节点"}')
)
INSERT INTO sys_dict_data
  (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
   is_default, status, create_by, create_time, remark)
SELECT ids.base + ROW_NUMBER() OVER (ORDER BY s.sort), s.sort, s.label, s.value,
       'video_tiandy_sdk_fields', '', '', s.required, '0', 'migration', CURRENT_TIMESTAMP, s.config
FROM seed s CROSS JOIN (SELECT COALESCE(MAX(dict_code), 0) AS base FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data
                  WHERE dict_type = 'video_tiandy_sdk_fields' AND dict_value = s.value);

WITH seed(sort, label, value, config) AS (VALUES
  (1, '移动侦测', '0x00000000', '{"type":"motion","eventId":"motionDetection","sdkCode":"ALARM_VDO_MOTION","aliases":["tiandy_alarm_0"]}'),
  (2, '录像丢失', '0x00000001', '{"type":"record_lost","eventId":"recordingLoss","sdkCode":"ALARM_VDO_REC","aliases":["tiandy_alarm_1"]}'),
  (3, '视频丢失', '0x00000002', '{"type":"video_lost","eventId":"videoLoss","sdkCode":"ALARM_VDO_LOST","aliases":["tiandy_alarm_2"]}'),
  (4, '报警输入', '0x00000003', '{"type":"alarm_input","eventId":"alarmInput","sdkCode":"ALARM_VDO_INPORT","aliases":["tiandy_alarm_3"]}'),
  (5, '报警输出', '0x00000004', '{"type":"alarm_output","eventId":"alarmOutput","sdkCode":"ALARM_VDO_OUTPORT","aliases":["tiandy_alarm_4"]}'),
  (6, '视频遮挡', '0x00000005', '{"type":"shelter","eventId":"videoTamper","sdkCode":"ALARM_VDO_COVER","aliases":["tiandy_alarm_5"]}'),
  (8, '音频丢失', '0x00000007', '{"type":"audio_lost","eventId":"audioLoss","sdkCode":"ALARM_AUDIO_LOST","aliases":["tiandy_alarm_7"]}'),
  (9, '设备异常', '0x00000008', '{"type":"device_exception","eventId":"deviceException","sdkCode":"ALARM_EXCEPTION","aliases":["tiandy_alarm_8"]}')
)
INSERT INTO sys_dict_data
  (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status,
   create_by, create_time, remark)
SELECT ids.base + ROW_NUMBER() OVER (ORDER BY s.sort), s.sort, s.label, s.value,
       'video_sdk_event_tiandy_sdk', 'N', '0', 'migration', CURRENT_TIMESTAMP, s.config
FROM seed s CROSS JOIN (SELECT COALESCE(MAX(dict_code), 0) AS base FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data
                  WHERE dict_type = 'video_sdk_event_tiandy_sdk' AND dict_value = s.value);
