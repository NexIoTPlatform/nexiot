-- Initialize device-side verification fields for existing video-cloud products that have no custom fields.
-- Product create/update also injects and normalizes these fields in VideoCloudSystemFieldInjector.
UPDATE iot_product
SET third_configuration = '{"customField":[{"id":"validateCode","name":"设备验证码","description":"萤石设备标签上的验证码，首次绑定设备时使用","type":"password","required":true,"placeholder":"请输入设备验证码","options":[],"disabled":false,"hidden":false,"system":true,"autoGenerate":false}]}'
WHERE LOWER(third_platform) = 'ezviz'
  AND (
    third_configuration IS NULL
    OR TRIM(third_configuration) IN ('', '{}', '{"customField":[]}', '{"customField": []}')
  );

UPDATE iot_product
SET third_configuration = '{"customField":[{"id":"appKey","name":"设备安全码","description":"乐橙设备标签上的安全码，首次绑定设备时使用","type":"password","required":true,"placeholder":"请输入设备安全码","options":[],"disabled":false,"hidden":false,"system":true,"autoGenerate":false}]}'
WHERE LOWER(third_platform) = 'imoulife'
  AND (
    third_configuration IS NULL
    OR TRIM(third_configuration) IN ('', '{}', '{"customField":[]}', '{"customField": []}')
  );
