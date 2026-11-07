/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.common.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DingTalkUtil {

  private static final String token = System.getenv("DINGTALK_WEBHOOK_URL");
  private static final String secret = System.getenv("DINGTALK_WEBHOOK_SECRET");
  //	@Value("${notice.dingTalk.sign}")
  private static final String mark = "platform Universal";
  private static Set<String> prodProfileActive =
      Stream.of("test", "test2", "prod").collect(Collectors.toSet());

  // 第三方调用失败钉钉通知
  public static void send(String msg) {
    if (StrUtil.isBlank(token) || StrUtil.isBlank(secret)) {
      return;
    }
    String actProfiles = SpringUtil.getActiveProfile();
    if (StrUtil.isBlank(actProfiles) || !prodProfileActive.contains(actProfiles)) {
      log.warn("非正式环境，中止发送钉钉告警,msg={}", msg);
      return;
    }
    try {
      String res = "";
      Long timestamp = System.currentTimeMillis();
      String stringToSign = timestamp + "\n" + secret;
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
      byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
      String sign = URLEncoder.encode(Base64.encode(signData), "UTF-8");
      String url = token + "&timestamp=" + timestamp + "&sign=" + sign;
      JSONObject jsonObject = new JSONObject();
      JSONObject text = new JSONObject();
      JSONObject at = new JSONObject();
      msg = mark + ":" + actProfiles + ":" + msg;
      jsonObject.set("msgtype", "text");
      text.set("content", msg);
      jsonObject.set("text", text);
      List<String> phone = new ArrayList<>();
      at.set("atMobiles", phone);
      jsonObject.set("at", at);
      String response = HttpUtil.post(url, JSONUtil.toJsonStr(jsonObject));
      JSONObject result = JSONUtil.parseObj(response);
      Integer errCode = result.getInt("errcode");
      if (errCode == 0) {
        res = "成功";
      } else {
        switch (errCode) {
          case -1:
            res = "系统繁忙";
            break;
          case 88:
            res = "鉴权异常";
            break;
          case 404:
            res = "请求的URI地址不存在";
            break;
          case 34015:
            res = "发送群会话消息失败";
            break;
          case 34016:
            res = "会话消息的内容超长";
            break;
          case 40001:
            res = "获取access_token时Secret错误，或者access_token无效";
            break;
          case 40002:
            res = "不合法的凭证类型";
            break;
          default:
            res = result.getStr("errmsg");
            break;
        }
      }
      if (!"".equals(res) && !"成功".equals(res)) {
        log.error("钉钉系统监测推送失败，原因{}", res);
      }
    } catch (Exception e) {
      log.error("钉钉调用异常:", e);
    }
  }
}
