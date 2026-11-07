/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频设备导入服务实现
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.video.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.universal.admin.video.service.VideoDeviceImportService;
import cn.universal.admin.video.service.VideoPlatformProductService;
import cn.universal.admin.video.service.VideoDeviceNames;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.exception.BaseException;
import cn.universal.common.utils.StringUtils;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import cn.universal.security.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

/** 视频设备导入服务 负责将三方平台设备落库到IoTDevice */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoDeviceImportServiceImpl implements VideoDeviceImportService {

  private final VideoPlatformInstanceMapper instanceMapper;
  private final VideoPlatformProductService productService;
  private final IoTDeviceMapper deviceMapper;
  private final IoTProductMapper productMapper;
  private final ObjectMapper objectMapper;
  private final VideoPlatformDeviceMapper videoPlatformDeviceMapper;
  private final VideoPlatformChannelMapper videoPlatformChannelMapper;
  private final cn.universal.admin.video.alarm.SdkVideoAlarmContextCache alarmContextCache;

  @Override
  @Transactional
  public Map<String, Integer> importDevices(
      String instanceKey, List<Map<String, Object>> devices, String productKey) {
    Map<String, Integer> result = new HashMap<>();
    int success = 0, failed = 0, exists = 0;

    for (Map<String, Object> device : devices) {
      try {
        String deviceId = importDevice(instanceKey, device, productKey);
        if (deviceId != null) {
          if (deviceId.startsWith("EXISTS:")) {
            exists++;
          } else {
            success++;
          }
        } else {
          failed++;
        }
      } catch (Exception e) {
        log.error("导入设备失败：{}", e.getMessage(), e);
        failed++;
      }
    }

    result.put("success", success);
    result.put("failed", failed);
    result.put("exists", exists);
    return result;
  }

  @Override
  @Transactional
  public String importDevice(String instanceKey, Map<String, Object> device, String productKey) {
    // 1. 获取平台实例
    Example example = new Example(VideoPlatformInstance.class);
    example.createCriteria().andEqualTo("instanceKey", instanceKey);
    VideoPlatformInstance instance =
        instanceMapper.selectByExample(example).stream().findFirst().orElse(null);
    if (instance == null) {
      throw new BaseException("平台实例不存在：" + instanceKey);
    }

    // 2. 获取或创建平台产品
    String deviceId = (String) device.get("deviceId");
    if (StringUtils.isEmpty(deviceId)) {
      throw new BaseException("设备DeviceId不能为空");
    }

    VideoPlatformDevice catalog = videoPlatformDeviceMapper.selectOne(VideoPlatformDevice.builder()
        .instanceKey(instanceKey).deviceId(deviceId).tenantId(instance.getTenantId()).build());
    if (catalog != null) {
      device = new java.util.LinkedHashMap<>(device);
      device.put("deviceName", catalog.getDeviceName());
      Map<String, Object> config = VideoDeviceNames.configuration(catalog.getConfiguration());
      if (config.containsKey("channelList")) {
        device.put("channelList", config.get("channelList"));
        device.remove("channels");
      }
      for (String key : VideoDeviceNames.METADATA)
        if (config.containsKey(key)) device.put(key, config.get(key));
      if (supportsSdkChannelCatalog(instance.getPlatformType())) {
        for (String key : List.of("channelCatalogAuthoritative", "loginChannelCount", "channelCount"))
          if (config.containsKey(key)) device.put(key, config.get(key));
      }
    }

    // 判断设备节点类型（优先从设备数据获取，否则默认VIDEO_DEVICE）
    String deviceNode = (String) device.getOrDefault("deviceNode", "VIDEO_DEVICE");

    IoTProduct product;
    if (StringUtils.isEmpty(productKey)) {
      // 自动创建/选择平台产品
      product = productService.ensurePlatformProduct(instance, deviceNode);
    } else {
      // 使用指定产品
      product = productMapper.getProductByProductKey(productKey);
      if (product == null) {
        throw new BaseException("指定产品不存在：" + productKey);
      }
    }
    String deviceName = (String) device.getOrDefault("deviceName", "");
    String deviceIp = (String) device.getOrDefault("deviceIp", "");

    // 3. 检查设备是否已存在（幂等性校验：productKey + deviceId）
    Example deviceExample = new Example(IoTDevice.class);
    deviceExample
        .createCriteria()
        .andEqualTo("productKey", product.getProductKey())
        .andEqualTo("deviceId", deviceId)
        .andEqualTo("tenantId", instance.getTenantId());
    IoTDevice existingDevice =
        deviceMapper.selectByExample(deviceExample).stream().findFirst().orElse(null);
    if (existingDevice != null) {
      updateDeviceStatusAndConfig(instanceKey, device, instance.getPlatformType());
      log.debug("设备已存在，刷新资料：{}/{}", product.getProductKey(), deviceId);
      return "EXISTS:" + existingDevice.getIotId();
    }

    // 4. 创建IoTDevice
    IoTDevice iotDevice = new IoTDevice();
    // 沿用平台既有 iotId 规则；上行查找仍用 productKey + deviceId 两列，不能反拆 iotId。
    iotDevice.setIotId(product.getProductKey() + deviceId);
    iotDevice.setProductKey(product.getProductKey());
    iotDevice.setDeviceId(deviceId);
    iotDevice.setDeviceSecret(
        MD5.create().digestHex(product.getProductKey() + deviceId + IoTConstant.SALT));
    iotDevice.setDeviceName(StrUtil.isBlank(deviceName) ? deviceIp : deviceName);
    iotDevice.setProductName(product.getName());
    iotDevice.setState(parseDeviceStatus(device));
    String actor = SecurityUtils.getUnionIdQuietly();
    if (StrUtil.isBlank(actor)) actor = instance.getCreatorId();
    if (StrUtil.isBlank(actor)) throw new BaseException("视频实例缺少可信创建者");
    iotDevice.setCreatorId(actor);
    iotDevice.setTenantId(StrUtil.blankToDefault(product.getTenantId(), instance.getTenantId()));
    iotDevice.setCreateTime(System.currentTimeMillis() / 1000);

    // 填充网关产品Key（平台实例作为网关）
    iotDevice.setGwProductKey(product.getProductKey());

    // 填充配置字段（包含通道列表等）
    // 对于WVP设备，需要从video_platform_channel表查询通道列表
    iotDevice.setConfiguration(buildConfiguration(device, instanceKey, instance.getPlatformType()));

    // 填充派生元数据（媒体能力、通道信息等）
    iotDevice.setDeriveMetadata(buildDeriveMetadata(device));

    deviceMapper.insertSelective(iotDevice);
    alarmContextCache.publishPlatformChange(instanceKey);
    log.info("设备导入成功：{}/{} -> {}", product.getProductKey(), deviceId, iotDevice.getIotId());
    return iotDevice.getIotId();
  }

  /** 解析设备状态 */
  private Integer parseDeviceStatus(Map<String, Object> device) {
    Object status = device.getOrDefault("status", device.get("deviceStatus"));
    boolean flag = false;
    if (status == null) return 0;
    if (status instanceof Boolean) {
      flag = (Boolean) status;
      return flag ? 1 : 0;
    }
    String statusStr = status.toString().toLowerCase();
    flag = "online".equals(statusStr) || "on".equals(statusStr) || "1".equals(statusStr) || "true".equals(statusStr);
    return flag ? 1 : 0;
  }

  /**
   * 更新已存在设备的在线状态和配置（同步时调用）
   *
   * @param instanceKey 平台实例Key
   * @param device 设备数据
   * @param platformType 平台类型
   * @return 更新成功返回true
   */
  @Transactional
  public boolean updateDeviceStatusAndConfig(
      String instanceKey, Map<String, Object> device, String platformType) {
    try {
      String deviceId = (String) device.get("deviceId");
      if (StringUtils.isEmpty(deviceId)) {
        return false;
      }

      VideoPlatformInstance instance = instanceMapper.selectOne(VideoPlatformInstance.builder().instanceKey(instanceKey).build());
      if (instance == null || StrUtil.isBlank(instance.getTenantId()))
        throw new BaseException("视频实例缺少可信租户");
      // 自定义产品导入也通过明确的 instanceKey 关联；同租户同 deviceId 不代表同一视频实例。
      Example deviceExample = new Example(IoTDevice.class);
      Example.Criteria identity = deviceExample.createCriteria().andEqualTo("deviceId", deviceId)
          .andEqualTo("tenantId", instance.getTenantId());
      if (!supportsSdkChannelCatalog(platformType)) identity.andEqualTo("productKey", instanceKey);
      deviceExample.setForUpdate(true);
      List<IoTDevice> existingDevices = deviceMapper.selectByExample(deviceExample);
      boolean linked = false;
      for (IoTDevice existingDevice : existingDevices) {
        Map<String, Object> merged = new java.util.LinkedHashMap<>();
        if (StrUtil.isNotBlank(existingDevice.getConfiguration()))
          merged.putAll(objectMapper.readValue(existingDevice.getConfiguration(), Map.class));
        String linkedInstance = VideoDeviceNames.text(merged.get("instanceKey"));
        if (!instanceKey.equals(linkedInstance)
            && !(linkedInstance.isEmpty() && instanceKey.equals(existingDevice.getProductKey()))) continue;
        linked = true;
        Integer newStatus = parseDeviceStatus(device);
        com.fasterxml.jackson.databind.JsonNode previousConfiguration = objectMapper.valueToTree(merged);
        Map<String, Object> incoming = objectMapper.readValue(buildConfiguration(device, instanceKey, platformType), Map.class);
        List<Map<String, Object>> incomingChannels = mergeChannelSnapshots(merged.get("channelList"), incoming.get("channelList"));
        if (supportsSdkChannelCatalog(platformType)
            && Boolean.FALSE.equals(device.get("channelCatalogAuthoritative"))) {
          incomingChannels = retainMissingChannelSnapshots(merged.get("channelList"), incomingChannels);
        }
        incoming.put("channelList", incomingChannels);
        merged.putAll(incoming);
        String alias = VideoDeviceNames.text(
            device.get(VideoDeviceNames.DEVICE_OVERRIDE));
        boolean renamed = !alias.isEmpty() && !java.util.Objects.equals(alias, existingDevice.getDeviceName());
        if (java.util.Objects.equals(existingDevice.getState(), newStatus)
            && !renamed && previousConfiguration.equals(objectMapper.valueToTree(merged))) continue;
        existingDevice.setState(newStatus);
        existingDevice.setConfiguration(objectMapper.writeValueAsString(merged));
        if (renamed) existingDevice.setDeviceName(alias);
        deviceMapper.updateByPrimaryKeySelective(existingDevice);
        alarmContextCache.publishPlatformChange(instanceKey);
      }
      return linked;
    } catch (Exception e) {
      log.error("更新设备状态失败: deviceId={}, error={}", device.get("deviceId"), e.getMessage());
      throw new BaseException("同步物联设备资料失败");
    }
  }

  private List<Map<String, Object>> mergeChannelSnapshots(Object stored, Object incoming) {
    Map<String, Map<String, Object>> previous = new java.util.LinkedHashMap<>();
    if (stored instanceof List<?> list) for (Object value : list) {
      Map<String, Object> channel = VideoDeviceNames.object(value);
      previous.put(VideoDeviceNames.text(channel.get("channelId")), channel);
    }
    List<Map<String, Object>> result = new java.util.ArrayList<>();
    if (incoming instanceof List<?> list) for (Object value : list) {
      Map<String, Object> channel = VideoDeviceNames.object(value);
      Map<String, Object> merged = new java.util.LinkedHashMap<>(previous.getOrDefault(
          VideoDeviceNames.text(channel.get("channelId")), Map.of()));
      merged.putAll(channel);
      if (merged.containsKey("name")) merged.put("name", merged.get("channelName"));
      result.add(merged);
    }
    return result;
  }

  private List<Map<String, Object>> retainMissingChannelSnapshots(
      Object stored, List<Map<String, Object>> incoming) {
    Map<String, Map<String, Object>> result = new java.util.LinkedHashMap<>();
    if (stored instanceof List<?> list) for (Object item : list) {
      Map<String, Object> channel = VideoDeviceNames.object(item);
      result.put(VideoDeviceNames.text(channel.get("channelId")), channel);
    }
    for (Map<String, Object> channel : incoming)
      result.put(VideoDeviceNames.text(channel.get("channelId")), channel);
    return new java.util.ArrayList<>(result.values());
  }

  private boolean supportsSdkChannelCatalog(String platformType) {
    return "dahua_sdk".equalsIgnoreCase(platformType)
        || "hik_sdk".equalsIgnoreCase(platformType)
        || "uniview_sdk".equalsIgnoreCase(platformType)
        || "tiandy_sdk".equalsIgnoreCase(platformType);
  }

  /** 构建配置字段（保存轻量配置，如通道列表） WVP设备的通道需要从video_platform_channel表查询 乐橙云等其他设备的通道直接从设备数据中获取 */
  private String buildConfiguration(
      Map<String, Object> device, String instanceKey, String platformType) {
    try {
      Map<String, Object> config = new HashMap<>();
      config.put("instanceKey", instanceKey);
      config.put("platformType", platformType);
      // 保存厂商、型号等基础信息
      if (device.containsKey("manufacturer")) {
        config.put("manufacturer", device.get("manufacturer"));
      }
      if (device.containsKey("model")) {
        config.put("model", device.get("model"));
      }

      // 构建统一的channelList结构
      List<Map<String, Object>> channelList = new java.util.ArrayList<>();

      if (IoTConstant.ProtocolModule.wvp.name().equalsIgnoreCase(platformType) || "hik_isup".equals(platformType)) {
        // WVP设备: 从video_platform_channel表查询通道
        channelList = queryWvpChannels(instanceKey, (String) device.get("deviceId"));
      } else {
        // 其他平台: 从设备数据中获取通道列表，并统一结构
        channelList = normalizeChannelList(device, platformType);
      }

      // 如果没有通道，添加默认通道
      boolean authoritativeSdkCatalog = supportsSdkChannelCatalog(platformType)
          && Boolean.TRUE.equals(device.get("channelCatalogAuthoritative"));
      boolean explicitSdkDirectory = ("hik_sdk".equalsIgnoreCase(platformType)
          || "tiandy_sdk".equalsIgnoreCase(platformType))
          && device.containsKey("channelCatalogAuthoritative")
          && device.get("channelList") instanceof List<?>;
      if (channelList.isEmpty() && !"hik_isup".equals(platformType)
          && !authoritativeSdkCatalog && !explicitSdkDirectory) {
        channelList.add(createDefaultChannel(device, platformType));
      }

      config.put("channelList", channelList);

      if (device.containsKey("channelCount")) {
        config.put("channelCount", device.get("channelCount"));
      }
      if (supportsSdkChannelCatalog(platformType)) {
        for (String key : List.of("channelCatalogAuthoritative", "loginChannelCount"))
          if (device.containsKey(key)) config.put(key, device.get(key));
      }
      // 保存原始三方设备ID
      if (device.containsKey("extDeviceId")) {
        config.put("thirdPartyDeviceId", device.get("extDeviceId"));
      }
      // 设备类型（IPC/NVR等）
      if (device.containsKey("deviceType")) {
        config.put("deviceType", device.get("deviceType"));
      }
      return objectMapper.writeValueAsString(config);
    } catch (Exception e) {
      log.warn("构建配置失败：{}", e.getMessage());
      return "{}";
    }
  }

  /** 规范化通道列表（统一字段名） */
  private List<Map<String, Object>> normalizeChannelList(Map<String, Object> device, String platformType) {
    List<Map<String, Object>> channelList = new java.util.ArrayList<>();
    Object channels = device.get("channelList");
    if (channels == null) {
      channels = device.get("channels");
    }
    if (channels instanceof List) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> rawChannels = (List<Map<String, Object>>) channels;
      for (Map<String, Object> ch : rawChannels) {
        Map<String, Object> normalizedChannel = new HashMap<>();
        // 统一channelId字段
        Object chId = ch.get("channelId");
        if (chId == null) chId = ch.get("id");
        if (chId == null || String.valueOf(chId).isBlank()) continue;
        normalizedChannel.put("channelId", String.valueOf(chId));
        // 统一channelName字段
        Object chName = ch.get("channelName");
        if (chName == null) chName = ch.get("name");
        normalizedChannel.put("channelName", chName != null ? String.valueOf(chName) : "通道");
        // 统一channelStatus字段
        Object chStatus = ch.get("channelStatus");
        if (chStatus == null) chStatus = ch.get("status");
        if (supportsSdkChannelCatalog(platformType)) {
          Map<String, Object> metadata = VideoDeviceNames.object(ch.get("attributes"));
          metadata.putAll(ch);
          String state = VideoDeviceNames.text(metadata.get("onlineState")).toLowerCase(java.util.Locale.ROOT);
          if (List.of("online", "offline", "unknown").contains(state)) chStatus = state;
          for (String key : List.of("channelNo", "nativeChannelNo", "channelSeq", "rtspChannel", "sdkChannel",
              "streamType", "onlineState", "configured", "enabled", "channelStateSource"))
            if (metadata.containsKey(key)) normalizedChannel.put(key, metadata.get(key));
        }
        normalizedChannel.put(
            "channelStatus", chStatus != null ? String.valueOf(chStatus) : "online");
        channelList.add(normalizedChannel);
      }
    }
    return channelList;
  }

  /** 创建默认通道 */
  private Map<String, Object> createDefaultChannel(
      Map<String, Object> device, String platformType) {
    Map<String, Object> defaultChannel = new HashMap<>();
    if (IoTConstant.ProtocolModule.wvp.name().equalsIgnoreCase(platformType)) {
      // WVP设备默认使用deviceId作为channelId
      defaultChannel.put("channelId", device.get("deviceId"));
    } else {
      defaultChannel.put("channelId", "0");
    }
    defaultChannel.put("channelName", "通道1");
    defaultChannel.put("channelStatus", "online");
    return defaultChannel;
  }

  /**
   * 从video_platform_channel表查询WVP设备的通道列表 返回统一的channelList结构: [{channelId, channelName,
   * channelStatus}]
   */
  private List<Map<String, Object>> queryWvpChannels(String instanceKey, String platformDeviceId) {
    List<Map<String, Object>> result = new java.util.ArrayList<>();
    try {
      // 先查询video_platform_device获取主表ID
      Example deviceEx = new Example(VideoPlatformDevice.class);
      deviceEx
          .createCriteria()
          .andEqualTo("instanceKey", instanceKey)
          .andEqualTo("deviceId", platformDeviceId);
      List<VideoPlatformDevice> devices = videoPlatformDeviceMapper.selectByExample(deviceEx);

      if (!devices.isEmpty()) {
        Long deviceId = devices.get(0).getId();
        // 查询通道表
        Example channelEx = new Example(VideoPlatformChannel.class);
        channelEx.createCriteria().andEqualTo("deviceId", deviceId);
        List<VideoPlatformChannel> channels = videoPlatformChannelMapper.selectByExample(channelEx);

        for (VideoPlatformChannel ch : channels) {
          Map<String, Object> channelMap = new HashMap<>();
          channelMap.put("channelId", ch.getChannelId());
          channelMap.put("channelName", ch.getChannelName());
          // 通道状态：1=在线 ON/online, 0=离线 OFF/offline
          String chStatus = ch.getChannelStatus();
          if (chStatus != null) {
            chStatus = chStatus.toUpperCase();
            if ("OFF".equals(chStatus) || "OFFLINE".equals(chStatus) || "0".equals(chStatus)) {
              channelMap.put("channelStatus", "offline");
            } else {
              channelMap.put("channelStatus", "online");
            }
          } else {
            channelMap.put("channelStatus", "online"); // 默认在线
          }
          // 可选：添加额外的WVP通道信息
          if (ch.getPtzType() != null) {
            channelMap.put("ptzType", ch.getPtzType());
          }
          result.add(channelMap);
        }
      }
    } catch (Exception e) {
      log.warn(
          "查询WVP通道失败: instanceKey={}, deviceId={}, error={}",
          instanceKey,
          platformDeviceId,
          e.getMessage());
    }
    return result;
  }

  /** 构建派生元数据（媒体能力、编解码等） */
  private String buildDeriveMetadata(Map<String, Object> device) {
    try {
      Map<String, Object> metadata = new HashMap<>();
      // 媒体能力（预览URL、回放能力等）
      if (device.containsKey("capabilities")) {
        metadata.put("capabilities", device.get("capabilities"));
      }
      // PTZ能力
      if (device.containsKey("ptzSupport")) {
        metadata.put("ptzSupport", device.get("ptzSupport"));
      }
      // 编解码信息
      if (device.containsKey("videoCodec")) {
        metadata.put("videoCodec", device.get("videoCodec"));
      }
      if (device.containsKey("audioCodec")) {
        metadata.put("audioCodec", device.get("audioCodec"));
      }
      return objectMapper.writeValueAsString(metadata);
    } catch (Exception e) {
      log.warn("构建派生元数据失败：{}", e.getMessage());
      return "{}";
    }
  }

  /** 构建事件元数据（仅用ext1，遵循规范） */
  private String buildEventMetadata(Map<String, Object> device) {
    try {
      Map<String, Object> eventMeta = new HashMap<>();
      // 保存原始三方设备ID
      eventMeta.put("thirdPartyDeviceId", device.get("extDeviceId"));
      // 设备类型（IPC/NVR等）
      if (device.containsKey("deviceType")) {
        eventMeta.put("deviceType", device.get("deviceType"));
      }
      return objectMapper.writeValueAsString(eventMeta);
    } catch (Exception e) {
      log.warn("构建事件元数据失败：{}", e.getMessage());
      return "{}";
    }
  }
}
