-- 宇视 NETDEVSDK V2.8.1.0 NetDEVSDK.h 的 NETDEV_ALARM_TYPE_E 官方智能告警。
-- 1003 越界、1004 区域入侵、1015 进入区域、1016 离开区域；不推断其它原始码。
-- 仅补缺失映射，保留已有自定义名称、语义和停用状态，不改订阅配置及历史告警。
WITH seed(sort, label, value, config) AS (VALUES
  (9, '越界告警', '0x000003EB', '{"type":"line_crossing","eventId":"lineCrossing","sdkCode":"NETDEV_ALARM_LINE_CROSS (1003)","aliases":["uniview_alarm_1003"]}'),
  (10, '区域入侵', '0x000003EC', '{"type":"intrusion","eventId":"regionIntrusion","sdkCode":"NETDEV_ALARM_OBJECTS_INSIDE (1004)","aliases":["uniview_alarm_1004"]}'),
  (11, '进入区域', '0x000003F7', '{"type":"region_enter","eventId":"regionEntrance","sdkCode":"NETDEV_ALARM_ENTER_AREA (1015)","aliases":["uniview_alarm_1015"]}'),
  (12, '离开区域', '0x000003F8', '{"type":"region_exit","eventId":"regionExit","sdkCode":"NETDEV_ALARM_LEAVE_AREA (1016)","aliases":["uniview_alarm_1016"]}')
)
INSERT INTO sys_dict_data
  (dict_code, dict_sort, dict_label, dict_value, dict_type, is_default, status,
   create_by, create_time, remark)
SELECT ids.base + ROW_NUMBER() OVER (ORDER BY s.sort), s.sort, s.label, s.value,
       'video_sdk_event_uniview_sdk', 'N', '0', 'migration', CURRENT_TIMESTAMP, s.config
FROM seed s CROSS JOIN (SELECT COALESCE(MAX(dict_code), 0) AS base FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data
                  WHERE dict_type = 'video_sdk_event_uniview_sdk' AND dict_value = s.value);
