-- 验证SQL数据是否正确导入
-- 请执行此脚本来检查字典数据是否正确

-- 1. 检查字典类型
SELECT dict_id,
       dict_name,
       dict_type,
       status
FROM sys_dict_type
WHERE dict_type LIKE 'databridge_%'
ORDER BY dict_type;

-- 2. 检查各资源类型的配置字段数量
SELECT dict_type,
       COUNT(*) as field_count
FROM sys_dict_data
WHERE dict_type LIKE 'databridge_%_fields'
GROUP BY dict_type
ORDER BY dict_type;

-- 3. 检查腾讯云IoT配置字段（重点检查）
SELECT dict_code,
       dict_sort,
       dict_label,
       dict_value,
       dict_type,
       remark
FROM sys_dict_data
WHERE dict_type = 'databridge_tencent_iot_fields'
ORDER BY dict_sort;

-- 4. 验证JSON格式是否正确
SELECT dict_value,
       remark,
       CASE
           WHEN JSON_VALID(remark) THEN 'Valid JSON'
           ELSE 'Invalid JSON'
           END as json_status
FROM sys_dict_data
WHERE dict_type = 'databridge_tencent_iot_fields'
  AND remark IS NOT NULL
  AND remark != ''
ORDER BY dict_sort;

-- 5. 检查是否有重复的dict_code
SELECT dict_code,
       COUNT(*) as count
FROM sys_dict_data
WHERE dict_type LIKE 'databridge_%'
GROUP BY dict_code
HAVING COUNT (*) > 1
ORDER BY dict_code;
