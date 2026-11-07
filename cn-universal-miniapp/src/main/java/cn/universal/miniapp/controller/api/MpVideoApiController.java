/*
*
• Copyright (c) 2025, NexIoT. All Rights Reserved.

*
• @Description: 小程序视频设备API接口

• @Author: gitee.com/NexIoT

*
*/

package cn.universal.miniapp.controller.api;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.domain.R;
import cn.universal.common.exception.IoTException;
import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.miniapp.config.MpUserContext;
import cn.universal.miniapp.util.VideoProxyUtil;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import tk.mybatis.mapper.entity.Example;

/**
 * • 小程序视频设备API接口
 *
 * <p>•
 *
 * <p>通过VideoPlatformAdapter统一接口调用WVP/HIK/ICC等视频平台
 *
 * <p>•
 *
 * <p>设备/通道查询直接查库，控制类操作通过适配器调用
 *
 * <p>•
 *
 * <p>路由前缀: /mp/api/video
 *
 * <p>•
 *
 * <p>注意：小程序端视频播放推荐使用HLS(m3u8)格式，因为微信小程序原生video组件支持m3u8
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "小程序-视频设备", description = "小程序视频设备管理接口（WVP/国标）")
@RequestMapping("/mp/api/video")
public class MpVideoApiController {

  @Value("${video.proxy.domain:wxapi.nexiot.cc}")
  private String videoProxyDomain;

  @Value("${video.proxy.enabled:true}")
  private boolean videoProxyEnabled;

  // 可配置不同协议的代理端口
  @Value("${video.proxy.rtmp.port:1935}")
  private int rtmpProxyPort;

  @Value("${video.proxy.rtsp.port:554}")
  private int rtspProxyPort;

  @Value("${video.proxy.ws.port:31080}")
  private int wsProxyPort;

  @Value("${video.proxy.wss.port:30443}")
  private int wssProxyPort;

  private final VideoPlatformInstanceMapper instanceMapper;
  private final VideoPlatformDeviceMapper deviceMapper;
  private final VideoPlatformChannelMapper channelMapper;
  private final VideoPlatformAdapterRegistry adapterRegistry;

  // ==================== 查询类接口（直接查库） ====================

  /**
   * 获取平台实例列表
   *
   * @param platformType 平台类型
   * @return 平台实例列表
   */
  @Operation(summary = "获取平台实例列表", description = "获取所有可用的视频平台实例列表")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功获取平台实例列表",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = R.class)))
  @io.swagger.v3.oas.annotations.Parameter(
      name = "platformType",
      description = "平台类型",
      required = false)
  @GetMapping("/instances")
  public R<List<Map<String, Object>>> listInstances(
      @RequestParam(required = false) String platformType) {
    assertLogin();
    try {
      List<VideoPlatformInstance> instances;
      if (StrUtil.isNotBlank(platformType)) {
        instances = instanceMapper.selectByPlatformType(platformType);
      } else {
        instances = instanceMapper.selectAll();
      }
      List<Map<String, Object>> result = new ArrayList<>();
      for (VideoPlatformInstance inst : instances) {
        if (inst.getEnabled() != null && inst.getEnabled() == 1) {
          Map<String, Object> item = new HashMap<>();
          item.put("instanceKey", inst.getInstanceKey());
          item.put("name", inst.getName());
          item.put("platformType", inst.getPlatformType());
          item.put("endpoint", inst.getEndpoint());
          result.add(item);
        }
      }
      return R.ok(result);
    } catch (Exception e) {
      log.error("获取平台实例列表失败", e);
      return R.error("获取平台实例失败: " + e.getMessage(), null);
    }
  }

  /**
   * 获取设备列表（从video_platform_device表查询）
   *
   * @param instanceKey 平台实例Key
   * @param keyword 关键词搜索
   * @return 设备列表
   */
  @Operation(summary = "获取设备列表", description = "根据平台实例Key获取设备列表（从video_platform_device表查询）")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功获取设备列表",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = R.class)))
  @io.swagger.v3.oas.annotations.Parameter(
      name = "instanceKey",
      description = "平台实例Key",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "keyword",
      description = "关键词搜索",
      required = false)
  @GetMapping("/{instanceKey}/devices")
  public R<List<Map<String, Object>>> listDevices(
      @PathVariable("instanceKey") String instanceKey,
      @RequestParam(required = false) String keyword) {
    assertLogin();
    assertInstancePermission(instanceKey);

    try {
      Example example = new Example(VideoPlatformDevice.class);
      Example.Criteria criteria = example.createCriteria();
      criteria.andEqualTo("instanceKey", instanceKey);
      criteria.andEqualTo("enabled", 1);
      if (StrUtil.isNotBlank(keyword)) {
        criteria.andLike("deviceName", "%" + keyword + "%");
      }
      List<VideoPlatformDevice> devices = deviceMapper.selectByExample(example);

      List<Map<String, Object>> result = new ArrayList<>();
      for (VideoPlatformDevice d : devices) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", d.getId());
        item.put("deviceId", d.getDeviceId());
        item.put("deviceName", d.getDeviceName());
        item.put("deviceStatus", d.getDeviceStatus());
        item.put("deviceModel", d.getDeviceModel());
        item.put("manufacturer", d.getManufacturer());
        item.put("deviceIp", d.getDeviceIp());
        result.add(item);
      }
      return R.ok(result);
    } catch (Exception e) {
      log.error("获取设备列表失败: instanceKey={}", instanceKey, e);
      return R.error("获取设备列表失败: " + e.getMessage(), null);
    }
  }

  /**
   * 获取设备通道列表（从video_platform_channel表查询）
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID（可选，不传则查询该实例下所有通道）
   * @param keyword 关键词搜索
   * @return 设备通道列表
   */
  @Operation(summary = "获取设备通道列表", description = "获取设备通道列表（从video_platform_channel表查询）")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功获取设备通道列表",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = R.class)))
  @io.swagger.v3.oas.annotations.Parameter(
      name = "instanceKey",
      description = "平台实例Key",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "deviceId",
      description = "设备ID（可选，不传则查询该实例下所有通道）",
      required = false)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "keyword",
      description = "关键词搜索",
      required = false)
  @GetMapping("/{instanceKey}/channels")
  public R<List<Map<String, Object>>> listChannels(
      @PathVariable("instanceKey") String instanceKey,
      @RequestParam(required = false) String deviceId,
      @RequestParam(required = false) String keyword) {
    assertLogin();
    assertInstancePermission(instanceKey);

    try {
      Example example = new Example(VideoPlatformChannel.class);
      Example.Criteria criteria = example.createCriteria();
      criteria.andEqualTo("instanceKey", instanceKey);
      if (StrUtil.isNotBlank(deviceId)) {
        criteria.andEqualTo("platformDeviceId", deviceId);
      }
      if (StrUtil.isNotBlank(keyword)) {
        criteria.andLike("channelName", "%" + keyword + "%");
      }
      List<VideoPlatformChannel> channels = channelMapper.selectByExample(example);

      List<Map<String, Object>> result = new ArrayList<>();
      for (VideoPlatformChannel ch : channels) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", ch.getId());
        item.put("channelId", ch.getChannelId());
        item.put("channelName", ch.getChannelName());
        item.put("channelStatus", ch.getChannelStatus());
        item.put("channelType", ch.getChannelType());
        item.put("platformDeviceId", ch.getPlatformDeviceId());
        item.put("ptzType", ch.getPtzType());
        item.put("manufacturer", ch.getManufacturer());
        item.put("model", ch.getModel());
        item.put("longitude", ch.getLongitude());
        item.put("latitude", ch.getLatitude());
        item.put("hasAudio", ch.getHasAudio());
        result.add(item);
      }
      return R.ok(result);
    } catch (Exception e) {
      log.error("获取通道列表失败: instanceKey={}", instanceKey, e);
      return R.error("获取通道列表失败: " + e.getMessage(), null);
    }
  }

  // ==================== 控制类接口（通过适配器调用） ====================

  /**
   * 播放通道（获取预览流地址）
   *
   * @param instanceKey 平台实例Key
   * @param channelId 通道ID
   * @param deviceId 设备ID
   * @param streamType 流类型
   * @return 预览流地址信息
   */
  @Operation(
      summary = "播放通道-获取预览流地址",
      description = "播放通道获取预览流地址，返回包含多种格式的流地址：ws_flv, flv, hls(m3u8)")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功获取预览流地址",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = R.class)))
  @io.swagger.v3.oas.annotations.Parameter(
      name = "instanceKey",
      description = "平台实例Key",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "channelId",
      description = "通道ID",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "deviceId",
      description = "设备ID",
      required = false)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "streamType",
      description = "流类型",
      required = false)
  @GetMapping("/{instanceKey}/play/{channelId}")
  public R<Map<String, String>> playChannel(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("channelId") String channelId,
      @RequestParam(required = false) String deviceId,
      @RequestParam(defaultValue = "main") String streamType) {
    assertLogin();
    assertInstancePermission(instanceKey);

    try {
      // 获取平台实例和适配器
      VideoPlatformInstance instance = getInstanceByKey(instanceKey);
      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());

      // 调用适配器获取流地址
      Map<String, String> streamUrls =
          adapter.getStreamUrl(instance, deviceId, channelId, streamType);

      if (streamUrls == null || streamUrls.isEmpty()) {
        return R.error("未获取到视频流地址", null);
      }

      log.info(
          "小程序获取视频流: instanceKey={}, channelId={}, urls={}",
          instanceKey,
          channelId,
          streamUrls.keySet());
      // 如果启用了代理，转换地址
      Map<String, String> finalUrls =
          VideoProxyUtil.convertStreamUrls(streamUrls, videoProxyDomain, videoProxyEnabled);
      return R.ok(finalUrls);
    } catch (Exception e) {
      log.error("获取预览流失败: instanceKey={}, channelId={}", instanceKey, channelId, e);
      return R.error("获取视频流失败: " + e.getMessage(), null);
    }
  }

  /**
   * 停止播放通道
   *
   * @param instanceKey 平台实例Key
   * @param channelId 通道ID
   * @return 停止播放结果
   */
  @Operation(summary = "停止播放通道", description = "停止播放指定通道的视频流")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功停止播放通道",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = R.class)))
  @io.swagger.v3.oas.annotations.Parameter(
      name = "instanceKey",
      description = "平台实例Key",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "channelId",
      description = "通道ID",
      required = true)
  @GetMapping("/{instanceKey}/stop/{channelId}")
  public R<Void> stopChannel(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("channelId") String channelId) {
    assertLogin();
    assertInstancePermission(instanceKey);

    try {
      // 停止播放通常不需要调用适配器，客户端直接断开流即可
      log.info("小程序停止播放: instanceKey={}, channelId={}", instanceKey, channelId);
      return R.ok();
    } catch (Exception e) {
      log.error("停止播放失败", e);
      return R.error("停止播放失败", null);
    }
  }

  /**
   * 查询设备录像列表
   *
   * @param instanceKey 平台实例Key
   * @param channelId 通道ID
   * @param deviceId 设备ID
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 设备录像列表
   */
  @Operation(summary = "查询设备录像列表", description = "根据时间范围查询设备录像列表")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功查询设备录像列表",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = R.class)))
  @io.swagger.v3.oas.annotations.Parameter(
      name = "instanceKey",
      description = "平台实例Key",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "channelId",
      description = "通道ID",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "deviceId",
      description = "设备ID",
      required = false)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "startTime",
      description = "开始时间",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(name = "endTime", description = "结束时间", required = true)
  @GetMapping("/{instanceKey}/records/{channelId}")
  public R<List<Map<String, Object>>> queryRecords(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("channelId") String channelId,
      @RequestParam(required = false) String deviceId,
      @RequestParam String startTime,
      @RequestParam String endTime) {
    assertLogin();
    assertInstancePermission(instanceKey);

    try {
      VideoPlatformInstance instance = getInstanceByKey(instanceKey);
      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());

      long startTs = parseTime(startTime);
      long endTs = parseTime(endTime);

      List<Map<String, Object>> records =
          adapter.queryRecords(instance, deviceId, channelId, startTs, endTs);
      return R.ok(records);
    } catch (Exception e) {
      log.error("查询设备录像失败", e);
      return R.error("查询设备录像失败: " + e.getMessage(), null);
    }
  }

  /**
   * 设备录像回放
   *
   * @param instanceKey 平台实例Key
   * @param channelId 通道ID
   * @param deviceId 设备ID
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 回放地址信息
   */
  @Operation(summary = "设备录像回放", description = "获取设备录像回放地址")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功获取设备回放地址",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = R.class)))
  @io.swagger.v3.oas.annotations.Parameter(
      name = "instanceKey",
      description = "平台实例Key",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "channelId",
      description = "通道ID",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "deviceId",
      description = "设备ID",
      required = false)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "startTime",
      description = "开始时间",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(name = "endTime", description = "结束时间", required = true)
  @GetMapping("/{instanceKey}/playback/{channelId}")
  public R<Map<String, String>> playback(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("channelId") String channelId,
      @RequestParam(required = false) String deviceId,
      @RequestParam String startTime,
      @RequestParam String endTime) {
    assertLogin();
    assertInstancePermission(instanceKey);

    try {
      VideoPlatformInstance instance = getInstanceByKey(instanceKey);
      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());

      long startTs = parseTime(startTime);
      long endTs = parseTime(endTime);

      Map<String, Object> result =
          adapter.getPlaybackUrl(instance, deviceId, channelId, startTs, endTs);

      // 转换为Map<String, String>并进行代理转换
      Map<String, String> streamUrls = new HashMap<>();
      if (result != null) {
        for (Map.Entry<String, Object> entry : result.entrySet()) {
          if (entry.getValue() instanceof String) {
            streamUrls.put(entry.getKey(), (String) entry.getValue());
          }
        }
      }
      Map<String, String> rs =
          VideoProxyUtil.convertStreamUrls(streamUrls, videoProxyDomain, videoProxyEnabled);
      return R.ok(rs);
    } catch (Exception e) {
      log.error("获取设备回放地址失败", e);
      return R.error("获取回放地址失败: " + e.getMessage(), null);
    }
  }

  /**
   * PTZ云台控制
   *
   * @param instanceKey 平台实例Key
   * @param channelId 通道ID
   * @param body 控制参数
   * @return 控制结果
   */
  @Operation(summary = "PTZ云台控制", description = "对指定通道进行PTZ云台控制操作")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功执行PTZ控制",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = R.class)))
  @io.swagger.v3.oas.annotations.Parameter(
      name = "instanceKey",
      description = "平台实例Key",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "channelId",
      description = "通道ID",
      required = true)
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "控制参数",
      required = true,
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = Map.class)))
  @PostMapping("/{instanceKey}/ptz/{channelId}")
  public R<Void> controlPtz(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("channelId") String channelId,
      @RequestBody Map<String, Object> body) {
    assertLogin();
    assertInstancePermission(instanceKey);

    try {
      VideoPlatformInstance instance = getInstanceByKey(instanceKey);
      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());

      String deviceId = (String) body.get("deviceId");
      String command = String.valueOf(body.getOrDefault("command", "stop"));
      int speed = 50;
      try {
        Object s = body.get("speed");
        if (s != null) speed = Integer.parseInt(String.valueOf(s));
      } catch (Exception ignore) {
      }

      adapter.controlPTZ(instance, deviceId, channelId, command, speed);
      log.info(
          "小程序PTZ控制: instanceKey={}, channelId={}, command={}", instanceKey, channelId, command);

      return R.ok();
    } catch (Exception e) {
      log.error("PTZ控制失败", e);
      return R.error("PTZ控制失败: " + e.getMessage(), null);
    }
  }

  // ==================== 云端录像接口（WVP特有） ====================

  /**
   * 查询云端录像列表
   *
   * @param instanceKey 平台实例Key
   * @param channelId 通道ID
   * @param deviceId 设备ID
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param page 页码
   * @param count 每页数量
   * @return 云端录像列表
   */
  @Operation(summary = "查询云端录像列表（WVP平台）", description = "查询云端录像列表，仅支持WVP平台")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功查询云端录像列表",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = R.class)))
  @io.swagger.v3.oas.annotations.Parameter(
      name = "instanceKey",
      description = "平台实例Key",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "channelId",
      description = "通道ID",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "deviceId",
      description = "设备ID",
      required = false)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "startTime",
      description = "开始时间",
      required = false)
  @io.swagger.v3.oas.annotations.Parameter(name = "endTime", description = "结束时间", required = false)
  @io.swagger.v3.oas.annotations.Parameter(name = "page", description = "页码", required = false)
  @io.swagger.v3.oas.annotations.Parameter(name = "count", description = "每页数量", required = false)
  @GetMapping("/{instanceKey}/cloud-records/{channelId}")
  public R<Map<String, Object>> queryCloudRecords(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("channelId") String channelId,
      @RequestParam(required = false) String deviceId,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      @RequestParam(required = false, defaultValue = "1") String page,
      @RequestParam(required = false, defaultValue = "20") String count) {
    assertLogin();
    assertInstancePermission(instanceKey);

    return R.error("开源版本不支持云端录像查询", null);
  }

  /**
   * 云端录像播放
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param channelId 通道ID
   * @param data 加载参数（包含app、stream、cloudRecordId等）
   * @return 视频流地址
   */
  @Operation(summary = "云端录像播放（WVP平台）", description = "加载云端录像进行实时播放，仅支持WVP平台")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功加载云端录像文件",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = R.class)))
  @io.swagger.v3.oas.annotations.Parameter(
      name = "instanceKey",
      description = "平台实例Key",
      required = true)
  @io.swagger.v3.oas.annotations.Parameter(name = "deviceId", description = "设备ID", required = true)
  @io.swagger.v3.oas.annotations.Parameter(
      name = "channelId",
      description = "通道ID",
      required = true)
  @PostMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/cloud-records/load")
  public R<Map<String, Object>> loadCloudRecordPlay(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestBody Map<String, Object> data) {
    assertLogin();
    assertInstancePermission(instanceKey);

    return R.error("开源版本不支持云端录像播放", null);
  }

  // ==================== 辅助方法 ====================

  /** 断言用户已登录 */
  private void assertLogin() {
    MpUserContext.assertLogin();
  }

  /** 获取当前unionId */
  private String getCurrentUnionId() {
    return MpUserContext.requireUnionId();
  }

  /** 检查视频实例访问权限 */
  private void assertInstancePermission(String instanceKey) {
    String unionId = getCurrentUnionId();
    if (StrUtil.isBlank(unionId)) {
      throw new IoTException("无法获取应用授权信息");
    }
    // 这里可以添加更多权限检查逻辑
    log.debug("视频实例权限检查: instanceKey={}, unionId={}", instanceKey, unionId);
  }

  /** 根据instanceKey和channelId获取通道信息 */
  private VideoPlatformChannel getChannelById(String instanceKey, String channelId) {
    try {
      Example example = new Example(VideoPlatformChannel.class);
      example
          .createCriteria()
          .andEqualTo("instanceKey", instanceKey)
          .andEqualTo("channelId", channelId);
      List<VideoPlatformChannel> channels = channelMapper.selectByExample(example);
      return channels.isEmpty() ? null : channels.get(0);
    } catch (Exception e) {
      log.warn("获取通道信息失败: instanceKey={}, channelId={}", instanceKey, channelId);
      return null;
    }
  }

  /** 根据instanceKey获取平台实例 */
  private VideoPlatformInstance getInstanceByKey(String instanceKey) {
    VideoPlatformInstance query = new VideoPlatformInstance();
    query.setInstanceKey(instanceKey);
    VideoPlatformInstance instance = instanceMapper.selectOne(query);
    if (instance == null) {
      throw new IoTException("平台实例不存在：" + instanceKey);
    }
    return instance;
  }

  /** 解析时间字符串 */
  private long parseTime(String timeStr) {
    try {
      // 支持格式：yyyy-MM-dd HH:mm:ss 或时间戳（毫秒/秒）
      if (timeStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.parse(timeStr).getTime();
      } else {
        long timestamp = Long.parseLong(timeStr);
        // 如果是10位秒级时间戳，转换为毫秒
        if (timeStr.length() == 10) {
          timestamp = timestamp * 1000;
        }
        return timestamp;
      }
    } catch (Exception e) {
      throw new IoTException("时间格式错误: " + timeStr);
    }
  }
}
