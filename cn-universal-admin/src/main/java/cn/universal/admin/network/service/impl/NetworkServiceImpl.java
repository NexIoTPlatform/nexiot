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

package cn.universal.admin.network.service.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.network.service.INetworkService;
import cn.universal.admin.network.utils.NetworkTypeUtil;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.constant.NetworkTypeConstants;
import cn.universal.common.domain.R;
import cn.universal.common.enums.NetworkType;
import cn.universal.common.exception.IoTException;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.device.service.protocol.ProtocolClusterService;
import cn.universal.dm.device.service.protocol.ProtocolServerManager;
import cn.universal.mqtt.protocol.third.ThirdMQTTServerManager;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.Network;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.entity.bo.IoTProductBO;
import cn.universal.persistence.entity.bo.NetworkBO;
import cn.universal.persistence.entity.vo.IoTProductVO;
import cn.universal.persistence.entity.vo.NetworkVO;
import cn.universal.persistence.mapper.IoTDeviceFenceRelMapper;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.NetworkMapper;
import cn.universal.persistence.query.NetworkQuery;
import cn.universal.security.utils.SecurityUtils;
import cn.universal.websocket.protocol.manager.IWebSocketServerManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import tk.mybatis.mapper.entity.Example;

/**
 * 网络组件Service实现类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
@Service
public class NetworkServiceImpl implements INetworkService {

  @Resource private NetworkMapper networkMapper;
  @Resource private IoTDeviceMapper ioTDeviceMapper;
  @Resource private IoTProductMapper ioTProductMapper;
  @Resource private IoTDeviceFenceRelMapper ioTDeviceFenceRelMapper;

  // 直接注入 TCP 和 MQTT 服务
  @Qualifier("tcpClusterService")
  @Autowired(required = false)
  private ProtocolClusterService tcpClusterService;

  @Autowired(required = false)
  private ProtocolServerManager tcpServerManager;

  @Qualifier("udpClusterService")
  @Autowired(required = false)
  private ProtocolClusterService udpClusterService;

  @Autowired(required = false)
  private ThirdMQTTServerManager mqttServerManager;

  @Autowired(required = false)
  private IWebSocketServerManager webSocketServerManager;

  @Override
  public boolean del(String productKey) {
    Example ex = new Example(Network.class);
    ex.createCriteria().andEqualTo("productKey", productKey);
    return networkMapper.deleteByExample(ex) > 0;
  }

  @Override
  public List<NetworkVO> selectNetworkList(NetworkBO bo) {
    return networkMapper.selectNetworkListV1(bo);
  }

  @Override
  public List<IoTDevice> queryMileSightList(NetworkBO bo) {
    return ioTDeviceMapper.queryMileSightList(bo);
  }

  @Override
  public R insertDevInstance(IoTDeviceBO devInstancebo) {
    JSONObject downObj = new JSONObject();
    // 预添加归属为空
    downObj.set("appUnionId", "");
    // 产品编号
    downObj.set("productKey", devInstancebo.getProductKey());
    // 设备序列号
    downObj.set("deviceId", devInstancebo.getDeviceId());
    // Lora密匙
    downObj.set("deviceKey", devInstancebo.getSecretKey());
    // 添加
    downObj.set("cmd", DownCmd.DEV_ADD.name());
    // 说明
    downObj.set("detail", devInstancebo.getDetail());
    JSONObject ob = new JSONObject();
    // 设备实例名称
    ob.set("deviceName", devInstancebo.getDeviceName());
    // 设备序列号
    ob.set("imei", devInstancebo.getDeviceId());
    // 维度
    ob.set("latitude", devInstancebo.getLatitude());
    // 经度
    ob.set("longitude", devInstancebo.getLongitude());
    // 添加额外参数
    for (Map.Entry<String, Object> entry : devInstancebo.getOtherParams().entrySet()) {
      ob.set(entry.getKey(), entry.getValue());
    }
    downObj.set("data", ob);
    IoTProduct ioTProduct = ioTProductMapper.getProductByProductKey(devInstancebo.getProductKey());
    // 转换为UnifiedDownlinkCommand（保持所有参数）
    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(downObj);
    return IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);
  }

  @Override
  public R deleteDevInstanceByIds(String[] ids) {
    R result = new R();
    // 获取网关eui
    String id1 = ids[0];
    IoTDevice ioTDevice1 = ioTDeviceMapper.selectDevInstanceById(id1);
    String gatewayEUI = JSONUtil.parseObj(ioTDevice1.getConfiguration()).getStr("gatewayEUI");
    if (StrUtil.isBlank(gatewayEUI)) {
      return R.error("删除失败，缺少网关EUI");
    }
    for (String id : ids) {
      IoTDevice ioTDevice = ioTDeviceMapper.selectDevInstanceById(id);
      JSONObject downRequest = new JSONObject();
      downRequest.set("appUnionId", "");
      downRequest.set("productKey", ioTDevice.getProductKey());
      downRequest.set("deviceId", ioTDevice.getDeviceId());
      downRequest.set("cmd", DownCmd.DEV_DEL.name());
      JSONObject ob = new JSONObject();
      ob.set("gatewayEUI", gatewayEUI);
      downRequest.set("data", ob);
      IoTProduct ioTProduct = ioTProductMapper.getProductByProductKey(ioTDevice.getProductKey());
      // 转换为UnifiedDownlinkCommand（保持所有参数）
      UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(downRequest);
      result = IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);
      if (!result.getCode().equals(0)) {
        return R.error("设备名称: " + ioTDevice.getDeviceName() + " 删除失败，原因: " + result.getMsg());
      }
      ioTDeviceFenceRelMapper.deleteFenceInstance(ioTDevice.getIotId());
    }
    return R.ok("删除成功");
  }

  @Override
  public List<NetworkBO> selectNetworkList(NetworkQuery query) {
    // 验证类型参数
    // 如果type参数不为空，将其转换为types列表
    if (StrUtil.isNotBlank(query.getType())) {
      List<String> types = NetworkTypeUtil.parseTypes(query.getType());

      // 验证所有类型是否有效
      if (!NetworkTypeUtil.isValidTypes(types)) {
        List<String> invalidTypes = NetworkTypeUtil.getInvalidTypes(types);
        throw new IoTException("无效的网络类型: " + String.join(", ", invalidTypes));
      }
      // 设置types列表，清空type字段
      query.setTypes(types);
      query.setType(null);
    }
    // 计算分页参数
    Page<IoTProductVO> page = PageHelper.startPage(query.getPageNum(), query.getPageSize());
    List<NetworkBO> list = networkMapper.selectNetworkList(query);
    for (NetworkBO bo : list) {
      // 启用/停用状态
      bo.setEnableName(Boolean.TRUE.equals(bo.getState()) ? "已启用" : "已停用");
      // 实际运行状态
      String type = bo.getType();
      String productKey = bo.getProductKey();
      String unionId = bo.getUnionId();
      if (NetworkType.TCP_CLIENT.getId().equals(type)
          || NetworkType.TCP_SERVER.getId().equals(type)) {
        if (tcpClusterService != null && tcpServerManager != null) {
          Object serverInstance = tcpServerManager.getServerInstance(productKey);
          if (serverInstance != null && tcpServerManager.isAlive(serverInstance)) {
            bo.setStateName("已启动");
            bo.setRunning(Boolean.TRUE);
          } else {
            bo.setStateName("未启动");
            bo.setRunning(Boolean.FALSE);
          }
        } else {
          bo.setStateName("未启动");
          bo.setRunning(Boolean.FALSE);
        }
        // 查出哪些TCP-Server绑定了产品
        if (NetworkType.TCP_SERVER.getId().equals(type)) {
          IoTProductBO ioTProductBO =
              ioTProductMapper.selectTcpProductsUseNetwork(bo.getProductKey());
          bo.setBindTcpServerProductCount(ioTProductBO == null ? 0 : 1);
          bo.setBindTcpServerProducts(ioTProductBO);
        }

      } else if (NetworkType.MQTT_CLIENT.getId().equals(type)
          || NetworkType.MQTT_SERVER.getId().equals(type)) {
        List<IoTProductBO> ioTProductBOS = ioTProductMapper.selectMqttProductsUseNetwork(unionId);
        bo.setBindMqttServerProductCount(CollUtil.size(ioTProductBOS));
        bo.setBindMqttServerProducts(ioTProductBOS);
        if (mqttServerManager != null && mqttServerManager.isConnected(unionId)) {
          bo.setStateName("已启动");
          bo.setRunning(Boolean.TRUE);
        } else {
          bo.setStateName("未启动");
          bo.setRunning(Boolean.FALSE);
        }
      } else if (NetworkType.UDP.getId().equals(type)) {
        if (udpClusterService != null && udpClusterService.isProductServerAlive(productKey)) {
          bo.setStateName("已启动");
          bo.setRunning(Boolean.TRUE);
        } else {
          bo.setStateName("未启动");
          bo.setRunning(Boolean.FALSE);
        }
      } else if (NetworkType.WEB_SOCKET_SERVER.getId().equals(type)
          || NetworkType.WEB_SOCKET_CLIENT.getId().equals(type)) {
        // WebSocket 当前通过 state 字段来判断运行状态（临时方案，后续需实现 WebSocketManager）
        if (Boolean.TRUE.equals(bo.getState())) {
          bo.setStateName("已启动");
          bo.setRunning(Boolean.TRUE);
        } else {
          bo.setStateName("未启动");
          bo.setRunning(Boolean.FALSE);
        }
      } else {
        bo.setStateName("未知");
        bo.setRunning(Boolean.FALSE);
      }
    }
    return list;
  }

  @Override
  public NetworkVO selectNetworkById(Integer id) {
    Network network = networkMapper.selectNetworkById(id);
    if (network == null) {
      return null;
    }
    NetworkVO vo = new NetworkVO();
    // 复制基本属性
    vo.setId(network.getId());
    vo.setType(network.getType());
    vo.setUnionId(network.getUnionId());
    vo.setProductKey(network.getProductKey());
    vo.setName(network.getName());
    vo.setDescription(network.getDescription());
    vo.setCreateDate(network.getCreateDate());
    vo.setCreateUser(network.getCreateUser());
    vo.setState(network.getState());
    vo.setConfiguration(network.getConfiguration());
    // 类型名称
    switch (network.getType()) {
      case NetworkTypeConstants.TCP_CLIENT:
        vo.setTypeName(NetworkType.TCP_CLIENT.getDescription());
        break;
      case NetworkTypeConstants.TCP_SERVER:
        vo.setTypeName(NetworkType.TCP_SERVER.getDescription());
        break;
      case NetworkTypeConstants.MQTT_CLIENT:
        vo.setTypeName(NetworkType.MQTT_CLIENT.getDescription());
        break;
      case NetworkTypeConstants.MQTT_SERVER:
        vo.setTypeName(NetworkType.MQTT_SERVER.getDescription());
        break;
      case NetworkTypeConstants.UDP:
        vo.setTypeName(NetworkType.UDP.getDescription());
        break;
      case NetworkTypeConstants.WEB_SOCKET_SERVER:
        vo.setTypeName(NetworkType.WEB_SOCKET_SERVER.getDescription());
        break;
      case NetworkTypeConstants.WEB_SOCKET_CLIENT:
        vo.setTypeName(NetworkType.WEB_SOCKET_CLIENT.getDescription());
        break;
      default:
        vo.setTypeName(network.getType());
    }
    // 启用/停用状态
    vo.setEnableName(Boolean.TRUE.equals(network.getState()) ? "已启用" : "已停用");
    // 实际运行状态和绑定产品
    String type = network.getType();
    String productKey = network.getProductKey();
    String unionId = network.getUnionId();
    if (NetworkType.TCP_CLIENT.getId().equals(type)
        || NetworkType.TCP_SERVER.getId().equals(type)) {
      if (tcpClusterService != null && tcpServerManager != null) {
        Object serverInstance = tcpServerManager.getServerInstance(productKey);
        if (serverInstance != null && tcpServerManager.isAlive(serverInstance)) {
          vo.setStateName("已启动");
          vo.setRunning(Boolean.TRUE);
        } else {
          vo.setStateName("未启动");
          vo.setRunning(Boolean.FALSE);
        }
      } else {
        vo.setStateName("未启动");
        vo.setRunning(Boolean.FALSE);
      }
      // TCP类型查询绑定产品
      if (NetworkType.TCP_SERVER.getId().equals(type)) {
        IoTProductBO ioTProductBO =
            ioTProductMapper.selectTcpProductsUseNetwork(productKey);
        vo.setBindTcpServerProductCount(ioTProductBO == null ? 0 : 1);
        vo.setBindTcpServerProducts(ioTProductBO);
      }

      } else if (NetworkType.MQTT_CLIENT.getId().equals(type)
          || NetworkType.MQTT_SERVER.getId().equals(type)) {
        // 查询MQTT绑定产品
        List<IoTProductBO> ioTProductBOS = ioTProductMapper.selectMqttProductsUseNetwork(unionId);
        vo.setBindMqttServerProductCount(CollUtil.size(ioTProductBOS));
        vo.setBindMqttServerProducts(ioTProductBOS);
        // 查询运行状态
        if (mqttServerManager != null && mqttServerManager.isConnected(unionId)) {
          vo.setStateName("已启动");
          vo.setRunning(Boolean.TRUE);
        } else {
          vo.setStateName("未启动");
          vo.setRunning(Boolean.FALSE);
        }
      } else if (NetworkType.UDP.getId().equals(type)) {
      if (udpClusterService != null && udpClusterService.isProductServerAlive(productKey)) {
        vo.setStateName("已启动");
        vo.setRunning(Boolean.TRUE);
      } else {
        vo.setStateName("未启动");
        vo.setRunning(Boolean.FALSE);
      }
    } else {
      vo.setStateName("未知");
      vo.setRunning(Boolean.FALSE);
    }
    return vo;
  }

  /** 检查端口是否被占用或为常用端口 */
  private void checkPortAvailable(Network network, boolean isUpdate) {
    if (StrUtil.isBlank(network.getConfiguration())) {
      return;
    }
    JSONObject config = JSONUtil.parseObj(network.getConfiguration());
    Integer port = config.getInt("port");
    if (port == null) {
      return;
    }
    // 常用端口
    Set<Integer> commonPorts = new HashSet<>();
    int[] arr = {
      80, 443, 3306, 6379, 8080, 22, 21, 25, 53, 123, 1521, 5432, 8888, 9000, 5000, 7001, 27017
    };
    for (int p : arr) {
      commonPorts.add(p);
    }
    if (commonPorts.contains(port)) {
      throw new RuntimeException("端口 " + port + " 为常用端口，禁止使用");
    }
    // 检查数据库是否被占用
    Integer excludeId = isUpdate ? network.getId() : null;
    int count = networkMapper.selectTcpNetworkByPortCount(port, excludeId);
    if (count > 0) {
      throw new RuntimeException("端口 " + port + " 已被其他网络组件占用");
    }
  }

  @Override
  @Transactional
  public int insertNetwork(Network network) {
    // 验证配置
    if (!validateNetworkConfig(network)) {
      throw new RuntimeException("网络组件配置验证失败");
    }

    // 检查配置唯一性
    checkConfigurationUniqueness(network, null);

    // 设置默认值
    if (network.getState() == null) {
      network.setState(false);
    }

    // 设置创建用户（如果未设置）
    if (StrUtil.isBlank(network.getCreateUser())) {
      // 从当前登录用户获取 unionId
      network.setCreateUser(SecurityUtils.getUnionId());
    }
    network.setUnionId(IdUtil.objectId());
    network.setState(false);

    // 新增TCP_SERVER/UDP时，必须指定productKey，并且不能重复绑定
    if (NetworkType.TCP_SERVER.getId().equals(network.getType())
        || NetworkType.TCP_CLIENT.getId().equals(network.getType())
        || NetworkType.UDP.getId().equals(network.getType())) {
      if (StrUtil.isBlank(network.getProductKey())) {
        throw new RuntimeException("TCP/UDP服务组件必须选择产品");
      }
      network.setUnionId(network.getProductKey());
      // 检查是否已被绑定
      int refCount = ioTProductMapper.countByNetworkUnionId(network.getProductKey());
      if (refCount > 0) {
        throw new RuntimeException("该产品已绑定其他TCP服务组件，不能重复绑定");
      }
    }
    checkPortAvailable(network, false);
    int result = networkMapper.insertNetwork(network);
    // 新增成功后，若为TCP_SERVER，更新iot_product表
    if (result > 0 && NetworkType.TCP_SERVER.getId().equals(network.getType())) {
      ioTProductMapper.updateNetworkUnionIdByProductKey(
          network.getProductKey(), network.getProductKey());
    }
    return result;
  }

  @Override
  @Transactional
  public int updateNetwork(Network network) {
    if (network == null || network.getId() == null) {
      throw new RuntimeException("网络组件配置验证失败");
    }
    // 验证配置
    if (!validateNetworkConfig(network)) {
      throw new RuntimeException("网络组件配置验证失败");
    }
    // 检查权限（只能操作自己创建的网络组件，除非是管理员）
    Network existNetwork = networkMapper.selectNetworkById(network.getId());
    if (existNetwork == null) {
      throw new RuntimeException("网络组件不存在");
    }
    IoTUser currentUser = SecurityUtils.getIoTUnionUser();
    if (!existNetwork.getCreateUser().equals(currentUser.getUnionId()) && !currentUser.isAdmin()) {
      throw new RuntimeException("不能操作不是自己的网络组件");
    }

    String type = existNetwork.getType();
    if ((NetworkType.TCP_SERVER.getId().equals(type)
        || NetworkType.TCP_CLIENT.getId().equals(type)
        || NetworkType.UDP.getId().equals(type)
        || NetworkType.WEB_SOCKET_SERVER.getId().equals(type)
        || NetworkType.WEB_SOCKET_CLIENT.getId().equals(type))) {
      // 只有当unionId发生变化时，才做"已被产品关联且产品下有设备时禁止修改"的校验
      String oldProductKey = existNetwork.getProductKey();
      String networkProductKey = network.getProductKey();
      String current = ioTProductMapper.findProductKeyByNetworkUnionId(oldProductKey);
      if (current != null && !oldProductKey.equals(networkProductKey)) {
        int deviceCount = ioTDeviceMapper.countByProductKey(oldProductKey);
        if (deviceCount > 0) {
          throw new RuntimeException("原关联产品有设备，无法变更");
        }
      }
    }
    // 检查配置唯一性（排除自己）
    checkConfigurationUniqueness(network, network.getId());
    checkPortAvailable(network, true);
    int count = networkMapper.updateNetwork(network);
    if (count > 0 && (NetworkType.TCP_SERVER.getId().equals(network.getType()) ||
                      NetworkType.WEB_SOCKET_SERVER.getId().equals(network.getType()) ||
                      NetworkType.WEB_SOCKET_CLIENT.getId().equals(network.getType()))) {
      ioTProductMapper.updateNetworkUnionIdByProductKey(
          network.getProductKey(), network.getProductKey());
    }
    return count;
  }

  @Override
  @Transactional
  public int deleteNetworkById(Integer id) {
    Network network = networkMapper.selectNetworkById(id);
    if (network == null) {
      throw new RuntimeException("网络组件不存在");
    }
    String type = network.getType();
    // TCP、UDP 和 WebSocket 都需要做一对一检查
    if (NetworkType.TCP_SERVER.getId().equals(type)
        || NetworkType.TCP_CLIENT.getId().equals(type)
        || NetworkType.UDP.getId().equals(type)
        || NetworkType.WEB_SOCKET_SERVER.getId().equals(type)
        || NetworkType.WEB_SOCKET_CLIENT.getId().equals(type)) {
      // 查找是否有关联产品
      String productKey = ioTProductMapper.findProductKeyByNetworkUnionId(network.getUnionId());
      if (productKey != null) {
        // 查找该产品下是否有设备
        int deviceCount = ioTDeviceMapper.countByProductKey(productKey);
        if (deviceCount > 0) {
          throw new RuntimeException("该网络组件已被产品关联且产品下存在设备，无法删除");
        }
      }
    }
    int ct = networkMapper.deleteNetworkById(id);
    if (ct <= 0) {
      throw new RuntimeException("删除失败,请重试");
    }
    ioTProductMapper.updateNetworkProductsUseNetwork(network.getUnionId());
    return ct;
  }

  @Override
  @Transactional
  public int deleteNetworkByIds(Integer[] ids) {
    if (ids == null || ids.length == 0) {
      return 0;
    }
    int count = 0;
    for (Integer id : ids) {
      count=  ((INetworkService) AopContext.currentProxy()).deleteNetworkById(id);
      count++;
    }
    return count;
  }

  @Override
  @Transactional
  public int startNetwork(Integer id) {
    Network network = networkMapper.selectNetworkById(id);
    if (network == null) {
      throw new RuntimeException("网络组件不存在");
    }
    String type = network.getType();
    JSONObject config = JSONUtil.parseObj(network.getConfiguration());
    if (NetworkType.TCP_CLIENT.getId().equals(type)
        || NetworkType.TCP_SERVER.getId().equals(type)) {
      Integer port = config.getInt("port");
      if (port == null) {
        throw new RuntimeException("TCP网络组件启动/重启时端口不能为空");
      }
    } else if (NetworkType.MQTT_CLIENT.getId().equals(type)
        || NetworkType.MQTT_SERVER.getId().equals(type)) {
      String host = config.getStr("host");
      if (StrUtil.isBlank(host)) {
        throw new RuntimeException("MQTT网络组件启动/重启时host不能为空");
      }
    } else if (NetworkType.UDP.getId().equals(type)) {
      Integer port = config.getInt("port");
      if (port == null) {
        throw new RuntimeException("UDP网络组件启动/重启时端口不能为空");
      }
    }
    try {
      // 根据网络类型调用相应的启动逻辑
      boolean success = startNetworkByType(network);

      if (success) {
        // 更新数据库状态为已启用（state=true 表示启用）
        network.setState(true);
        networkMapper.updateNetwork(network);
        log.info("启动网络组件成功: {}", network.getName());
        return 1;
      } else {
        throw new RuntimeException("启动失败: " + network.getName());
      }
    } catch (Exception e) {
      log.error("启动网络组件失败: {}", network.getName(), e);
      throw new RuntimeException("启动网络组件失败: " + e.getMessage());
    }
  }

  @Override
  @Transactional
  public int stopNetwork(Integer id) {
    Network network = networkMapper.selectNetworkById(id);
    if (network == null) {
      throw new RuntimeException("网络组件不存在");
    }

    try {
      // 根据网络类型调用相应的停止逻辑
      boolean success = stopNetworkByType(network);
      if (success) {
        // 更新数据库状态为已停用（state=false 表示禁用）
        network.setState(false);
        networkMapper.updateNetwork(network);
        log.info("停止网络组件成功: {}", network.getName());
        return 1;
      } else {
        throw new RuntimeException("停止网络组件失败: " + network.getName());
      }
    } catch (Exception e) {
      log.error("停止网络组件失败: {}", network.getName(), e);
      throw new RuntimeException("停止网络组件失败: " + e.getMessage());
    }
  }

  @Override
  @Transactional
  public int restartNetwork(Integer id) {
    Network network = networkMapper.selectNetworkById(id);
    if (network == null) {
      throw new RuntimeException("网络组件不存在");
    }
    String type = network.getType();
    JSONObject config = JSONUtil.parseObj(network.getConfiguration());
    if (NetworkType.TCP_CLIENT.getId().equals(type)
        || NetworkType.TCP_SERVER.getId().equals(type)) {
      Integer port = config.getInt("port");
      if (port == null) {
        throw new RuntimeException("TCP网络组件启动/重启时端口不能为空");
      }
    } else if (NetworkType.MQTT_CLIENT.getId().equals(type)
        || NetworkType.MQTT_SERVER.getId().equals(type)) {
      String host = config.getStr("host");
      if (StrUtil.isBlank(host)) {
        throw new RuntimeException("MQTT网络组件启动/重启时host不能为空");
      }
    } else if (NetworkType.UDP.getId().equals(type)) {
      Integer port = config.getInt("port");
      if (port == null) {
        throw new RuntimeException("UDP网络组件启动/重启时端口不能为空");
      }
    }
    try {
      // 根据网络类型调用相应的重启逻辑
      boolean success = restartNetworkByType(network);

      if (success) {
        // 更新数据库状态为已启用（重启后状态应该是启用的）
        network.setState(true);
        networkMapper.updateNetwork(network);
        log.info("重启网络组件成功: {}", network.getName());
        return 1;
      } else {
        throw new RuntimeException("重启网络组件失败: " + network.getName());
      }
    } catch (Exception e) {
      log.error("重启网络组件失败: {}", network.getName(), e);
      throw new RuntimeException("重启网络组件失败: " + e.getMessage());
    }
  }

  @Override
  public List<String> getNetworkTypes() {
    return Arrays.asList(
        NetworkType.TCP_CLIENT.getId(),
        NetworkType.TCP_SERVER.getId(),
        NetworkType.MQTT_CLIENT.getId(),
        NetworkType.MQTT_SERVER.getId(),
        NetworkType.UDP.getId());
  }

  /**
   * 验证网络类型是否有效
   *
   * @param type 网络类型
   * @return 是否有效
   */
  private boolean isValidNetworkType(String type) {
    return getNetworkTypes().contains(type);
  }

  /**
   * 验证多个网络类型是否都有效
   *
   * @param types 网络类型列表
   * @return 是否都有效
   */
  private boolean isValidNetworkTypes(List<String> types) {
    return types.stream().allMatch(this::isValidNetworkType);
  }

  @Override
  public boolean validateNetworkConfig(Network network) {
    if (network == null) {
      return false;
    }

    // 验证必填字段
    if (StrUtil.isBlank(network.getType()) || StrUtil.isBlank(network.getName())) {
      return false;
    }

    // 验证配置JSON格式
    if (StrUtil.isNotBlank(network.getConfiguration())) {
      try {
        JSONUtil.parseObj(network.getConfiguration());
      } catch (Exception e) {
        log.warn("网络组件配置JSON格式错误: {}", e.getMessage());
        return false;
      }
    }
    return true;
  }

  /**
   * 检查网络组件配置的唯一性
   *
   * @param network 网络组件
   * @param excludeId 排除的网络组件ID（用于更新时排除自己）
   */
  private void checkConfigurationUniqueness(Network network, Integer excludeId) {
    if (StrUtil.isBlank(network.getConfiguration())) {
      return;
    }

    try {
      JSONObject config = JSONUtil.parseObj(network.getConfiguration());
      String type = network.getType();
      // TCP/UDP类型检查端口唯一性
      if (NetworkType.TCP_CLIENT.getId().equals(type)
          || NetworkType.TCP_SERVER.getId().equals(type)
          || NetworkType.UDP.getId().equals(type)) {
        Integer port = config.getInt("port");
        if (port != null) {
          List<Network> existingNetworks = networkMapper.selectTcpNetworkByPort(port, excludeId);
          if (!existingNetworks.isEmpty()) {
            Network existingNetwork = existingNetworks.get(0);
            throw new RuntimeException(
                "端口 " + port + " 已被 " + existingNetwork.getName() + " 网络组件使用");
          }
        }
      }

      // MQTT类型检查主机和用户名组合唯一性
      if (NetworkType.MQTT_CLIENT.getId().equals(type)
          || NetworkType.MQTT_SERVER.getId().equals(type)) {
        String host = config.getStr("host");
        String username = config.getStr("username");

        if (StrUtil.isNotBlank(host) && StrUtil.isNotBlank(username)) {
          List<Network> existingNetworks =
              networkMapper.selectMqttNetworkByHostAndUsername(host, username, excludeId);
          if (!existingNetworks.isEmpty()) {
            Network existingNetwork = existingNetworks.get(0);
            throw new RuntimeException(
                "主机 "
                    + host
                    + " 和用户名 "
                    + username
                    + " 的组合已被 "
                    + existingNetwork.getName()
                    + " 网络组件使用");
          }
        }
      }
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw e;
      }
      log.warn("检查网络组件配置唯一性时发生错误: {}", e.getMessage());
    }
  }

  /**
   * 根据网络类型启动网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean startNetworkByType(Network network) {
    String type = network.getType();

    try {
      switch (type) {
        case NetworkTypeConstants.TCP_CLIENT:
        case NetworkTypeConstants.TCP_SERVER:
          return startTcpNetwork(network);
        case NetworkTypeConstants.MQTT_CLIENT:
        case NetworkTypeConstants.MQTT_SERVER:
          return startMqttNetwork(network);
        case NetworkTypeConstants.UDP:
          return startUdpNetwork(network);
        case NetworkTypeConstants.WEB_SOCKET_SERVER:
        case NetworkTypeConstants.WEB_SOCKET_CLIENT:
          return startWebSocketNetwork(network);
        default:
          log.warn("不支持的网络类型: {}", type);
          return false;
      }
    } catch (Exception e) {
      log.error("启动网络组件失败: type={}, name={}", type, network.getName(), e);
      return false;
    }
  }

  /**
   * 根据网络类型停止网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean stopNetworkByType(Network network) {
    String type = network.getType();

    try {
      switch (type) {
        case NetworkTypeConstants.TCP_CLIENT:
        case NetworkTypeConstants.TCP_SERVER:
          return stopTcpNetwork(network);
        case NetworkTypeConstants.MQTT_CLIENT:
        case NetworkTypeConstants.MQTT_SERVER:
          return stopMqttNetwork(network);
        case NetworkTypeConstants.UDP:
          return stopUdpNetwork(network);
        case NetworkTypeConstants.WEB_SOCKET_SERVER:
        case NetworkTypeConstants.WEB_SOCKET_CLIENT:
          return stopWebSocketNetwork(network);
        default:
          log.warn("不支持的网络类型: {}", type);
          return false;
      }
    } catch (Exception e) {
      log.error("停止网络组件失败: type={}, name={}", type, network.getName(), e);
      return false;
    }
  }

  /**
   * 根据网络类型重启网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean restartNetworkByType(Network network) {
    String type = network.getType();
    try {
      switch (type) {
        case NetworkTypeConstants.TCP_CLIENT:
        case NetworkTypeConstants.TCP_SERVER:
          return restartTcpNetwork(network);
        case NetworkTypeConstants.MQTT_CLIENT:
        case NetworkTypeConstants.MQTT_SERVER:
          return restartMqttNetwork(network);
        case NetworkTypeConstants.UDP:
          return restartUdpNetwork(network);
        case NetworkTypeConstants.WEB_SOCKET_SERVER:
        case NetworkTypeConstants.WEB_SOCKET_CLIENT:
          return restartWebSocketNetwork(network);
        default:
          log.warn("不支持的网络类型: {}", type);
          return false;
      }
    } catch (Exception e) {
      log.error("重启网络组件失败: type={}, name={}", type, network.getName(), e);
      return false;
    }
  }

  /**
   * 启动TCP网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean startTcpNetwork(Network network) {
    if (tcpClusterService == null) {
      log.error("TCP服务管理器未注入，无法启动TCP网络组件: {}", network.getName());
      throw new IoTException("TCP模块未注入，无法启动TCP网络组件");
    }

    String productKey = network.getProductKey();
    if (StrUtil.isBlank(productKey)) {
      log.error("TCP网络组件缺少productKey: {}", network.getName());
      return false;
    }

    try {
      // 使用新的方法，从数据库加载配置并启动
      boolean success = tcpClusterService.start(productKey);
      if (success) {
        log.info("TCP网络组件启动成功: productKey={}", productKey);
      } else {
        log.error("TCP网络组件启动失败: productKey={}", productKey);
      }
      return success;
    } catch (Exception e) {
      log.error("启动TCP网络组件失败: productKey={}", productKey, e);
      return false;
    }
  }

  /**
   * 停止TCP网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean stopTcpNetwork(Network network) {
    if (tcpClusterService == null) {
      log.error("TCP服务管理器未注入，无法启动TCP网络组件: {}", network.getName());
      throw new IoTException("TCP模块未注入，无法启动TCP网络组件");
    }

    String productKey = network.getProductKey();
    if (StrUtil.isBlank(productKey)) {
      log.error("TCP网络组件缺少productKey: {}", network.getName());
      return false;
    }

    try {
      boolean success = tcpClusterService.stop(productKey);
      if (success) {
        log.info("TCP网络组件停止成功: productKey={}", productKey);
      } else {
        log.error("TCP网络组件停止失败: productKey={}", productKey);
      }
      return success;
    } catch (Exception e) {
      log.error("停止TCP网络组件失败: productKey={}", productKey, e);
      return false;
    }
  }

  /**
   * 重启TCP网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean restartTcpNetwork(Network network) {
    if (tcpClusterService == null) {
      log.error("TCP服务管理器未注入，无法启动TCP网络组件: {}", network.getName());
      throw new IoTException("TCP模块未注入，无法启动TCP网络组件");
    }

    String productKey = network.getProductKey();
    if (StrUtil.isBlank(productKey)) {
      log.error("TCP网络组件缺少productKey: {}", network.getName());
      return false;
    }

    try {
      boolean success = tcpClusterService.restart(productKey);
      if (success) {
        log.info("TCP网络组件重启成功: productKey={}", productKey);
      } else {
        log.error("TCP网络组件重启失败: productKey={}", productKey);
      }
      return success;
    } catch (Exception e) {
      log.error("重启TCP网络组件失败: productKey={}", productKey, e);
      return false;
    }
  }

  /**
   * 启动MQTT网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean startMqttNetwork(Network network) {
    if (mqttServerManager == null) {
      throw new IoTException("MQTT模块未注入，无法启动TCP网络组件");
    }

    String unionId = network.getUnionId();
    if (StrUtil.isBlank(unionId)) {
      log.error("MQTT网络组件缺少unionId: {}", network.getName());
      return false;
    }

    try {
      boolean success = mqttServerManager.startMqttClient(unionId);
      if (success) {
        log.info("MQTT网络组件启动成功: unionId={}", unionId);
      } else {
        log.error("MQTT网络组件启动失败: unionId={}", unionId);
      }
      return success;
    } catch (Exception e) {
      log.error("启动MQTT网络组件失败: unionId={}", unionId, e);
      return false;
    }
  }

  /**
   * 停止MQTT网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean stopMqttNetwork(Network network) {
    if (mqttServerManager == null) {
      throw new IoTException("MQTT模块未注入，无法启动TCP网络组件");
    }
    String unionId = network.getUnionId();
    if (StrUtil.isBlank(unionId)) {
      log.error("MQTT网络组件缺少unionId: {}", network.getName());
      return false;
    }

    try {
      boolean success = mqttServerManager.stopMqttClient(unionId);
      if (success) {
        log.info("MQTT网络组件停止成功: unionId={}", unionId);
      } else {
        log.error("MQTT网络组件停止失败: unionId={}", unionId);
      }
      return success;
    } catch (Exception e) {
      log.error("停止MQTT网络组件失败: unionId={}", unionId, e);
      return false;
    }
  }

  /**
   * 重启MQTT网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean restartMqttNetwork(Network network) {
    if (mqttServerManager == null) {
      throw new IoTException("MQTT模块未注入，无法启动TCP网络组件");
    }

    String unionId = network.getUnionId();
    if (StrUtil.isBlank(unionId)) {
      log.error("MQTT网络组件缺少unionId: {}", network.getName());
      return false;
    }

    try {
      boolean success = mqttServerManager.restartMqttClient(unionId);
      if (success) {
        log.info("MQTT网络组件重启成功: unionId={}", unionId);
      } else {
        log.error("MQTT网络组件重启失败: unionId={}", unionId);
      }
      return success;
    } catch (Exception e) {
      log.error("重启MQTT网络组件失败: unionId={}", unionId, e);
      return false;
    }
  }

  @Override
  public IoTDevice getDeviceById(String id) {
    return ioTDeviceMapper.selectDevInstanceById(id);
  }

  /**
   * 启动UDP网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean startUdpNetwork(Network network) {
    if (udpClusterService == null) {
      log.error("UDP服务管理器未注入，无法启动TCP网络组件: {}", network.getName());
      throw new IoTException("UDP模块未注入，无法启动UDP网络组件");
    }
    String productKey = network.getProductKey();
    if (StrUtil.isBlank(productKey)) {
      log.error("UDP网络组件缺少productKey: {}", network.getName());
      return false;
    }

    try {
      boolean success;
      if (udpClusterService != null) {
        // 使用集群服务启动（会广播到其他节点）
        success = udpClusterService.start(productKey);
        log.info("UDP网络组件集群启动: productKey={}, success={}", productKey, success);
      } else {
        log.error("UDP集群服务未注入，无法启动UDP网络组件: {}", network.getName());
        return false;
      }
      return success;
    } catch (Exception e) {
      log.error("启动UDP网络组件失败: productKey={}", productKey, e);
      return false;
    }
  }

  /**
   * 停止UDP网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean stopUdpNetwork(Network network) {
    if (udpClusterService == null) {
      log.error("UDP服务管理器未注入，无法启动TCP网络组件: {}", network.getName());
      throw new IoTException("UDP模块未注入，无法启动UDP网络组件");
    }
    String productKey = network.getProductKey();
    if (StrUtil.isBlank(productKey)) {
      log.error("UDP网络组件缺少productKey: {}", network.getName());
      return false;
    }

    try {
      boolean success;
      if (udpClusterService != null) {
        // 使用集群服务停止（会广播到其他节点）
        success = udpClusterService.stop(productKey);
        log.info("UDP网络组件集群停止: productKey={}, success={}", productKey, success);
      } else {
        log.error("UDP集群服务未注入，无法停止UDP网络组件: {}", network.getName());
        return false;
      }
      return success;
    } catch (Exception e) {
      log.error("停止UDP网络组件失败: productKey={}", productKey, e);
      return false;
    }
  }

  /**
   * 重启UDP网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean restartUdpNetwork(Network network) {
    if (udpClusterService == null) {
      log.error("UDP服务管理器未注入，无法启动TCP网络组件: {}", network.getName());
      throw new IoTException("UDP模块未注入，无法启动UDP网络组件");
    }
    String productKey = network.getProductKey();
    if (StrUtil.isBlank(productKey)) {
      log.error("UDP网络组件缺少productKey: {}", network.getName());
      return false;
    }

    try {
      boolean success;
      if (udpClusterService != null) {
        // 使用集群服务重启（会广播到其他节点）
        success = udpClusterService.restart(productKey);
        log.info("UDP网络组件集群重启: productKey={}, success={}", productKey, success);
      } else {
        log.error("UDP集群服务未注入，无法重启UDP网络组件: {}", network.getName());
        return false;
      }
      return success;
    } catch (Exception e) {
      log.error("重启UDP网络组件失败: productKey={}", productKey, e);
      return false;
    }
  }

  /**
   * 启动WebSocket网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean startWebSocketNetwork(Network network) {
    log.info("[WebSocket网络组件][启动] 组件名称: {}, 类型: {}", network.getName(), network.getType());
    
    try {
      // 解析并验证配置
      JSONObject config = JSONUtil.parseObj(network.getConfiguration());
      String host = config.getStr("host");
      Integer port = config.getInt("port");
      String path = config.getStr("path");
      String clientId = config.getStr("clientId");
      String username = config.getStr("username");

      // 如果 clientId 为空，生成 UUID
      if (StrUtil.isBlank(clientId)) {
        clientId = IdUtil.simpleUUID();
        log.info("[WebSocket网络组件][ClientId] 未配置，自动生成: {}", clientId);
        // 将生成的 clientId 保存回配置，确保后续可以使用
        config.set("clientId", clientId);
        network.setConfiguration(config.toString());
      }

      // 判断是客户端还是服务端
      boolean isClient = "WEB_SOCKET_CLIENT".equals(network.getType());

      if (isClient) {
        // 客户端模式: 需要 host
        if (StrUtil.isBlank(host)) {
          log.error("[WebSocket网络组件][启动失败] 客户端模式下host不能为空");
          return false;
        }
        log.info("[WebSocket网络组件][配置] [客户端] host={}, port={}, path={}, clientId={}, username={}, productKey={}",
                 host, port, path, clientId, StrUtil.isBlank(username) ? "未配置" : username, network.getProductKey());
      } else {
        // 服务端模式: 不需要 host
        log.info("[WebSocket网络组件][配置] [服务端] port={}, path={}, clientId={}, username={}, productKey={}",
                 port, path, clientId, StrUtil.isBlank(username) ? "未配置" : username, network.getProductKey());
      }

      // 验证端口和路径
      if (port == null || port <= 0 || port > 65535) {
        log.error("[WebSocket网络组件][启动失败] 端口配置无效: {}", port);
        return false;
      }

      if (StrUtil.isBlank(path) || !path.startsWith("/")) {
        log.error("[WebSocket网络组件][启动失败] 路径配置无效: {}", path);
        return false;
      }

      // 调用 WebSocket 管理器启动
      if (webSocketServerManager != null) {
        boolean success = webSocketServerManager.startServer(network.getId());
        if (success) {
          log.info("[WebSocket网络组件][启动成功] 组件: {}", network.getName());
        } else {
          log.error("[WebSocket网络组件][启动失败] 管理器启动失败: {}", network.getName());
        }
        return success;
      } else {
        log.warn("[WebSocket网络组件][启动] WebSocket管理器未注入，使用默认启动");
        log.info("[WebSocket网络组件][启动成功] 组件: {}", network.getName());
        return true;
      }

    } catch (Exception e) {
      log.error("[WebSocket网络组件][启动失败] 组件: {}, 错误: {}", network.getName(), e.getMessage(), e);
      return false;
    }
  }

  /**
   * 停止WebSocket网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean stopWebSocketNetwork(Network network) {
    log.info("[WebSocket网络组件][停止] 组件名称: {}, 类型: {}", network.getName(), network.getType());
    
    try {
      if (webSocketServerManager != null) {
        boolean success = webSocketServerManager.stopServer(network.getId());
        if (success) {
          log.info("[WebSocket网络组件][停止成功] 组件: {}", network.getName());
        } else {
          log.error("[WebSocket网络组件][停止失败] 管理器停止失败: {}", network.getName());
        }
        return success;
      } else {
        log.warn("[WebSocket网络组件][停止] WebSocket管理器未注入，使用默认停止");
        log.info("[WebSocket网络组件][停止成功] 组件: {}", network.getName());
        return true;
      }
    } catch (Exception e) {
      log.error("[WebSocket网络组件][停止失败] 组件: {}, 错误: {}", network.getName(), e.getMessage(), e);
      return false;
    }
  }

  /**
   * 重启WebSocket网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean restartWebSocketNetwork(Network network) {
    log.info("[WebSocket网络组件][重启] 组件名称: {}, 类型: {}", network.getName(), network.getType());
    
    try {
      if (webSocketServerManager != null) {
        boolean success = webSocketServerManager.restartServer(network.getId());
        if (success) {
          log.info("[WebSocket网络组件][重启成功] 组件: {}", network.getName());
        } else {
          log.error("[WebSocket网络组件][重启失败] 管理器重启失败: {}", network.getName());
        }
        return success;
      } else {
        log.warn("[WebSocket网络组件][重启] WebSocket管理器未注入，执行停止启动");
        boolean stopSuccess = stopWebSocketNetwork(network);
        if (!stopSuccess) {
          log.warn("[WebSocket网络组件][重启] 停止失败，继续尝试启动");
        }
        Thread.sleep(500);
        boolean startSuccess = startWebSocketNetwork(network);
        log.info("[WebSocket网络组件][重启完成] 组件: {}, 结果: {}", network.getName(), startSuccess);
        return startSuccess;
      }
    } catch (Exception e) {
      log.error("[WebSocket网络组件][重启失败] 组件: {}, 错误: {}", network.getName(), e.getMessage(), e);
      return false;
    }
  }
}
