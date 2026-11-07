/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台实例服务接口
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.service;

import cn.universal.admin.video.sync.DeviceSyncHandler;
import cn.universal.admin.video.vo.VideoPlatformDeviceVO;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.query.VideoPlatformDeviceQuery;
import java.util.List;
import java.util.Map;

public interface VideoPlatformInstanceService {

  /** 查询所有平台实例 */
  List<VideoPlatformInstance> listAll();

  /** 根据平台类型查询 */
  List<VideoPlatformInstance> listByType(String platformType);

  /** 根据instanceKey查询 */
  VideoPlatformInstance getByInstanceKey(String instanceKey);

  /** 根据ID查询 */
  VideoPlatformInstance getById(Long id);

  /** 创建平台实例 */
  VideoPlatformInstance create(VideoPlatformInstance instance);

  /** 更新平台实例 */
  VideoPlatformInstance update(VideoPlatformInstance instance);

  /** 删除平台实例 */
  void delete(Long id);

  void delete(Long id, boolean force);

  /** 测试连接（调用适配器真实测试） */
  Map<String, Object> testConnection(String instanceKey);

  /** 获取设备列表 */
  List<VideoPlatformDeviceVO> listDevices(VideoPlatformDeviceQuery query);

  /** 强制全量同步设备缓存 */
  DeviceSyncHandler.DeviceSyncResult forceSyncDevices(String instanceKey);

  /** 按指定扫描/过滤参数同步设备缓存，主要用于 ONVIF 详情页网段扫描后入库。 */
  DeviceSyncHandler.DeviceSyncResult forceSyncDevices(
      String instanceKey, Map<String, Object> filters);

  /** 获取组织树（调用适配器，部分平台支持） */
  List<Map<String, Object>> listOrganizations(String instanceKey, Map<String, Object> filters);

  /** 应用订阅（根据实例配置） */
  void applySubscription(String instanceKey);

  /** 服务启动后恢复已启用平台实例的订阅。 */
  Map<String, Object> restoreEnabledSubscriptions();

  /** 取消订阅（根据实例配置） */
  void cancelSubscription(String instanceKey);

  /** 查询直连 SDK 告警订阅配置与网关运行态。 */
  Map<String, Object> getSdkSubscriptionStatus(String instanceKey);

  Map<String, Object> saveSdkSubscription(String instanceKey, Map<String, Object> config);

  Map<String, Object> retrySdkSubscription(String instanceKey, List<String> deviceIds);


  /** 获取设备通道列表（从中间表） */
  void updateChannelName(String instanceKey, String deviceId, String channelId, Map<String, Object> updates);

  List<Map<String, Object>> listDeviceChannels(String instanceKey, String deviceId);

  /** 执行平台特有操作，如 ONVIF 发现、重启、校时、预置点等。 */
  Map<String, Object> executePlatformOperation(
      String instanceKey, String operation, Map<String, Object> params);

  /** 启动ONVIF异步扫描任务。 */
  Map<String, Object> startOnvifScan(String instanceKey, Map<String, Object> params);

  /** 获取ONVIF异步扫描任务状态。 */
  Map<String, Object> getOnvifScanTask(String instanceKey, String taskId);

  /** 获取ONVIF最近一次扫描任务状态。 */
  Map<String, Object> getLatestOnvifScan(String instanceKey);

  /** 启动直连SDK异步发现任务。 */
  Map<String, Object> startSdkDiscovery(String instanceKey, Map<String, Object> params);

  /** 获取直连SDK发现任务状态。 */
  Map<String, Object> getSdkDiscoveryTask(String instanceKey, String taskId);

  /** 获取直连SDK最近一次发现任务状态。 */
  Map<String, Object> getLatestSdkDiscovery(String instanceKey);

  /** 验证单个直连SDK摄像头连接与通道。 */
  Map<String, Object> validateSdkDevice(String instanceKey, Map<String, Object> params);

  /** 导入直连SDK发现到的摄像头并同步通道。 */
  Map<String, Object> importSdkDevices(String instanceKey, Map<String, Object> body);

  /** 更新设备直连SDK默认预览ZLM节点。 */
  VideoPlatformInstance updateSdkDefaultMediaServer(
      String instanceKey, Map<String, Object> body);

  /**
   * 查询通道列表（从通道表）
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID（可选）
   * @param filters 筛选条件: keyword(通道名称), status(通道状态), ptzType(PTZ类型)
   */
  List<Map<String, Object>> listChannels(
      String instanceKey, String deviceId, Map<String, Object> filters);

  /** 重新探测并同步单个支持的平台设备的最新通道列表。 */
  Map<String, Object> syncDeviceChannels(String instanceKey, String deviceId);

  /** 获取通道融合配置；只读配置和当前运行态，不取流、不启动后端任务。 */
  Map<String, Object> getChannelFusion(
      String instanceKey, String deviceId, String channelId);

  /** 更新通道融合配置；service 会根据 fusionMode 清理互斥模式的运行态。 */
  Map<String, Object> updateChannelFusion(
      String instanceKey, String deviceId, String channelId, Map<String, Object> updates);

  /** 获取通道融合上下文 */
  default Map<String, Object> getChannelFusionContext(
      String instanceKey, String deviceId, String channelId) {
    return getChannelFusionContext(instanceKey, deviceId, channelId, true);
  }

  /** 获取通道融合上下文；includeOriginUrls=false 时不触发平台取流，适合前端仅判断模式。 */
  Map<String, Object> getChannelFusionContext(
      String instanceKey, String deviceId, String channelId, boolean includeOriginUrls);

  /** 同步获取后端融合任务状态；前端 OSD 模式应被实现层直接跳过。 */
  Map<String, Object> getChannelFusionTask(
      String instanceKey, String deviceId, String channelId);

  /** 启动后端融合任务；只允许 backend_fusion 模式调用。 */
  Map<String, Object> startChannelFusionTask(
      String instanceKey, String deviceId, String channelId);

  /** 刷新后端融合任务热数据；前端 OSD 通过快照/事件更新。 */
  Map<String, Object> refreshChannelFusionTask(
      String instanceKey, String deviceId, String channelId);

  /** 停止后端融合任务，并清理发布 URL 等运行态。 */
  Map<String, Object> stopChannelFusionTask(
      String instanceKey, String deviceId, String channelId);

  /** 获取通道前端 OSD 快照；只返回叠加文本和样式，不返回视频流。 */
  Map<String, Object> getChannelFusionOverlay(
      String instanceKey, String deviceId, String channelId);

  /** 创建通道前端 OSD 分享；分享页通常播放原始流并在浏览器叠加 OSD。 */
  Map<String, Object> createFrontendOsdShare(
      String instanceKey, String deviceId, String channelId, Map<String, Object> options);

  /** 读取前端 OSD 分享播放上下文 */
  Map<String, Object> getFrontendOsdShareContext(String token);

  /** 读取前端 OSD 分享叠加数据快照 */
  Map<String, Object> getFrontendOsdShareOverlay(String token);

  /** 撤销前端 OSD 分享 */
  void revokeFrontendOsdShare(String instanceKey, String deviceId, String channelId, String token);

  /** 获取通道的流地址（预览流） */
  Map<String, String> getStreamUrl(
      String instanceKey, String deviceId, String channelId, String streamType);

  /** 获取通道的流地址（支持平台特有参数） */
  Map<String, String> getStreamUrl(
      String instanceKey,
      String deviceId,
      String channelId,
      String streamType,
      Map<String, String> options);

  /** 控制当前通道已建立的录像会话。 */
  void controlPlayback(String instanceKey, String deviceId, String channelId,
      String stream, String command, Double speed);

  /** 获取通道的回放流地址 */
  Map<String, Object> getPlaybackUrl(
      String instanceKey, String deviceId, String channelId, long startTime, long endTime);

  /** 获取通道的回放流地址（支持平台特有参数） */
  Map<String, Object> getPlaybackUrl(
      String instanceKey,
      String deviceId,
      String channelId,
      long startTime,
      long endTime,
      Map<String, String> options);

  /** 停止预览/回放时释放直连 RTSP 代理到 ZLM 的拉流任务。 */
  Map<String, Object> stopPreview(
      String instanceKey, String deviceId, String channelId, Map<String, String> options);

  /** 设备抓图；优先 SDK 原生能力，失败时使用 RTSP/ZLM 抓图并归档 OSS。 */
  Map<String, Object> captureSnapshot(
      String instanceKey, String deviceId, String channelId, Map<String, Object> options);

  /** Internal durable alarm worker only; revalidates saved policy and exact tenant/device/channel. */
  Map<String, Object> captureAlarmPlatformFrame(
      String instanceKey, String tenantId, String deviceId, String channelId);

  /** PTZ控制 */
  void controlPTZ(String instanceKey, String deviceId, String channelId, String command, int speed);

  /** 查询录像记录 */
  List<Map<String, Object>> queryRecords(
      String instanceKey, String deviceId, String channelId, long startTime, long endTime);

  /** 查询录像记录（支持平台特有参数） */
  List<Map<String, Object>> queryRecords(
      String instanceKey,
      String deviceId,
      String channelId,
      long startTime,
      long endTime,
      Map<String, String> options);

  // ==================== 国标录像下载 ====================

  /** 开始国标录像下载 */
  cn.universal.common.domain.R<Map<String, Object>> startGBRecordDownload(
      String instanceKey, String deviceId, String channelId, Map<String, Object> data);

  /** 停止国标录像下载 */
  cn.universal.common.domain.R<Void> stopGBRecordDownload(
      String instanceKey, String deviceId, String channelId, Map<String, Object> data);

  /** 获取国标录像下载进度 */
  cn.universal.common.domain.R<Map<String, Object>> getGBRecordDownloadProgress(
      String instanceKey, String deviceId, String channelId, String stream);

  // ==================== 云端录像 ====================

  /** 查询云端录像日期列表 */
  cn.universal.common.domain.R<List<String>> queryCloudRecordDates(
      String instanceKey, String deviceId, String channelId, Map<String, String> params);

  /** 查询云端录像列表 */
  cn.universal.common.domain.R<Map<String, Object>> queryCloudRecords(
      String instanceKey, String deviceId, String channelId, Map<String, String> params);

  /** 加载云端录像文件 */
  cn.universal.common.domain.R<Map<String, Object>> loadCloudRecord(
      String instanceKey, String deviceId, String channelId, Map<String, Object> data);

  /** 云端录像定位 */
  cn.universal.common.domain.R<Void> seekCloudRecord(
      String instanceKey, String deviceId, String channelId, Map<String, Object> data);

  /** 设置云端录像倍速 */
  cn.universal.common.domain.R<Void> setCloudRecordSpeed(
      String instanceKey, String deviceId, String channelId, Map<String, Object> data);

  /** 获取云端录像下载地址 */
  cn.universal.common.domain.R<Map<String, Object>> getCloudRecordPlayPath(
      String instanceKey, Integer recordId);

  /**
   * 获取设备完整信息（跨主表和扩展表查询）
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @return 设备完整信息（主表+扩展表）
   */
  Map<String, Object> getDeviceDetail(String instanceKey, String deviceId);

  /** 更新设备基本信息（remark/gps/org/enabled等） */
  Map<String, Object> updateDevice(
      String instanceKey, String deviceId, Map<String, Object> updates);

  /** 删除设备（级联删除扩展与通道） */
  void deleteDevice(String instanceKey, String deviceId);
}
