package cn.universal.manager.notice.service.channel;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 抽象通知渠道基类 提供通用的配置解析和日志记录功能 */
public abstract class AbstractNoticeSendChannel implements NoticeSendChannel {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /** 从配置中解析JSON对象 */
  protected JSONObject parseConfig(cn.universal.manager.notice.model.NoticeChannel config) {
    if (config == null || config.getConfig() == null) {
      return new JSONObject();
    }
    try {
      return JSONUtil.parseObj(config.getConfig());
    } catch (Exception e) {
      logger.error("解析配置失败: {}", config.getConfig(), e);
      return new JSONObject();
    }
  }

  /** 记录发送日志 */
  protected void logSend(
      String channelType, String content, String receivers, boolean success, String message) {
    if (success) {
      logger.info("[{}] 发送成功 - 接收者: {}, 内容: {}", channelType, receivers, content);
    } else {
      logger.error("[{}] 发送失败 - 接收者: {}, 内容: {}, 错误: {}", channelType, receivers, content, message);
    }
  }

  /** 验证配置是否完整 */
  protected boolean validateConfig(JSONObject config, String... requiredFields) {
    for (String field : requiredFields) {
      if (!config.containsKey(field)
          || config.getStr(field) == null
          || config.getStr(field).trim().isEmpty()) {
        logger.error("配置缺少必需字段: {}", field);
        return false;
      }
    }
    return true;
  }

  /** 构建并记录发送结果 */
  protected NoticeSendResult buildAndLogResult(
      String channelType, String content, String receivers, boolean success, String errorMessage) {
    logSend(channelType, content, receivers, success, errorMessage);
    return NoticeSendResult.builder()
        .success(success)
        .receivers(receivers)
        .content(content)
        .errorMessage(errorMessage)
        .build();
  }
}
