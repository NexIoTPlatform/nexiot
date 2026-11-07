-- GB/T 28181-2022 A.2.5.3 报警通知：AlarmMethod[:AlarmType] 十进制联合键。
-- AlarmMethod=2 且不携带 AlarmType 时是默认设备报警；未知/歧义编号由 cameraAlarm 保留。
-- 与 SDK 对齐稳定 eventId（如 5:2=motionDetection、5:6=regionIntrusion）。
-- 仅补缺失字典，不覆盖已有标签、状态、备注或用户物模型；产品由应用模板补齐。
-- 继承目标列的字符集/排序规则，兼容已有 MySQL 字典表。
DROP TEMPORARY TABLE IF EXISTS wvp_gb28181_event_seed;
CREATE TEMPORARY TABLE wvp_gb28181_event_seed AS
  SELECT dict_sort AS sort, dict_label AS label, dict_value AS value, remark AS config
  FROM sys_dict_data WHERE 1=0;
INSERT INTO wvp_gb28181_event_seed (sort,label,value,config) VALUES
  (1,'电话报警','1','{"type":"phone_alarm","eventId":"phoneAlarm","sdkCode":"GB/T 28181-2022 AlarmMethod=1","aliases":[]}'),
  (2,'设备报警','2','{"type":"device_alarm","eventId":"deviceAlarm","sdkCode":"GB/T 28181-2022 AlarmMethod=2","aliases":[]}'),
  (3,'短信报警','3','{"type":"sms_alarm","eventId":"smsAlarm","sdkCode":"GB/T 28181-2022 AlarmMethod=3","aliases":[]}'),
  (4,'GPS报警','4','{"type":"gps_alarm","eventId":"gpsAlarm","sdkCode":"GB/T 28181-2022 AlarmMethod=4","aliases":[]}'),
  (5,'其他报警','7','{"type":"other_alarm","eventId":"otherAlarm","sdkCode":"GB/T 28181-2022 AlarmMethod=7","aliases":[]}'),
  (6,'视频丢失','2:1','{"type":"video_lost","eventId":"videoLoss","sdkCode":"GB/T 28181-2022 AlarmMethod=2; AlarmType=1","aliases":[]}'),
  (7,'设备防拆','2:2','{"type":"device_tamper","eventId":"deviceTamper","sdkCode":"GB/T 28181-2022 AlarmMethod=2; AlarmType=2","aliases":[]}'),
  (8,'硬盘满','2:3','{"type":"disk_full","eventId":"diskFull","sdkCode":"GB/T 28181-2022 AlarmMethod=2; AlarmType=3","aliases":[]}'),
  (9,'设备高温','2:4','{"type":"device_over_temperature","eventId":"deviceOverTemperature","sdkCode":"GB/T 28181-2022 AlarmMethod=2; AlarmType=4","aliases":[]}'),
  (10,'设备低温','2:5','{"type":"device_under_temperature","eventId":"deviceUnderTemperature","sdkCode":"GB/T 28181-2022 AlarmMethod=2; AlarmType=5","aliases":[]}'),
  (11,'人工视频报警','5:1','{"type":"manual_video_alarm","eventId":"manualVideoAlarm","sdkCode":"GB/T 28181-2022 AlarmMethod=5; AlarmType=1","aliases":[]}'),
  (12,'移动侦测','5:2','{"type":"motion","eventId":"motionDetection","sdkCode":"GB/T 28181-2022 AlarmMethod=5; AlarmType=2","aliases":[]}'),
  (13,'遗留物检测','5:3','{"type":"object_left","eventId":"objectLeft","sdkCode":"GB/T 28181-2022 AlarmMethod=5; AlarmType=3","aliases":[]}'),
  (14,'物体移除','5:4','{"type":"object_removed","eventId":"objectRemoved","sdkCode":"GB/T 28181-2022 AlarmMethod=5; AlarmType=4","aliases":[]}'),
  (15,'绊线检测','5:5','{"type":"line_crossing","eventId":"lineCrossing","sdkCode":"GB/T 28181-2022 AlarmMethod=5; AlarmType=5","aliases":[]}'),
  (16,'区域入侵','5:6','{"type":"intrusion","eventId":"regionIntrusion","sdkCode":"GB/T 28181-2022 AlarmMethod=5; AlarmType=6","aliases":[]}'),
  (17,'逆行检测','5:7','{"type":"reverse_direction","eventId":"reverseDirection","sdkCode":"GB/T 28181-2022 AlarmMethod=5; AlarmType=7","aliases":[]}'),
  (18,'徘徊检测','5:8','{"type":"loitering","eventId":"loitering","sdkCode":"GB/T 28181-2022 AlarmMethod=5; AlarmType=8","aliases":[]}'),
  (19,'流量统计','5:9','{"type":"traffic_statistics","eventId":"trafficStatistics","sdkCode":"GB/T 28181-2022 AlarmMethod=5; AlarmType=9","aliases":[]}'),
  (20,'密度检测','5:10','{"type":"density_detection","eventId":"densityDetection","sdkCode":"GB/T 28181-2022 AlarmMethod=5; AlarmType=10","aliases":[]}'),
  (21,'视频异常','5:11','{"type":"video_exception","eventId":"videoException","sdkCode":"GB/T 28181-2022 AlarmMethod=5; AlarmType=11","aliases":[]}'),
  (22,'快速移动','5:12','{"type":"fast_movement","eventId":"fastMovement","sdkCode":"GB/T 28181-2022 AlarmMethod=5; AlarmType=12","aliases":[]}'),
  (23,'视频遮挡','5:13','{"type":"shelter","eventId":"videoTamper","sdkCode":"GB/T 28181-2022 AlarmMethod=5; AlarmType=13","aliases":[]}'),
  (24,'硬盘故障','6:1','{"type":"disk_error","eventId":"diskError","sdkCode":"GB/T 28181-2022 AlarmMethod=6; AlarmType=1","aliases":[]}'),
  (25,'风扇故障','6:2','{"type":"fan_error","eventId":"fanError","sdkCode":"GB/T 28181-2022 AlarmMethod=6; AlarmType=2","aliases":[]}');

DROP PROCEDURE IF EXISTS nexiot_wvp_events_2026101601;
DELIMITER $$
CREATE PROCEDURE nexiot_wvp_events_2026101601()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    START TRANSACTION;
INSERT INTO sys_dict_type (dict_id,dict_name,dict_type,status,create_by,create_time,remark)
SELECT ids.base + 1, 'GB28181国标告警事件', 'video_sdk_event_wvp', '0',
       'migration', CURRENT_TIMESTAMP, 'GB/T 28181-2022 A.2.5.3；字典值为十进制 AlarmMethod[:AlarmType]；备注JSON定义 type、eventId、sdkCode'
FROM (SELECT COALESCE(MAX(dict_id),0) AS base FROM sys_dict_type) ids
WHERE NOT EXISTS (SELECT 1 FROM (SELECT dict_type FROM sys_dict_type GROUP BY dict_type) existing WHERE dict_type='video_sdk_event_wvp');

INSERT INTO sys_dict_data
  (dict_code,dict_sort,dict_label,dict_value,dict_type,is_default,status,create_by,create_time,remark)
SELECT ids.base + ROW_NUMBER() OVER (ORDER BY s.sort), s.sort, s.label, s.value,
       'video_sdk_event_wvp', 'N', '0', 'migration', CURRENT_TIMESTAMP, s.config
FROM wvp_gb28181_event_seed s
CROSS JOIN (SELECT COALESCE(MAX(dict_code),0) AS base FROM sys_dict_data) ids
WHERE NOT EXISTS (SELECT 1 FROM (SELECT dict_type,dict_value FROM sys_dict_data GROUP BY dict_type,dict_value) d
                  WHERE d.dict_type='video_sdk_event_wvp' AND d.dict_value=s.value);

    IF (SELECT COUNT(*) FROM sys_dict_type WHERE dict_type='video_sdk_event_wvp') <> 1
       OR EXISTS (SELECT 1 FROM wvp_gb28181_event_seed s
                  WHERE (SELECT COUNT(*) FROM sys_dict_data d
                         WHERE d.dict_type='video_sdk_event_wvp' AND d.dict_value=s.value) <> 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incomplete or duplicate GB28181 event dictionary';
    END IF;
    COMMIT;
END$$
DELIMITER ;
CALL nexiot_wvp_events_2026101601();
DROP PROCEDURE nexiot_wvp_events_2026101601;
DROP TEMPORARY TABLE wvp_gb28181_event_seed;
