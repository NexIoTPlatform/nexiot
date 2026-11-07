package cn.universal.manager.notice.service.channel;

import cn.hutool.json.JSONObject;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SmsTencentNoticeSendChannel extends AbstractNoticeSendChannel {

  @Override
  public boolean support(String type) {
    return "sms_tencent".equalsIgnoreCase(type);
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
        logSend("SmsTencent", content, receivers, false, errMsg);
        return NoticeSendResult.builder()
            .success(false)
            .receivers(receivers)
            .content(content)
            .errorMessage(errMsg)
            .build();
      }
      Credential cred = new Credential(configObj.getStr("secretId"), configObj.getStr("secretKey"));
      SmsClient client = new SmsClient(cred, "ap-guangzhou");
      String[] templateParams;
      if (params != null && !params.isEmpty()) {
        templateParams = params.values().stream().map(Object::toString).toArray(String[]::new);
      } else {
        templateParams = new String[] {content};
      }
      SendSmsRequest request = new SendSmsRequest();
      request.setSmsSdkAppId("1400000000"); // 需要配置实际的SDK AppId
      request.setSignName(configObj.getStr("signName"));
      request.setTemplateId(configObj.getStr("templateCode"));
      request.setTemplateParamSet(templateParams);
      request.setPhoneNumberSet(new String[] {receivers});
      SendSmsResponse response = client.SendSms(request);
      if (response.getSendStatusSet() != null && response.getSendStatusSet().length > 0) {
        String status = response.getSendStatusSet()[0].getCode();
        if ("Ok".equals(status)) {
          logSend("SmsTencent", content, receivers, true, "发送成功");
          return NoticeSendResult.builder()
              .success(true)
              .receivers(receivers)
              .content(content)
              .errorMessage(null)
              .build();
        } else {
          String errMsg = "腾讯云返回错误: " + response.getSendStatusSet()[0].getMessage();
          logSend("SmsTencent", content, receivers, false, errMsg);
          return NoticeSendResult.builder()
              .success(false)
              .receivers(receivers)
              .content(content)
              .errorMessage(errMsg)
              .build();
        }
      } else {
        String errMsg = "腾讯云返回空响应";
        logSend("SmsTencent", content, receivers, false, errMsg);
        return NoticeSendResult.builder()
            .success(false)
            .receivers(receivers)
            .content(content)
            .errorMessage(errMsg)
            .build();
      }
    } catch (TencentCloudSDKException e) {
      String errMsg = "腾讯云SDK异常: " + e.getMessage();
      logSend("SmsTencent", content, receivers, false, errMsg);
      logger.error("腾讯云短信发送异常", e);
      return NoticeSendResult.builder()
          .success(false)
          .receivers(receivers)
          .content(content)
          .errorMessage(errMsg)
          .build();
    } catch (Exception e) {
      String errMsg = "发送异常: " + e.getMessage();
      logSend("SmsTencent", content, receivers, false, errMsg);
      logger.error("腾讯云短信发送异常", e);
      return NoticeSendResult.builder()
          .success(false)
          .receivers(receivers)
          .content(content)
          .errorMessage(errMsg)
          .build();
    }
  }
}
