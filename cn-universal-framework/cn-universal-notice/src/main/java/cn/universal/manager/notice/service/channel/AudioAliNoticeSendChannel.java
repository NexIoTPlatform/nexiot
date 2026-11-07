package cn.universal.manager.notice.service.channel;

import cn.hutool.json.JSONObject;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AudioAliNoticeSendChannel extends AbstractNoticeSendChannel {

  @Override
  public boolean support(String type) {
    return "audio_ali".equalsIgnoreCase(type);
  }

  @Override
  public NoticeSendResult send(
      String content,
      String receivers,
      cn.universal.manager.notice.model.NoticeChannel config,
      Map<String, Object> params) {
    try {
      JSONObject configObj = parseConfig(config);
      if (!validateConfig(configObj, "accessKey", "secretKey", "signName", "templateCode")) {
        String errMsg = "配置不完整";
        logSend("AudioAli", content, receivers, false, errMsg);
        return NoticeSendResult.builder()
            .success(false)
            .receivers(receivers)
            .content(content)
            .errorMessage(errMsg)
            .build();
      }
      // TODO: 实现阿里云语音API调用
      logger.info("阿里云语音发送 - 接收者: {}, 内容: {}, 配置: {}", receivers, content, configObj.toString());
      logSend("AudioAli", content, receivers, true, "发送成功（模拟）");
      return NoticeSendResult.builder()
          .success(true)
          .receivers(receivers)
          .content(content)
          .errorMessage(null)
          .build();
    } catch (Exception e) {
      String errMsg = "发送异常: " + e.getMessage();
      logSend("AudioAli", content, receivers, false, errMsg);
      logger.error("阿里云语音发送异常", e);
      return NoticeSendResult.builder()
          .success(false)
          .receivers(receivers)
          .content(content)
          .errorMessage(errMsg)
          .build();
    }
  }
}
