package cn.universal.manager.notice.service.channel;

import cn.hutool.json.JSONObject;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.tts.v20190823.TtsClient;
import com.tencentcloudapi.tts.v20190823.models.TextToVoiceRequest;
import com.tencentcloudapi.tts.v20190823.models.TextToVoiceResponse;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AudioTencentNoticeSendChannel extends AbstractNoticeSendChannel {

  @Override
  public boolean support(String type) {
    return "audio_tencent".equalsIgnoreCase(type);
  }

  @Override
  public NoticeSendResult send(
      String content,
      String receivers,
      cn.universal.manager.notice.model.NoticeChannel config,
      Map<String, Object> params) {
    try {
      JSONObject configObj = parseConfig(config);
      if (!validateConfig(
          configObj, "secretId", "secretKey", "signName", "templateCode", "receivers")) {
        String errMsg = "配置不完整";
        logSend("AudioTencent", content, receivers, false, errMsg);
        return NoticeSendResult.builder()
            .success(false)
            .receivers(receivers)
            .content(content)
            .errorMessage(errMsg)
            .build();
      }
      Credential cred = new Credential(configObj.getStr("secretId"), configObj.getStr("secretKey"));
      TtsClient client = new TtsClient(cred, "ap-guangzhou");
      String[] templateParams;
      if (params != null && !params.isEmpty()) {
        templateParams = params.values().stream().map(Object::toString).toArray(String[]::new);
      } else {
        templateParams = new String[] {content};
      }
      TextToVoiceRequest request = new TextToVoiceRequest();
      request.setText(content);
      request.setSessionId(String.valueOf(System.currentTimeMillis()));
      request.setModelType(1L); // 默认模型
      request.setVolume(0.0f); // 默认音量
      request.setSpeed(0.0f); // 默认语速
      request.setProjectId(0L); // 默认项目ID
      request.setSampleRate(16000L); // 采样率
      TextToVoiceResponse response = client.TextToVoice(request);
      if (response.getAudio() != null) {
        logSend("AudioTencent", content, receivers, true, "发送成功");
        return NoticeSendResult.builder()
            .success(true)
            .receivers(receivers)
            .content(content)
            .errorMessage(null)
            .build();
      } else {
        String errMsg = "腾讯云返回空响应";
        logSend("AudioTencent", content, receivers, false, errMsg);
        return NoticeSendResult.builder()
            .success(false)
            .receivers(receivers)
            .content(content)
            .errorMessage(errMsg)
            .build();
      }
    } catch (TencentCloudSDKException e) {
      String errMsg = "腾讯云SDK异常: " + e.getMessage();
      logSend("AudioTencent", content, receivers, false, errMsg);
      logger.error("腾讯云语音发送异常", e);
      return NoticeSendResult.builder()
          .success(false)
          .receivers(receivers)
          .content(content)
          .errorMessage(errMsg)
          .build();
    } catch (Exception e) {
      String errMsg = "发送异常: " + e.getMessage();
      logSend("AudioTencent", content, receivers, false, errMsg);
      logger.error("腾讯云语音发送异常", e);
      return NoticeSendResult.builder()
          .success(false)
          .receivers(receivers)
          .content(content)
          .errorMessage(errMsg)
          .build();
    }
  }
}
