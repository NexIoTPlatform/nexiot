-- 宇视原生 NETDEVSDK 预览已接通；未配置取流策略时默认 SDK 直出。
-- 仅更新前一版本的内置提示，保留用户自定义字段及已有实例策略。
UPDATE sys_dict_data
SET remark = '{"type":"select","required":false,"options":["rtsp_first","sdk_fallback","sdk_only"],"placeholder":"默认 sdk_only"}'
WHERE dict_type = 'video_uniview_sdk_fields' AND dict_value = 'streamMode'
  AND remark = '{"type":"select","required":false,"options":["rtsp_first","sdk_fallback","sdk_only"],"placeholder":"默认 rtsp_first"}';
