package cn.universal.manager.notice.service.channel;

import cn.hutool.json.JSONObject;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SmsAliNoticeSendChannel extends AbstractNoticeSendChannel {

  @Override
  public boolean support(String type) {
    return "sms_ali".equalsIgnoreCase(type);
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
          configObj, "accessKey", "secretKey", "signName", "templateCode", "receivers")) {
        String errMsg = "配置不完整";
        logSend("SmsAli", content, receivers, false, errMsg);
        return NoticeSendResult.builder()
            .success(false)
            .receivers(receivers)
            .content(content)
            .errorMessage(errMsg)
            .build();
      }
      Config aliConfig =
          new Config()
              .setAccessKeyId(configObj.getStr("accessKey"))
              .setAccessKeySecret(configObj.getStr("secretKey"))
              .setEndpoint("dysmsapi.aliyuncs.com");
      Client client = new Client(aliConfig);
      JSONObject templateParam = new JSONObject();
      if (params != null) {
        params.forEach(templateParam::set);
      } else {
        templateParam.set("message", content);
      }
      SendSmsRequest request =
          new SendSmsRequest()
              .setPhoneNumbers(receivers)
              .setSignName(configObj.getStr("signName"))
              .setTemplateCode(configObj.getStr("templateCode"))
              .setTemplateParam(templateParam.toString());
      SendSmsResponse response = client.sendSms(request);
      if ("OK".equals(response.getBody().getCode())) {
        logSend("SmsAli", content, receivers, true, "发送成功");
        return NoticeSendResult.builder()
            .success(true)
            .receivers(receivers)
            .content(content)
            .errorMessage(null)
            .build();
      } else {
        String errMsg = "阿里云返回错误: " + response.getBody().getMessage();
        logSend("SmsAli", content, receivers, false, errMsg);
        return NoticeSendResult.builder()
            .success(false)
            .receivers(receivers)
            .content(content)
            .errorMessage(errMsg)
            .build();
      }
    } catch (Exception e) {
      String errMsg = "发送异常: " + e.getMessage();
      logSend("SmsAli", content, receivers, false, errMsg);
      logger.error("阿里云短信发送异常", e);
      return NoticeSendResult.builder()
          .success(false)
          .receivers(receivers)
          .content(content)
          .errorMessage(errMsg)
          .build();
    }
  }
}
