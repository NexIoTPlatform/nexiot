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

package cn.universal.web.context;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.system.service.impl.UserApplicationService;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.exception.IoTException;
import cn.universal.common.utils.AESOperator;
import cn.universal.common.utils.AuthUtil;
import cn.universal.persistence.entity.IoTUserApplication;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/30
 */
@Slf4j
@Component
public class IoTCustomAuthContext {

  @Resource private UserApplicationService userApplicationService;

  public JSONObject decryptMessage(String encryptBody, HttpServletRequest request) {

    String appId = request.getHeader(IoTConstant.HTTP_HEADER_APP);
    String time = request.getHeader(IoTConstant.HTTP_HEADER_TIME);
    String auth = request.getHeader(IoTConstant.HTTP_HEADER_AUTH);
    if (StrUtil.isBlank(appId)) {
      throw new IoTException("请求头[" + IoTConstant.HTTP_HEADER_APP + "] 不能为空", -1);
    }
    if (StrUtil.isBlank(time)) {
      throw new IoTException("请求头[" + IoTConstant.HTTP_HEADER_TIME + "] 不能为空", -1);
    }
    if (StrUtil.isBlank(time)) {
      throw new IoTException("请求头[" + IoTConstant.HTTP_HEADER_AUTH + "] 不能为空", -1);
    }
    IoTUserApplication iotUserApplication = userApplicationService.getUserAppByAppid(appId);

    ZonedDateTime temporalAccessor = ZonedDateTime.from(AuthUtil.parseZ8DateTime(time));
    ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneId.of(IoTConstant.HTTP_UTC_8));
    if (Duration.between(now, temporalAccessor).abs().getSeconds()
        > IoTConstant.HTTP_AUTH_TIMEOUT) {
      log.info(
          "app=[{}] IP=[{}] 验证已过期", iotUserApplication.getUnionId(), IPUtils.getIpAddr(request));
      throw new IoTException("auth expire", 10000);
    }

    String hashValue = AuthUtil.hash(time, iotUserApplication.getAppId(), encryptBody);
    if (!Objects.equals(auth, hashValue)) {
      log.info(
          "app【{}】from {},auth：{},curAuth：{} 验证失败",
          iotUserApplication.getUnionId(),
          IPUtils.getIpAddr(request),
          auth,
          hashValue);
      throw new IoTException("auth failed", 10000);
    }

    if (!encryptBody.startsWith("{") || !encryptBody.endsWith("}")) {
      encryptBody =
          AESOperator.getInstance()
              .decrypt(
                  encryptBody.trim(),
                  iotUserApplication.getAppId(),
                  iotUserApplication.getAppSecret());
      if (encryptBody == null) {
        log.info("app【{}】from {} 解密错误", iotUserApplication.getAppId(), IPUtils.getIpAddr(request));
        throw new IoTException("decrypt msg failed", 10000);
      }
    }
    JSONObject decode = JSONUtil.parseObj(encryptBody);
    // 设置appUnionId
    decode.set("appUnionId", iotUserApplication.getUnionId());
    decode.set("applicationId", iotUserApplication.getAppUniqueId());
    return decode;
  }
}
