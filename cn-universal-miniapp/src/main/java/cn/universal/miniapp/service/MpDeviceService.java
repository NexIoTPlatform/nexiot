/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.common.domain.R;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.miniapp.dto.MpDeviceQueryDTO;
import cn.universal.miniapp.entity.MpApp;
import cn.universal.miniapp.entity.MpUserPermission;
import cn.universal.miniapp.mapper.MpAppMapper;
import cn.universal.miniapp.mapper.MpDeviceMapper;
import cn.universal.miniapp.mapper.MpUserDeviceMapper;
import cn.universal.miniapp.mapper.MpUserPermissionMapper;
import cn.universal.miniapp.vo.MpDashboardVO;
import cn.universal.miniapp.vo.MpDeviceVO;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 小程序设备服务 设备通过 mp_app.linked_app_id ↔ iot_device.application 关联查询
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@Service
public class MpDeviceService {

  @Autowired private MpAppMapper mpAppMapper;

  @Autowired private MpDeviceMapper mpDeviceMapper;

  @Autowired private IoTDeviceShadowService ioTDeviceShadowService;

  @Autowired private IoTDeviceService ioTDeviceService;

  @Autowired private IoTUserApplicationMapper ioTUserApplicationMapper;

  @Autowired private MpUserPermissionMapper mpUserPermissionMapper;

  @Autowired private MpUserDeviceMapper mpUserDeviceMapper;

  /** 查询设备列表（支持按用户与权限策略过滤） */
  public List<MpDeviceVO> getDeviceListForUser(String appId, Long userId, MpDeviceQueryDTO queryDTO) {
    // 未登录用户或缺少userId时，回退到应用级列表（理论上小程序端应总是带userId）
    if (userId == null) {
      return getDeviceListWithQuery(appId, queryDTO);
    }

    // 拥有DEVICE_VIEW_ALL权限的用户视为管理员，使用旧的全量模式
    boolean isAdmin =
        mpUserPermissionMapper.hasPermission(
            userId, appId, MpUserPermission.PermissionCode.DEVICE_VIEW_ALL);
    if (isAdmin) {
      return getDeviceListWithQuery(appId, queryDTO);
    }

    String linkedAppId = getLinkedAppId(appId);
    if (linkedAppId == null) {
      return new ArrayList<>();
    }

    // 查询该用户在当前小程序下被授权的设备ID列表
    java.util.List<String> iotIds = mpUserDeviceMapper.selectIotIdsByUser(appId, userId);
    if (iotIds == null || iotIds.isEmpty()) {
      // 非管理员且未配置任何设备授权时，返回空列表
      return new ArrayList<>();
    }

    // 构造查询条件，追加用户授权的设备范围
    if (queryDTO == null) {
      queryDTO = new MpDeviceQueryDTO();
    }
    queryDTO.setLinkedAppId(linkedAppId);
    queryDTO.setIotIds(iotIds);

    List<MpDeviceVO> devices = mpDeviceMapper.selectDeviceListByQuery(queryDTO);
    devices.forEach(this::parseProductImage);
    return devices;
  }

  /** 查询设备列表（带查询条件，分页） */
  public PageInfo<MpDeviceVO> getDevicePageWithQuery(String appId, MpDeviceQueryDTO queryDTO) {
    // 获取关联的IoT应用ID
    String linkedAppId = getLinkedAppId(appId);
    if (linkedAppId == null) {
      return new PageInfo<>(new ArrayList<>());
    }

    // 设置查询条件
    if (queryDTO == null) {
      queryDTO = new MpDeviceQueryDTO();
    }
    if (queryDTO.getPageNum() == null || queryDTO.getPageNum() <= 0) {
      queryDTO.setPageNum(1);
    }
    if (queryDTO.getPageSize() == null || queryDTO.getPageSize() <= 0) {
      queryDTO.setPageSize(10);
    }
    queryDTO.setLinkedAppId(linkedAppId);

    // 启动分页后立即执行查询（避免中间嵌套调用导致分页失效）
    PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
    List<MpDeviceVO> devices = mpDeviceMapper.selectDeviceListByQuery(queryDTO);

    // 处理产品图片
    devices.forEach(this::parseProductImage);

    return new PageInfo<>(devices);
  }
 
  /** 查询设备列表 通过 mp_app.linked_app_id 关联查询 iot_device.application */
  public List<MpDeviceVO> getDeviceList(String appId) {
    String linkedAppId = getLinkedAppId(appId);
    if (linkedAppId == null) {
      return new ArrayList<>();
    }

    // 使用专用 Mapper 查询
    List<MpDeviceVO> devices = mpDeviceMapper.selectDeviceListByAppId(linkedAppId);

    // 处理产品图片 JSON 格式
    devices.forEach(this::parseProductImage);

    return devices;
  }

  /** 查询设备列表（带查询条件） */
  public List<MpDeviceVO> getDeviceListWithQuery(String appId, MpDeviceQueryDTO queryDTO) {
    String linkedAppId = getLinkedAppId(appId);
    if (linkedAppId == null) {
      return new ArrayList<>();
    }

    // 设置linkedAppId
    if (queryDTO == null) {
      queryDTO = new MpDeviceQueryDTO();
    }
    queryDTO.setLinkedAppId(linkedAppId);

    // 使用专用 Mapper 查询
    List<MpDeviceVO> devices = mpDeviceMapper.selectDeviceListByQuery(queryDTO);

    // 处理产品图片 JSON 格式
    devices.forEach(this::parseProductImage);

    return devices;
  }

  /** 查询设备列表（带属性） */
  public List<MpDeviceVO> getDeviceListWithProperties(String appId) {
    List<MpDeviceVO> devices = getDeviceList(appId);

    // 批量获取设备属性（限制前3个属性）
    for (MpDeviceVO device : devices) {
      try {
        List<IoTDevicePropertiesBO> props = ioTDeviceShadowService.getDevState(device.getIotId());
        if (props != null) {
          device.setProperties(props);
        } else {
          device.setProperties(new ArrayList<>());
        }
      } catch (Exception e) {
        log.debug("获取设备属性失败: iotId={}", device.getIotId());
      }
    }

    return devices;
  }

  /** 获取设备详情 */
  public MpDeviceVO getDeviceDetail(String iotId) {
    MpDeviceVO device = mpDeviceMapper.selectDeviceByIotId(iotId);
    if (device != null) {
      parseProductImage(device);
      // 获取设备属性
      try {
        List<IoTDevicePropertiesBO> props = ioTDeviceShadowService.getDevState(iotId);
        if (props != null) {
          device.setProperties(props);
        } else {
          device.setProperties(new ArrayList<>());
        }
      } catch (Exception e) {
        log.warn("获取设备属性失败: iotId={}", iotId);
      }

      // 判断设备类型
      if ("GATEWAY_SUB_DEVICE".equals(device.getDeviceNode())) {
        // 子设备：查询网关设备信息
        if (StrUtil.isNotBlank(device.getGwProductKey())
            && StrUtil.isNotBlank(device.getExtDeviceId())) {
          IoTDevice gwDevice =
              ioTDeviceService.selectDevInstance(device.getGwProductKey(), device.getExtDeviceId());
          if (ObjectUtil.isNotNull(gwDevice)) {
            device.setGwDevice(gwDevice);
          }
        }
      } else if ("GATEWAY".equals(device.getDeviceNode())) {
        // 网关设备：查询子设备列表
        if (StrUtil.isNotBlank(device.getProductKey())
            && StrUtil.isNotBlank(device.getDeviceId())) {
          List<MpDeviceVO> subDevices =
              mpDeviceMapper.selectSubDevicesByGateway(
                  device.getProductKey(), device.getDeviceId());
          if (subDevices != null && !subDevices.isEmpty()) {
            // 处理子设备的产品图片
            subDevices.forEach(this::parseProductImage);
            device.setSubDevices(subDevices);
          }
        }
      }
    }
    return device;
  }

  /** 获取仪表盘数据 */
  public MpDashboardVO getDashboard(String appId) {
    String linkedAppId = getLinkedAppId(appId);
    if (linkedAppId == null) {
      return MpDashboardVO.builder()
          .totalDevices(0)
          .onlineDevices(0)
          .offlineDevices(0)
          .alarmCount(0)
          .build();
    }

    int total = mpDeviceMapper.countDevicesByAppId(linkedAppId);
    int online = mpDeviceMapper.countOnlineDevicesByAppId(linkedAppId);

    return MpDashboardVO.builder()
        .totalDevices(total)
        .onlineDevices(online)
        .offlineDevices(total - online)
        .alarmCount(0)
        .build();
  }

  /** 获取设备数量 */
  public int getDeviceCount(String appId) {
    String linkedAppId = getLinkedAppId(appId);
    if (linkedAppId == null) {
      return 0;
    }
    return mpDeviceMapper.countDevicesByAppId(linkedAppId);
  }

  public int getDeviceOnlineCount(String appId) {
    String linkedAppId = getLinkedAppId(appId);
    if (linkedAppId == null) {
      return 0;
    }
    return mpDeviceMapper.countOnlineDevicesByAppId(linkedAppId);
  }

  /** 获取小程序关联的IoT应用ID */
  private String getLinkedAppId(String appId) {
    MpApp mpApp = mpAppMapper.selectByAppId(appId);
    if (mpApp == null) {
      log.warn("小程序不存在: appId={}", appId);
      return null;
    }
    return mpApp.getLinkedAppId();
  }

  /** 解析产品图片 photoUrl 格式: {"img":"xxx","detail":""} 或直接 URL */
  private void parseProductImage(MpDeviceVO device) {
    String photoUrl = device.getProductImage();
    if (StrUtil.isBlank(photoUrl)) {
      return;
    }

    try {
      if (photoUrl.startsWith("{")) {
        JSONObject json = JSONUtil.parseObj(photoUrl);
        device.setProductImage(json.getStr("img"));
      }
    } catch (Exception e) {
      log.debug("解析产品图片失败: {}", photoUrl);
    }
  }

  /** 格式化属性值 */
  private String formatPropertyValue(Object value) {
    if (value == null) {
      return "--";
    }
    if (value instanceof Double) {
      return String.format("%.1f", (Double) value);
    }
    if (value instanceof Float) {
      return String.format("%.1f", (Float) value);
    }
    return String.valueOf(value);
  }

  /** 获取产品图片（公开方法，供Controller调用） */
  public String getProductImage(String productKey) {
    MpDeviceVO device = new MpDeviceVO();
    // 查询产品图片
    List<MpDeviceVO> devices = mpDeviceMapper.selectDeviceListByAppId(null);
    for (MpDeviceVO d : devices) {
      if (productKey.equals(d.getProductKey())) {
        parseProductImage(d);
        return d.getProductImage();
      }
    }
    return null;
  }

  /**
   * 功能调用
   *
   * @param appId 小程序ID
   * @param iotId 设备ID
   * @param functionId 功能ID
   * @param params 参数
   * @return 调用结果
   */
  public R callFunction(String appId, String iotId, String functionId, JSONObject params) {
    try {
      // 1. 获取小程序信息，以获取creatorId作为unionId
      MpApp mpApp = mpAppMapper.selectByAppId(appId);
      if (mpApp == null) {
        log.warn("小程序不存在: appId={}", appId);
        return R.error("小程序不存在");
      }

      // 2. 获取设备信息
      IoTDeviceDTO deviceDTO = ioTDeviceService.selectDevInstanceBO(iotId);
      if (deviceDTO == null) {
        log.warn("设备不存在: iotId={}", iotId);
        return R.error("设备不存在");
      }

      // 3. 验证设备是否属于该小程序
      String linkedAppId = mpApp.getLinkedAppId();
      if (StrUtil.isBlank(linkedAppId) || !linkedAppId.equals(deviceDTO.getApplicationId())) {
        log.warn(
            "设备不属于该小程序: iotId={}, appId={}, linkedAppId={}, deviceAppId={}",
            iotId,
            appId,
            linkedAppId,
            deviceDTO.getApplicationId());
        return R.error("无权操作该设备");
      }
      IoTUserApplication application =
          ioTUserApplicationMapper.selectIotUserApplicationById(linkedAppId);
      if (application == null || StrUtil.isBlank(application.getUnionId())) {
        log.warn("小程序没有创建者: appId={}", appId);
        return R.error("小程序配置异常");
      }
      // 4. 构建功能调用数据
      JSONObject functionData = new JSONObject();
      functionData.set("function", functionId);
      functionData.set("data", params == null ? new JSONObject() : params);

      // 5. 构建下行指令
      JSONObject command = new JSONObject();
      command.set("function", functionData);
      command.set("messageType", MessageType.FUNCTIONS);
      command.set("iotId", deviceDTO.getIotId());
      command.set("appUnionId", application.getUnionId());
      command.set("productKey", deviceDTO.getProductKey());
      command.set("deviceId", deviceDTO.getDeviceId());
      command.set("cmd", DownCmd.DEV_FUNCTION);
      command.set("applicationId", linkedAppId);
      command.set("tenantId", deviceDTO.getTenantId());

      // 6. 转换为UnifiedDownlinkCommand
      UnifiedDownlinkCommand downlinkCommand = UnifiedDownlinkCommand.fromJson(command);

      // 7. 调用下行接口
      R result = IoTDownlFactory.getIDown(deviceDTO.getThirdPlatform()).doAction(downlinkCommand);

      log.info(
          "功能调用: appId={}, unionId={}, iotId={}, functionId={}, params={}, result={}",
          appId,
          application.getUnionId(),
          iotId,
          functionId,
          params,
          result);

      return result;
    } catch (Exception e) {
      log.error("功能调用异常", e);
      return R.error("功能调用失败: " + e.getMessage());
    }
  }
}
