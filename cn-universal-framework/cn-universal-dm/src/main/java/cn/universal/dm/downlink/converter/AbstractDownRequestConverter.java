/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权,未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.dm.downlink.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.downlink.converter.DownRequestConverter;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.persistence.base.BaseDownRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import lombok.extern.slf4j.Slf4j;

/**
 * 下行请求转换器抽象基类 提供通用的转换逻辑，减少各协议转换器的重复代码
 *
 * <p>本类作为可选的辅助基类，协议转换器可以选择：
 *
 * <ul>
 *   <li>继承此类 - 享受通用逻辑复用
 *   <li>直接实现{@link DownRequestConverter}接口 - 完全自定义
 * </ul>
 *
 * <p>通用功能：
 *
 * <ul>
 *   <li>复制通用字段（productKey、deviceId、cmd等）
 *   <li>加载产品配置（IoTProduct）
 *   <li>加载设备信息（IoTDeviceDTO）
 *   <li>设置上下文信息
 * </ul>
 *
 * <p>子类只需实现：
 *
 * <ul>
 *   <li>{@link #createRequest()} - 创建协议特定的请求对象
 *   <li>{@link #supportedProtocol()} - 返回支持的协议代码
 *   <li>{@link #fillProtocolSpecificFields(BaseDownRequest, UnifiedDownlinkCommand,
 *       DownlinkContext)} - 填充协议特定字段（可选）
 * </ul>
 *
 * @param <T> 协议特定的DownRequest类型
 * @version 1.0
 * @since 2025/10/25
 */
@Slf4j
public abstract class AbstractDownRequestConverter<T extends BaseDownRequest>
    extends AbstratIoTService implements DownRequestConverter<T> {

  /**
   * 转换统一命令为协议特定请求
   *
   * <p>执行流程：
   *
   * <ol>
   *   <li>创建请求对象
   *   <li>复制通用字段
   *   <li>加载产品配置
   *   <li>加载设备信息
   *   <li>填充协议特定字段
   *   <li>设置上下文
   * </ol>
   *
   * @param command 统一下行命令
   * @param context 下行上下文
   * @return 协议特定的请求对象
   */
  @Override
  public T convert(UnifiedDownlinkCommand command, DownlinkContext<?> context) {
    // 1. 创建请求对象
    T request = createRequest();

    // 2. 复制通用字段
    copyCommonFields(request, command);

    // 3. 加载产品配置
    loadProduct(request, command, context);

    // 4. 加载设备信息
    loadDevice(request, command, context);

    // 5. 填充协议特定字段
    fillProtocolSpecificFields(request, command, context);

    // 6. 设置上下文
    updateContext(request, command, context);

    return request;
  }

  /**
   * 创建协议特定的请求对象
   *
   * <p>子类必须实现此方法，返回对应协议的Request对象
   *
   * @return 协议特定的请求对象
   */
  protected abstract T createRequest();

  /**
   * 复制通用字段
   *
   * <p>将UnifiedDownlinkCommand中的通用字段复制到BaseDownRequest
   *
   * @param request 目标请求对象
   * @param command 源命令对象
   */
  protected void copyCommonFields(T request, UnifiedDownlinkCommand command) {
    // 使用BeanUtil复制同名字段
    BeanUtil.copyProperties(command, request);

    // 设置data字段（扩展参数）
    if (command.getExtensions() != null && !command.getExtensions().isEmpty()) {
      request.setData(new cn.hutool.json.JSONObject(command.getExtensions()));
    }

    // 设置params字段（兼容旧格式）
    if (command.getFunction() != null) {
      request.setParams(command.getFunction());
    }
  }

  /**
   * 加载产品配置
   *
   * <p>从数据库加载产品信息并设置到请求对象中
   *
   * @param request 请求对象
   * @param command 命令对象
   * @param context 上下文
   */
  protected void loadProduct(
      T request, UnifiedDownlinkCommand command, DownlinkContext<?> context) {
    String productKey = command.getProductKey();
    if (StrUtil.isNotBlank(productKey)) {
      try {
        IoTProduct product = getProduct(productKey);
        request.setIoTProduct(product);
        context.setAttribute("product", product);
      } catch (Exception e) {
        log.warn("加载产品配置失败: productKey={}, error={}", productKey, e.getMessage());
      }
    }
  }

  /**
   * 加载设备信息
   *
   * <p>从数据库加载设备信息并设置到请求对象中
   *
   * @param request 请求对象
   * @param command 命令对象
   * @param context 上下文
   */
  protected void loadDevice(T request, UnifiedDownlinkCommand command, DownlinkContext<?> context) {
    String productKey = command.getProductKey();
    String deviceId = command.getDeviceId();

    if (StrUtil.isNotBlank(productKey) && StrUtil.isNotBlank(deviceId)) {
      try {
        cn.universal.persistence.query.IoTDeviceQuery query =
            new cn.universal.persistence.query.IoTDeviceQuery();
        query.setProductKey(productKey);
        query.setDeviceId(deviceId);
        IoTDeviceDTO device = getIoTDeviceDTO(query);
        if (device != null) {
          request.setIoTDeviceDTO(device);
          context.setAttribute("device", device);
        }
      } catch (Exception e) {
        log.debug("加载设备信息失败(可能是新增设备场景): pk={}, deviceId={}", productKey, deviceId);
      }
    }
  }

  /**
   * 填充协议特定字段
   *
   * <p>子类可以重写此方法，添加协议特定的转换逻辑
   *
   * <p>默认实现为空，不做任何处理
   *
   * @param request 请求对象
   * @param command 命令对象
   * @param context 上下文
   */
  protected void fillProtocolSpecificFields(
      T request, UnifiedDownlinkCommand command, DownlinkContext<?> context) {
    // 默认不做任何处理，子类可以重写
  }

  /**
   * 更新上下文信息
   *
   * <p>将转换过程中的关键信息记录到上下文中
   *
   * @param request 请求对象
   * @param command 命令对象
   * @param context 上下文
   */
  protected void updateContext(
      T request, UnifiedDownlinkCommand command, DownlinkContext<?> context) {
    context.setAttribute("requestType", request.getClass().getSimpleName());
    context.setAttribute("protocol", supportedProtocol());
  }

  /**
   * 转换前的预处理
   *
   * <p>默认实现：验证必填参数
   *
   * @param command 统一下行命令
   * @param context 下行上下文
   */
  @Override
  public void preConvert(UnifiedDownlinkCommand command, DownlinkContext<?> context) {
    // 验证必填参数
    command.validate();

    log.debug(
        "开始转换命令: protocol={}, cmd={}, productKey={}, deviceId={}",
        supportedProtocol(),
        command.getCmd(),
        command.getProductKey(),
        command.getDeviceId());
  }

  /**
   * 转换后的后处理
   *
   * <p>默认实现：记录日志
   *
   * @param command 统一下行命令
   * @param request 转换后的请求对象
   * @param context 下行上下文
   */
  @Override
  public void postConvert(UnifiedDownlinkCommand command, T request, DownlinkContext<?> context) {
    log.debug(
        "命令转换完成: protocol={}, requestType={}",
        supportedProtocol(),
        request.getClass().getSimpleName());
  }
}
