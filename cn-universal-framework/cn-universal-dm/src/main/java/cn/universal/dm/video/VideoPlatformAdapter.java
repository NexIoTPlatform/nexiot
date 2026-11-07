/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台适配器接口
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.dm.video;

import cn.universal.persistence.entity.VideoPlatformInstance;
import java.util.List;
import java.util.Map;

/** 视频平台适配器统一接口 用于屏蔽不同平台（WVP/HIK/ICC）的API差异 */
public interface VideoPlatformAdapter {

  /** 获取支持的平台类型 */
  String getSupportedPlatformType();

  /**
   * 测试平台连接
   *
   * @param instance 平台实例配置
   * @return 测试结果 {reachable: boolean, latencyMs: int, message: string}
   */
  Map<String, Object> testConnection(VideoPlatformInstance instance);

  /**
   * 实时拉取设备目录（不落库）
   *
   * @param instance 平台实例
   * @param filters 过滤条件 {deviceType, keyword, page, pageSize}
   * @return 设备列表，每个设备包含：extDeviceId, name, manufacturer, model, status, channels等
   */
  List<Map<String, Object>> listDevices(
      VideoPlatformInstance instance, Map<String, Object> filters);

  /**
   * 获取设备实时流地址
   *
   * @param instance 平台实例
   * @param extDeviceId 三方设备ID
   * @param channelId 通道ID（可选）
   * @param streamType 码流类型：main主码流/sub子码流
   * @return {rtsp, rtmp, flv, hls, webrtc}
   */
  Map<String, String> getStreamUrl(
      VideoPlatformInstance instance, String extDeviceId, String channelId, String streamType);

  /**
   * 获取设备实时流地址（支持平台特有参数）
   *
   * @param options 平台特有参数，例如海康的protocol、transmode等
   */
  default Map<String, String> getStreamUrl(
      VideoPlatformInstance instance,
      String extDeviceId,
      String channelId,
      String streamType,
      Map<String, Object> options) {
    return getStreamUrl(instance, extDeviceId, channelId, streamType);
  }

  /**
   * 获取设备回放流地址
   *
   * @param instance 平台实例
   * @param extDeviceId 三方设备ID
   * @param channelId 通道ID
   * @param startTime 开始时间（时间戳，毫秒）
   * @param endTime 结束时间（时间戳，毫秒）
   * @return {flv, ws_flv, wss_flv, hls, rtmp, rtsp, app, stream, mediaServerId, key, ...}
   */
  Map<String, Object> getPlaybackUrl(
      VideoPlatformInstance instance,
      String extDeviceId,
      String channelId,
      long startTime,
      long endTime);

  /**
   * 获取设备回放流地址（支持平台特有参数）
   *
   * @param options 平台特有参数，例如海康的recordLocation、大华的recordSource等
   */
  default Map<String, Object> getPlaybackUrl(
      VideoPlatformInstance instance,
      String extDeviceId,
      String channelId,
      long startTime,
      long endTime,
      Map<String, Object> options) {
    return getPlaybackUrl(instance, extDeviceId, channelId, startTime, endTime);
  }

  /** 控制已建立的设备录像会话；不支持的平台必须明确拒绝。 */
  default void controlPlayback(
      VideoPlatformInstance instance, String deviceId, String channelId,
      String stream, String command, Double speed) {
    throw new UnsupportedOperationException("当前平台不支持设备录像控制");
  }

  /**
   * PTZ控制
   *
   * @param instance 平台实例
   * @param extDeviceId 三方设备ID
   * @param channelId 通道ID
   * @param command PTZ指令：up/down/left/right/zoomIn/zoomOut/stop
   * @param speed 速度：1-255
   */
  void controlPTZ(
      VideoPlatformInstance instance,
      String extDeviceId,
      String channelId,
      String command,
      int speed);

  /**
   * 查询录像记录
   *
   * @param instance 平台实例
   * @param extDeviceId 三方设备ID
   * @param channelId 通道ID
   * @param startTime 开始时间戳（秒）
   * @param endTime 结束时间戳（秒）
   * @return 录像片段列表 [{startTime, endTime, fileSize, fileUrl}]
   */
  List<Map<String, Object>> queryRecords(
      VideoPlatformInstance instance,
      String extDeviceId,
      String channelId,
      long startTime,
      long endTime);

  /**
   * 查询录像记录（支持平台特有参数）
   *
   * @param options 平台特有参数，例如海康的recordLocation、大华的recordSource等
   */
  default List<Map<String, Object>> queryRecords(
      VideoPlatformInstance instance,
      String extDeviceId,
      String channelId,
      long startTime,
      long endTime,
      Map<String, Object> options) {
    return queryRecords(instance, extDeviceId, channelId, startTime, endTime);
  }

  /**
   * 启动融合任务
   *
   * @param instance 平台实例
   * @param extDeviceId 三方设备ID
   * @param channelId 通道ID
   * @param options 平台特有融合参数
   * @return 融合任务状态数据
   */
  default Map<String, Object> startFusionTask(
      VideoPlatformInstance instance,
      String extDeviceId,
      String channelId,
      Map<String, Object> options) {
    throw new UnsupportedOperationException(
        getSupportedPlatformType() + " 平台未实现 startFusionTask");
  }

  /**
   * 刷新融合任务
   *
   * @param instance 平台实例
   * @param extDeviceId 三方设备ID
   * @param channelId 通道ID
   * @param options 平台特有融合参数
   * @return 融合任务状态数据
   */
  default Map<String, Object> refreshFusionTask(
      VideoPlatformInstance instance,
      String extDeviceId,
      String channelId,
      Map<String, Object> options) {
    throw new UnsupportedOperationException(
        getSupportedPlatformType() + " 平台未实现 refreshFusionTask");
  }

  /**
   * 停止融合任务
   *
   * @param instance 平台实例
   * @param extDeviceId 三方设备ID
   * @param channelId 通道ID
   * @return 融合任务状态数据
   */
  default Map<String, Object> stopFusionTask(
      VideoPlatformInstance instance, String extDeviceId, String channelId) {
    throw new UnsupportedOperationException(
        getSupportedPlatformType() + " 平台未实现 stopFusionTask");
  }

  /**
   * 查询融合任务状态
   *
   * @param instance 平台实例
   * @param extDeviceId 三方设备ID
   * @param channelId 通道ID
   * @return 融合任务状态数据
   */
  default Map<String, Object> getFusionTaskStatus(
      VideoPlatformInstance instance, String extDeviceId, String channelId) {
    throw new UnsupportedOperationException(
        getSupportedPlatformType() + " 平台未实现 getFusionTaskStatus");
  }

  /**
   * 设备抓图。
   *
   * <p>默认通过平台特有操作 {@code snapshot} 承载，便于 SDK/ONVIF/WVP 等平台逐步收敛为统一能力。
   * 适配器可覆盖本方法补充设备、通道上下文或 OSS 归档逻辑。
   *
   * @param instance 平台实例
   * @param extDeviceId 三方设备ID
   * @param channelId 通道ID
   * @param options 平台特有抓图参数
   * @return {snapshotUrl, ossUrl, ossId, contentType, ...}
   */
  default Map<String, Object> snapshot(
      VideoPlatformInstance instance,
      String extDeviceId,
      String channelId,
      Map<String, Object> options) {
    Map<String, Object> payload =
        options == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(options);
    if (extDeviceId != null && !payload.containsKey("deviceId")) {
      payload.put("deviceId", extDeviceId);
    }
    if (extDeviceId != null && !payload.containsKey("extDeviceId")) {
      payload.put("extDeviceId", extDeviceId);
    }
    if (channelId != null && !payload.containsKey("channelId")) {
      payload.put("channelId", channelId);
    }
    return executeOperation(instance, "snapshot", payload);
  }

  /**
   * 获取组织树（按需实现）
   *
   * @param instance 平台实例
   * @param filters 过滤条件 {keyword}
   * @return 组织节点列表 [{id, name, parentId, path}]
   */
  List<Map<String, Object>> listOrganizations(
      VideoPlatformInstance instance, Map<String, Object> filters);

  /**
   * 订阅平台事件
   *
   * @param instance 平台实例
   * @param topics 订阅事件类型集合
   * @param callback 回调地址或处理器标识
   */
  void subscribe(VideoPlatformInstance instance, java.util.List<String> topics, String callback);

  /** Internal SDK access context, including credentials. Must never be returned directly by a controller. */
  default List<Map<String, Object>> resolveSubscriptionDevices(VideoPlatformInstance instance) {
    return List.of();
  }


  /**
   * 取消订阅
   *
   * @param instance 平台实例
   * @param topics 取消的事件类型集合（为空则取消全部）
   */
  void unsubscribe(VideoPlatformInstance instance, java.util.List<String> topics);

  /**
   * 执行平台特有操作。
   *
   * <p>用于承载 ONVIF 发现、校时、重启、预置点等无法抽象进通用视频接口的能力；调用方仍通过
   * VideoPlatformInstanceService 做实例权限与路由，避免为每类平台暴露旁路接口。
   *
   * @param instance 平台实例
   * @param operation 操作标识
   * @param params 操作参数
   * @return 操作结果
   */
  default Map<String, Object> executeOperation(
      VideoPlatformInstance instance, String operation, Map<String, Object> params) {
    throw new UnsupportedOperationException(
        getSupportedPlatformType() + " 平台未实现操作: " + operation);
  }
}
