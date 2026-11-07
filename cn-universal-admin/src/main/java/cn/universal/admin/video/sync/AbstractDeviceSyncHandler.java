/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 设备同步处理器抽象基类
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.sync;

import cn.universal.admin.video.service.VideoPlatformDeviceCleanService;
import cn.universal.admin.video.service.VideoDeviceNames;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformDeviceExt;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceExtMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import cn.universal.security.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import tk.mybatis.mapper.entity.Example;

/**
 * 设备同步处理器抽象基类
 * 提供公共的数据库操作方法
 */
@Slf4j
public abstract class AbstractDeviceSyncHandler implements DeviceSyncHandler {

  private static final int LEN_20 = 20;
  private static final int LEN_50 = 50;
  private static final int LEN_100 = 100;
  private static final int LEN_200 = 200;
  private static final int LEN_500 = 500;

  @Autowired protected VideoPlatformDeviceMapper deviceMapper;

  @Autowired protected VideoPlatformDeviceExtMapper deviceExtMapper;

  @Autowired protected VideoPlatformChannelMapper channelMapper;

  @Autowired protected VideoPlatformInstanceMapper instanceMapper;

  @Autowired protected ObjectMapper objectMapper;

  @Autowired protected VideoPlatformDeviceCleanService cleanService;

  @Autowired protected DeviceSyncTransactionExecutor transactionExecutor;

  @Autowired protected cn.universal.admin.video.alarm.SdkVideoAlarmContextCache alarmContextCache;

  /**
   * 保存或更新设备主表记录
   * 
   * @param instanceKey 实例Key
   * @param deviceId 设备ID
   * @param deviceData 设备数据
   * @return 设备实体，包含生成的主键ID
   */
  protected VideoPlatformDevice upsertDevice(
      String instanceKey, String deviceId, Map<String, Object> deviceData) {
    String currentUserId = resolveSyncActor(instanceKey);
    String tenantId = resolveInstanceTenantId(instanceKey);
    Date now = new Date();

    // 提取公共字段
    String deviceName =
        fit(firstNonBlankString(deviceData.get("deviceName"), deviceData.get("name")), LEN_200);
    String status =
        fit(firstNonBlankString(deviceData.get("deviceStatus"), deviceData.get("status")), LEN_50);
    if (status == null || status.isBlank()) {
      status = "online";
    }
    String model =
        fit(firstNonBlankString(deviceData.get("deviceModel"), deviceData.get("model")), LEN_100);
    String manufacturer = fit(firstNonBlankString(deviceData.get("manufacturer"), ""), LEN_100);
    String orgId =
        fit(firstNonBlankString(deviceData.get("orgId"), deviceData.get("organizationId")), LEN_100);
    String orgName = fit(firstNonBlankString(deviceData.get("orgName"), ""), LEN_200);
    String deviceIp = fit(firstNonBlankString(deviceData.get("ip"), deviceData.get("deviceIp")), LEN_50);
    String gpsX =
        fit(
            firstNonBlankString(deviceData.get("gpsX"), deviceData.get("longitude"), deviceData.get("lng")),
            LEN_50);
    String gpsY =
        fit(
            firstNonBlankString(deviceData.get("gpsY"), deviceData.get("latitude"), deviceData.get("lat")),
            LEN_50);
    String gpsZ =
        fit(
            firstNonBlankString(deviceData.get("gpsZ"), deviceData.get("altitude"), deviceData.get("height")),
            LEN_50);
    instanceKey = fit(instanceKey, LEN_100);
    deviceId = fit(deviceId, LEN_100);
    currentUserId = fit(currentUserId, LEN_100);

    Integer devicePort = null;
    try {
      Object port = deviceData.getOrDefault("port", deviceData.get("devicePort"));
      if (port != null) devicePort = Integer.parseInt(String.valueOf(port));
    } catch (Exception ignore) {
    }

    // 构建configuration JSON
    String configuration = null;
    try {
      Map<String, Object> cfg = new java.util.HashMap<>();
      Object channelList = deviceData.get("channelList");
      if (channelList != null) cfg.put("channelList", channelList);
      Object extFields = deviceData.get("extFields");
      if (extFields != null) cfg.put("extFields", extFields);
      Object raw = deviceData.get("raw");
      if (raw != null) cfg.put("raw", raw);
      if (List.of("hik_sdk", "dahua_sdk", "tiandy_sdk", "uniview_sdk", "hik_isup").contains(getPlatformType())) {
        for (String key : List.of("channelCatalogAuthoritative", "loginChannelCount", "channelCount"))
          if (deviceData.containsKey(key)) cfg.put(key, deviceData.get(key));
      }
      for (String key : VideoDeviceNames.METADATA) {
        if (deviceData.containsKey(key)) cfg.put(key, deviceData.get(key));
      }
      if (!cfg.isEmpty()) configuration = objectMapper.writeValueAsString(cfg);
    } catch (Exception ignore) {
    }

    // 查询是否存在
    Example ex = new Example(VideoPlatformDevice.class);
    ex.createCriteria().andEqualTo("instanceKey", instanceKey).andEqualTo("deviceId", deviceId);
    List<VideoPlatformDevice> existList = deviceMapper.selectByExample(ex);
    VideoPlatformDevice deviceEntity = existList.isEmpty() ? null : existList.get(0);
    com.fasterxml.jackson.databind.JsonNode previousState = cacheState(deviceEntity);

    if (deviceEntity == null) {
      // 新增
      deviceEntity =
          VideoPlatformDevice.builder()
              .instanceKey(instanceKey)
              .deviceId(deviceId)
              .deviceName(deviceName)
              .deviceStatus(status)
              .deviceModel(model)
              .manufacturer(manufacturer)
              .deviceIp(deviceIp)
              .devicePort(devicePort)
              .orgId(orgId)
              .orgName(orgName)
              .gpsX(gpsX)
              .gpsY(gpsY)
              .gpsZ(gpsZ)
              .configuration(configuration)
              .enabled(1)
              .createId(currentUserId)
              .tenantId(tenantId)
              .updateId(currentUserId)
              .createTime(now)
              .updateTime(now)
              .build();
      deviceMapper.insertSelective(deviceEntity);

      // 重新查询获取自动生成的ID
      Example reloadEx = new Example(VideoPlatformDevice.class);
      reloadEx.createCriteria().andEqualTo("instanceKey", instanceKey).andEqualTo("deviceId", deviceId);
      List<VideoPlatformDevice> reloadList = deviceMapper.selectByExample(reloadEx);
      if (!reloadList.isEmpty()) {
        deviceEntity = reloadList.get(0);
      }
    } else {
      // 更新
      deviceEntity.setDeviceName(deviceName);
      deviceEntity.setDeviceStatus(status);
      deviceEntity.setDeviceModel(model);
      deviceEntity.setManufacturer(manufacturer);
      deviceEntity.setDeviceIp(deviceIp);
      deviceEntity.setDevicePort(devicePort);
      deviceEntity.setOrgId(orgId);
      deviceEntity.setOrgName(orgName);
      deviceEntity.setGpsX(gpsX);
      deviceEntity.setGpsY(gpsY);
      deviceEntity.setGpsZ(gpsZ);
      deviceEntity.setConfiguration(preserveLocalConfiguration()
          ? mergeConfiguration(deviceEntity.getConfiguration(), configuration) : configuration);
      if (!preserveLocalConfiguration()) deviceEntity.setEnabled(1);
      // 租户归属以平台实例为准，修复多租户接入前或实例迁移后遗留的错误归属。
      if (tenantId != null && !tenantId.isBlank()) {
        deviceEntity.setTenantId(tenantId);
      }
      if (hasDeviceChanges(previousState, deviceEntity, false)) {
        deviceEntity.setUpdateId(currentUserId);
        deviceEntity.setUpdateTime(now);
        deviceMapper.updateByPrimaryKeySelective(deviceEntity);
      }
    }

    // 设备同步可能变更 IP、启用状态及租户归属，提交后使告警路由重新加载。
    if (hasDeviceChanges(previousState, deviceEntity, true)) {
      alarmContextCache.publishPlatformChange(instanceKey);
    }
    return deviceEntity;
  }

  private boolean hasDeviceChanges(
      com.fasterxml.jackson.databind.JsonNode previous, VideoPlatformDevice device,
      boolean routingOnly) {
    com.fasterxml.jackson.databind.node.ObjectNode current =
        (com.fasterxml.jackson.databind.node.ObjectNode) cacheState(device);
    if (previous.isObject()) {
      // updateByPrimaryKeySelective 不写 NULL；缺失字段不能被误判为每轮都在清空旧值。
      previous.properties().forEach(entry -> {
        com.fasterxml.jackson.databind.JsonNode incoming = current.get(entry.getKey());
        if ((incoming == null || incoming.isNull())
            && (!"configuration".equals(entry.getKey()) || device.getConfiguration() == null)) {
          current.set(entry.getKey(), entry.getValue());
        }
      });
    }
    if (routingOnly) {
      previous = previous.deepCopy();
      removeObservationTime(previous);
      removeObservationTime(current);
    }
    return !previous.equals(current);
  }

  private void removeObservationTime(com.fasterxml.jackson.databind.JsonNode state) {
    if (state.isObject()) {
      // SDK 周期性探测时间继续入库，但不改变告警路由或通道能力。
      ((com.fasterxml.jackson.databind.node.ObjectNode) state).remove("capabilitiesCheckedAt");
      state.elements().forEachRemaining(this::removeObservationTime);
    } else if (state.isArray()) {
      state.elements().forEachRemaining(this::removeObservationTime);
    }
  }

  private com.fasterxml.jackson.databind.JsonNode cacheState(VideoPlatformDevice device) {
    if (device == null) return com.fasterxml.jackson.databind.node.NullNode.getInstance();
    com.fasterxml.jackson.databind.node.ObjectNode state = objectMapper.valueToTree(device);
    state.remove(List.of("updateId", "updateTime"));
    if (device.getConfiguration() != null) {
      try {
        state.set("configuration", objectMapper.readTree(device.getConfiguration()));
      } catch (java.io.IOException ignored) {
        // 历史非 JSON 配置仍按原字符串比较，不吞掉实际配置变化。
      }
    }
    return state;
  }

  /**
   * 解析整型或返回null
   */
  protected Integer parseIntOrNull(Object value) {
    if (value == null) return null;
    try {
      return Integer.parseInt(String.valueOf(value));
    } catch (Exception e) {
      return null;
    }
  }

  protected String firstNonBlankString(Object... values) {
    for (Object value : values) {
      if (value == null) {
        continue;
      }
      String text = String.valueOf(value).trim();
      if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
        return text;
      }
    }
    return null;
  }

  protected boolean shouldResetBeforeSync(Map<String, Object> filters) {
    if (filters == null) {
      return true;
    }
    Object flag = filters.get("__resetBeforeSync");
    if (flag == null) {
      flag = filters.get("_resetBeforeSync");
    }
    if (flag == null) {
      return true;
    }
    return Boolean.parseBoolean(String.valueOf(flag));
  }

  protected String resolvePlatformDeviceId(Map<String, Object> deviceData) {
    return fit(
        firstNonBlankString(
            deviceData.get("deviceId"),
            deviceData.get("id"),
            deviceData.get("cameraIndexCode"),
            deviceData.get("channelId")),
        LEN_100);
  }

  protected DeviceRecordSyncResult syncSingleDeviceInNewTransaction(
      VideoPlatformInstance instance, Map<String, Object> deviceData, String deviceId) throws Exception {
    return transactionExecutor.execute(
        () -> syncSingleDevice(instance.getInstanceKey(), deviceId, deviceData));
  }

  protected DeviceRecordSyncResult syncSingleDevice(
      String instanceKey, String deviceId, Map<String, Object> deviceData) {
    Example ex = new Example(VideoPlatformDevice.class);
    ex.createCriteria().andEqualTo("instanceKey", instanceKey).andEqualTo("deviceId", deviceId);
    ex.setForUpdate(true);
    List<VideoPlatformDevice> existList = deviceMapper.selectByExample(ex);
    boolean isNewDevice = existList.isEmpty();
    deviceData = applyDeviceNames(isNewDevice ? null : existList.get(0), deviceData);

    VideoPlatformDevice deviceEntity = upsertDevice(instanceKey, deviceId, deviceData);
    upsertDeviceExt(deviceEntity.getId(), instanceKey, deviceId, deviceData);

    int channelCount = 0;
    Object channelListObj = deviceData.get("channelList");
    if (channelListObj instanceof List) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> channelList = (List<Map<String, Object>>) channelListObj;
      // 只有成功读取的实时目录才能清空空槽；离线快照仍保留已有通道及本地配置。
      if (hasAuthoritativeChannelCatalog(deviceData)
          && (preserveLocalConfiguration() || channelList.isEmpty())) {
        cleanupStaleDeviceChannels(
            deviceEntity.getId(),
            collectIncomingChannelKeys(deviceEntity.getId(), instanceKey, deviceId, channelList));
      }
      channelCount = batchInsertChannels(deviceEntity.getId(), instanceKey, deviceId, channelList);
    }
    if (preserveLocalConfiguration() && isOffline(deviceEntity.getDeviceStatus())) {
      VideoPlatformChannel update = VideoPlatformChannel.builder().channelStatus("offline").build();
      Example channels = new Example(VideoPlatformChannel.class);
      channels.createCriteria().andEqualTo("deviceId", deviceEntity.getId());
      channelMapper.updateByExampleSelective(update, channels);
    }

    return new DeviceRecordSyncResult(deviceId, isNewDevice, channelCount);
  }

  protected Map<String, Object> applyDeviceNames(VideoPlatformDevice stored, Map<String, Object> incoming) {
    return VideoDeviceNames.apply(stored == null ? null : stored.getConfiguration(), incoming);
  }

  protected String resolveSyncActor(String instanceKey) {
    String actor = SecurityUtils.getUnionIdQuietly();
    if (actor != null && !actor.isBlank()) return actor;
    VideoPlatformInstance instance = instanceMapper.selectOne(VideoPlatformInstance.builder().instanceKey(instanceKey).build());
    if (instance == null || instance.getCreatorId() == null || instance.getCreatorId().isBlank())
      throw new cn.universal.common.exception.BaseException("视频实例缺少可信创建者");
    return instance.getCreatorId();
  }

  /** 主动注册目录仅更新运行状态，不覆盖人工启停、融合等本地配置。 */
  protected boolean preserveLocalConfiguration() { return false; }

  protected boolean hasAuthoritativeChannelCatalog(Map<String, Object> deviceData) { return false; }

  protected boolean preserveChannelDirectory() { return preserveLocalConfiguration(); }

  protected boolean isOffline(String status) {
    return status != null && Set.of("off", "offline", "0", "false")
        .contains(status.toLowerCase(java.util.Locale.ROOT));
  }

  protected String mergeConfiguration(String stored, String incoming) {
    try {
      Map<String, Object> result = new java.util.LinkedHashMap<>();
      if (stored != null && !stored.isBlank()) result.putAll(objectMapper.readValue(stored, Map.class));
      if (incoming != null && !incoming.isBlank()) result.putAll(objectMapper.readValue(incoming, Map.class));
      return objectMapper.writeValueAsString(result);
    } catch (Exception e) {
      throw new IllegalStateException("读取已保存的视频配置失败，已停止同步以保留原配置", e);
    }
  }

  protected String resolveInstanceTenantId(String instanceKey) {
    if (instanceKey == null || instanceKey.isBlank()) {
      return null;
    }
    VideoPlatformInstance instance =
        instanceMapper.selectOne(VideoPlatformInstance.builder().instanceKey(instanceKey).build());
    return instance == null ? null : instance.getTenantId();
  }

  protected void applySyncResult(DeviceSyncResult result, DeviceRecordSyncResult itemResult) {
    if (result == null || itemResult == null) {
      return;
    }
    result.setTotalDevices(result.getTotalDevices() + 1);
    if (itemResult.isNewDevice()) {
      result.addInsertedDevice();
    } else {
      result.addUpdatedDevice();
    }
    result.addChannel(itemResult.getChannelCount());
  }

  protected void addSyncError(DeviceSyncResult result, String deviceId, Exception e) {
    String message =
        "设备同步失败"
            + (deviceId == null || deviceId.isBlank() ? "" : "[deviceId=" + deviceId + "]")
            + ": "
            + e.getMessage();
    result.addError(message);
  }

  protected String fit(String value, int maxLength) {
    if (value == null || maxLength <= 0 || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }

  /**
   * 同步通道列表。
   *
   * <p>通道编号只在所属设备内唯一，按 device_id + channel_id 做 upsert，与 uk_device_channel
   * 约束保持一致。WVP 中不同设备可以上报相同国标通道编号，不能跨设备合并；parent_id 是目录信息。
   *
   * @param deviceId 设备主键ID
   * @param instanceKey 实例Key
   * @param platformDeviceId 平台设备ID
   * @param channels 通道列表
   * @return 本次处理的通道数量
   */
  protected int batchInsertChannels(
      Long deviceId, String instanceKey, String platformDeviceId, List<Map<String, Object>> channels) {
    if (channels == null || channels.isEmpty()) {
      return 0;
    }

    // 插入新通道
    String currentUserId = resolveSyncActor(instanceKey);
    Date now = new Date();
    int count = 0;
    instanceKey = fit(instanceKey, LEN_100);
    platformDeviceId = fit(platformDeviceId, LEN_100);
    currentUserId = fit(currentUserId, LEN_100);
    String tenantId = fit(resolveInstanceTenantId(instanceKey), LEN_100);

    Set<ChannelUniqueKey> incomingKeys =
        collectIncomingChannelKeys(deviceId, instanceKey, platformDeviceId, channels);
    if (!preserveChannelDirectory()) cleanupStaleDeviceChannels(deviceId, incomingKeys);

    Set<ChannelUniqueKey> processedKeys = new HashSet<>();
    for (Map<String, Object> ch : channels) {
      String channelId = fit(firstNonBlankString(ch.get("channelId"), ch.get("id")), LEN_100);
      if (channelId == null || channelId.equals("null")) continue;
      String parentId = fit(firstNonBlankString(ch.get("parentId")), LEN_100);
      ChannelUniqueKey uniqueKey = new ChannelUniqueKey(deviceId, channelId);
      if (!processedKeys.add(uniqueKey)) {
        continue;
      }

      VideoPlatformChannel.VideoPlatformChannelBuilder builder =
          VideoPlatformChannel.builder()
              .deviceId(deviceId)
              .instanceKey(instanceKey)
              .platformDeviceId(platformDeviceId)
              .tenantId(tenantId)
              .channelId(channelId);

      // 设置公共字段
      builder
          .channelName(fit(firstNonBlankString(ch.get("channelName"), ch.get("name")), LEN_200))
          .channelStatus(
              fit(firstNonBlankString(ch.get("channelStatus"), ch.get("status"), "online"), LEN_50))
          .channelType(fit(firstNonBlankString(ch.get("channelType")), LEN_50))
          .parentId(parentId)
          .manufacturer(fit(firstNonBlankString(ch.get("manufacturer")), LEN_100))
          .model(fit(firstNonBlankString(ch.get("model")), LEN_100))
          .owner(fit(firstNonBlankString(ch.get("owner")), LEN_100))
          .civilCode(fit(firstNonBlankString(ch.get("civilCode")), LEN_100))
          .address(fit(firstNonBlankString(ch.get("address")), LEN_200))
          .parental(parseIntOrNull(ch.get("parental")))
          .safetyWay(parseIntOrNull(ch.get("safetyWay")))
          .registerWay(parseIntOrNull(ch.get("registerWay")))
          .secrecy(parseIntOrNull(ch.get("secrecy")))
          .ipAddress(fit(firstNonBlankString(ch.get("ipAddress")), LEN_50))
          .port(parseIntOrNull(ch.get("port")))
          .longitude(fit(firstNonBlankString(ch.get("longitude")), LEN_50))
          .latitude(fit(firstNonBlankString(ch.get("latitude")), LEN_50))
          .ptzType(parseIntOrNull(ch.get("ptzType")))
          .positionType(parseIntOrNull(ch.get("positionType")));

      // 调用子类设置特有字段
      setPlatformSpecificChannelFields(builder, ch);

      // 能力集和流配置
      try {
        Object capabilities = ch.get("capabilities");
        if (capabilities != null) {
          builder.capabilities(objectMapper.writeValueAsString(capabilities));
        }
        Object streamConfig = ch.get("streamConfig");
        if (streamConfig != null) {
          builder.streamConfig(objectMapper.writeValueAsString(streamConfig));
        }
      } catch (Exception ignore) {
      }

      builder
          .enabled(1)
          .createId(currentUserId)
          .updateId(currentUserId)
          .createTime(now)
          .updateTime(now);

      VideoPlatformChannel finalChannel = builder.build();
      upsertChannelByUniqueKey(finalChannel);
      count++;
    }

    return count;
  }

  private Set<ChannelUniqueKey> collectIncomingChannelKeys(
      Long deviceId, String instanceKey, String platformDeviceId, List<Map<String, Object>> channels) {
    Set<ChannelUniqueKey> incomingKeys = new HashSet<>();
    for (Map<String, Object> ch : channels) {
      String channelId = fit(firstNonBlankString(ch.get("channelId"), ch.get("id")), LEN_100);
      if (channelId == null || channelId.equals("null")) {
        continue;
      }
      ChannelUniqueKey uniqueKey = new ChannelUniqueKey(deviceId, channelId);
      if (!incomingKeys.add(uniqueKey)) {
        log.warn(
            "[视频同步] 跳过重复通道: instanceKey={}, platformDeviceId={}, channelId={}",
            instanceKey,
            platformDeviceId,
            channelId);
      }
    }
    return incomingKeys;
  }

  private void cleanupStaleDeviceChannels(Long deviceId, Set<ChannelUniqueKey> incomingKeys) {
    Example currentExample = new Example(VideoPlatformChannel.class);
    currentExample.createCriteria().andEqualTo("deviceId", deviceId);
    List<VideoPlatformChannel> currentChannels = channelMapper.selectByExample(currentExample);
    for (VideoPlatformChannel current : currentChannels) {
      ChannelUniqueKey currentKey =
          new ChannelUniqueKey(current.getDeviceId(), current.getChannelId());
      if (!incomingKeys.contains(currentKey)) {
        channelMapper.deleteByPrimaryKey(current.getId());
      }
    }
  }

  private void upsertChannelByUniqueKey(VideoPlatformChannel channel) {
    VideoPlatformChannel existing = selectChannelByUniqueKey(channel);
    if (existing == null) {
      insertChannelWithDuplicateRetry(channel);
      return;
    }

    updateExistingChannel(existing, channel);
  }

  private void insertChannelWithDuplicateRetry(VideoPlatformChannel channel) {
    try {
      channelMapper.insertSelective(channel);
    } catch (DuplicateKeyException e) {
      VideoPlatformChannel existing = selectChannelByUniqueKey(channel);
      if (existing == null) {
        throw e;
      }
      updateExistingChannel(existing, channel);
    }
  }

  private VideoPlatformChannel selectChannelByUniqueKey(VideoPlatformChannel channel) {
    Example example = new Example(VideoPlatformChannel.class);
    example
        .createCriteria()
        .andEqualTo("deviceId", channel.getDeviceId())
        .andEqualTo("channelId", channel.getChannelId());

    List<VideoPlatformChannel> existingChannels = channelMapper.selectByExample(example);
    if (existingChannels.size() > 1) {
      log.warn(
          "[视频同步] 发现重复通道历史数据: instanceKey={}, deviceId={}, channelId={}, count={}",
          channel.getInstanceKey(),
          channel.getDeviceId(),
          channel.getChannelId(),
          existingChannels.size());
    }
    return existingChannels.isEmpty() ? null : existingChannels.get(0);
  }

  protected boolean preserveChannelConfiguration() { return preserveLocalConfiguration(); }

  private void updateExistingChannel(VideoPlatformChannel existing, VideoPlatformChannel channel) {
    channel.setId(existing.getId());
    channel.setCreateId(existing.getCreateId());
    channel.setCreateTime(existing.getCreateTime());
    if (preserveChannelConfiguration()) {
      channel.setEnabled(existing.getEnabled());
      channel.setStreamConfig(mergeConfiguration(existing.getStreamConfig(), channel.getStreamConfig()));
      channel.setExtField3(mergeConfiguration(existing.getExtField3(), channel.getExtField3()));
      channelMapper.updateByPrimaryKeySelective(channel);
    } else {
      channelMapper.updateByPrimaryKey(channel);
    }
  }

  private static class ChannelUniqueKey {
    private final Long deviceId;
    private final String channelId;

    private ChannelUniqueKey(Long deviceId, String channelId) {
      this.deviceId = deviceId;
      this.channelId = channelId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ChannelUniqueKey)) {
        return false;
      }
      ChannelUniqueKey that = (ChannelUniqueKey) o;
      return Objects.equals(deviceId, that.deviceId) && Objects.equals(channelId, that.channelId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(deviceId, channelId);
    }
  }

  protected static class DeviceRecordSyncResult {
    private final String deviceId;
    private final boolean newDevice;
    private final int channelCount;

    protected DeviceRecordSyncResult(String deviceId, boolean newDevice, int channelCount) {
      this.deviceId = deviceId;
      this.newDevice = newDevice;
      this.channelCount = channelCount;
    }

    public String getDeviceId() {
      return deviceId;
    }

    public boolean isNewDevice() {
      return newDevice;
    }

    public int getChannelCount() {
      return channelCount;
    }
  }

  /**
   * 设置平台特有的通道字段（由子类实现）
   */
  protected abstract void setPlatformSpecificChannelFields(
      VideoPlatformChannel.VideoPlatformChannelBuilder builder, Map<String, Object> channelData);

  /**
   * 保存或更新设备扩展表（由子类实现平台特有字段）
   */
  protected abstract VideoPlatformDeviceExt upsertDeviceExt(
      Long deviceId, String instanceKey, String platformDeviceId, Map<String, Object> deviceData);
}
