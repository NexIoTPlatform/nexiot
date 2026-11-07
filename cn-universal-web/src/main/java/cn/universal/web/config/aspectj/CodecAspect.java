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

package cn.universal.web.config.aspectj;

import cn.universal.admin.system.service.impl.IoTDeviceMessageCodecService;
import cn.universal.common.exception.CodeKeyException;
import cn.universal.persistence.codec.CodecParam;
import cn.universal.web.config.annotation.CodeBody;
import cn.universal.web.config.annotation.CodeKey;
import cn.universal.web.config.annotation.Codec;
import jakarta.annotation.Resource;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;

/** 编解码切面 @Author gitee.com/NexIoT */
@Aspect
@Component
@Slf4j
public class CodecAspect {

  @Resource private IoTDeviceMessageCodecService ioTDeviceMessageCodecService;

  /** 设置切入点 */
  @Pointcut("@annotation(cn.universal.web.config.annotation.Codec)")
  public void codecCut() {}

  /**
   * 获取方法参数中的唯一key和要转码对象
   *
   * @param joinPoint
   * @return
   */
  private CodecParam getParam(JoinPoint joinPoint) {
    Object[] params = joinPoint.getArgs();
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    String[] parameterNames = signature.getParameterNames();
    Parameter[] parameters = method.getParameters();
    Codec codeC = method.getAnnotation(Codec.class);
    Object codeBody = null;
    String codeKey = codeC.codeKey();
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      if (parameter.getAnnotation(CodeBody.class) != null
          || parameter.getAnnotation(RequestBody.class) != null) {
        codeBody = params[i];
      }
      if (parameter.getAnnotation(CodeKey.class) != null || "codeKey".equals(parameterNames[i])) {
        codeKey = params[i].toString();
      }
    }
    if (StringUtils.isEmpty(codeKey)) {
      throw new CodeKeyException("codeKey is not found");
    }
    if (Objects.isNull(codeBody)) {
      throw new CodeKeyException("codeBody is not found");
    }

    return new CodecParam(codeKey, codeBody);
  }

  /**
   * 将经过编解码转换后的新值放入到方法的参数中
   *
   * @param joinPoint
   * @param newValue
   * @return
   */
  private Object[] changeCodeBodyValue(JoinPoint joinPoint, Object newValue) {
    Object[] params = joinPoint.getArgs();
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    Parameter[] parameters = method.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      if (parameter.getAnnotation(CodeBody.class) != null
          || parameter.getAnnotation(RequestBody.class) != null) {
        params[i] = newValue;
      }
    }
    return params;
  }

  /**
   * 编解码切面
   *
   * @param joinPoint
   * @return
   * @throws Throwable
   */
  @Around("codecCut()")
  public Object aroundMethod(ProceedingJoinPoint joinPoint) throws Throwable {
    Object[] params = joinPoint.getArgs();
    if (params.length == 0) {
      return joinPoint.proceed();
    }
    CodecParam param = getParam(joinPoint);
    Object newValue =
        ioTDeviceMessageCodecService.preDecode(param.getCodeKey(), param.getCodeBody());
    // 将转换后的值放入方法参数中
    Object[] methodParams = changeCodeBodyValue(joinPoint, newValue);
    Object result = joinPoint.proceed(methodParams);
    // 暂时不用
    //    Object encodeBody = ioTDeviceMessageCodecService.encode(param.getCodeKey(),
    // JSONUtil.toJsonStr(result));
    return result;
  }
}
