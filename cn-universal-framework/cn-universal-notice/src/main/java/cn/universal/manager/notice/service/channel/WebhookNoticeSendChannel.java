package cn.universal.manager.notice.service.channel;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.manager.notice.model.NoticeChannel;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WebhookNoticeSendChannel extends AbstractNoticeSendChannel {

  @Override
  public boolean support(String type) {
    return "webhook".equalsIgnoreCase(type);
  }

  @Override
  public NoticeSendResult send(
      String content, String receivers, NoticeChannel config, Map<String, Object> params) {
    if (StrUtil.isBlank(content)) {
      logSend("Webhook", content, receivers, false, "发送内容为空");
      return NoticeSendResult.builder()
          .success(false)
          .receivers(receivers)
          .content(content)
          .errorMessage("发送内容为空")
          .build();
    }
    JSONObject configObj = parseConfig(config);
    String webhookUrl = configObj.getStr("webhook");
    String authTokenKey = configObj.getStr("AuthTokenKey");
    String tokenValue = configObj.getStr("TokenValue");
    if (StrUtil.isBlank(webhookUrl)) {
      String errMsg = "Webhook地址未配置";
      logSend("Webhook", content, receivers, false, errMsg);
      return NoticeSendResult.builder()
          .success(false)
          .receivers(receivers)
          .content(content)
          .errorMessage(errMsg)
          .build();
    }
    try {
      HttpRequest httpRequest = HttpUtil.createPost(webhookUrl);
      httpRequest.timeout(5000); // 默认5秒超时
      httpRequest.header(Header.CONTENT_TYPE, "application/json");
      // 自定义header
      if (StrUtil.isNotBlank(tokenValue)) {
        httpRequest.header(
            StrUtil.isBlank(authTokenKey) ? Header.AUTHORIZATION.getValue() : authTokenKey,
            tokenValue);
      }
      // 发送内容
      httpRequest.body(content);
      HttpResponse response = httpRequest.execute();
      String result = response.body();
      int status = response.getStatus();
      logger.warn(
          "[Webhook推送] url={}, status={}, body={}, result={}", webhookUrl, status, content, result);
      if (status >= 200 && status < 300) {
        logSend("Webhook", content, receivers, true, "发送成功");
        return NoticeSendResult.builder()
            .success(true)
            .receivers(receivers)
            .content(content)
            .errorMessage(null)
            .build();
      } else {
        String errMsg = "Webhook响应码:" + status + ", response=" + result;
        logSend("Webhook", content, receivers, false, errMsg);
        return NoticeSendResult.builder()
            .success(false)
            .receivers(receivers)
            .content(content)
            .errorMessage(errMsg)
            .build();
      }
    } catch (Exception e) {
      String errMsg = "Webhook发送异常: " + e.getMessage();
      logSend("Webhook", content, receivers, false, errMsg);
      return NoticeSendResult.builder()
          .success(false)
          .receivers(receivers)
          .content(content)
          .errorMessage(errMsg)
          .build();
    }
  }

  private boolean isJson(String str) {
    try {
      JSONUtil.parse(str);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
