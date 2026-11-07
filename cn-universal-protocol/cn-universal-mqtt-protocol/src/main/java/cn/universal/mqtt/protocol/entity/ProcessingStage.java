package cn.universal.mqtt.protocol.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/** 处理阶段枚举 */
@Schema(description = "处理阶段枚举")
public enum ProcessingStage {
  RECEIVED, // 已接收
  TOPIC_PARSED, // 主题已解析
  DEVICE_EXTRACTED, // 设备已提取
  DECODED, // 解码完成
  VALIDATED, // 验证完成
  BUSINESS_PROCESSED, // 业务处理完成
  PUBLISH_PROCESSED, // 发布处理完成
  PROCESSED, // 处理完成
  COMPLETED, // 处理完成
  ERROR // 处理错误
}
