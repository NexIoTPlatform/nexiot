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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.config.ConnectionConfig;
import cn.universal.admin.network.service.INetworkService;
import cn.universal.admin.system.service.ISysDeptService;
import cn.universal.admin.network.utils.NetworkTypeUtil;
import cn.universal.common.constant.IoTConstant.ProtocolModule;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.constant.NetworkTypeConstants;
import cn.universal.common.domain.R;
import cn.universal.common.enums.NetworkType;
import cn.universal.common.exception.IoTException;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.network.NetworkRuntimeDefinition;
import cn.universal.core.network.NetworkRuntimeProvider;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.device.service.protocol.ProtocolClusterService;
import cn.universal.dm.device.service.protocol.ProtocolServerManager;
import cn.universal.mqtt.protocol.third.ThirdMQTTServerManager;
import cn.universal.persistence.common.inteceptor.SQenGenId;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.Network;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.entity.bo.IoTProductBO;
import cn.universal.persistence.entity.bo.NetworkBO;
import cn.universal.persistence.entity.admin.SysDept;
import cn.universal.persistence.entity.vo.IoTProductVO;
import cn.universal.persistence.entity.vo.NetworkVO;
import cn.universal.persistence.mapper.IoTDeviceFenceRelMapper;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.NetworkMapper;
import cn.universal.persistence.query.NetworkQuery;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.support.DeptDataPermissionSupport;
import cn.universal.security.support.DeptDataPermissionSupport.DeptDataPermissionScope;
import cn.universal.security.utils.SecurityUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
  @Resource private ConnectionConfig connectionConfig;
  @Resource private IoTDeviceMapper ioTDeviceMapper;
  @Resource private IoTProductMapper ioTProductMapper;
  @Resource private IoTDeviceFenceRelMapper ioTDeviceFenceRelMapper;
  @Resource private IoTUserService ioTUserService;
  @Resource private ISysDeptService sysDeptService;
  @Resource private DeptDataPermissionSupport deptDataPermissionSupport;

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
  private List<NetworkRuntimeProvider> networkRuntimeProviders = new ArrayList<>();

  @Override
  public Map<String, Object> getTcpPortPolicy() {
    int min = connectionConfig.getTcpPortMin();
    int max = connectionConfig.getTcpPortMax();
    if (min < 1 || max > 65535 || min > max) {
      throw new IllegalStateException("TCP端口池配置无效，请检查 network.tcp.port-min / port-max");
    }
    return Map.of("min", min, "max", max, "publicHost", connectionConfig.getTcpPublicHost());
  }

  private void validateTcpPort(Network network) {
    if (!NetworkType.TCP_SERVER.getId().equals(network.getType())) return;
    Map<String, Object> policy = getTcpPortPolicy();
    Integer port = JSONUtil.parseObj(network.getConfiguration()).getInt("port");
    if (port == null || port < (int) policy.get("min") || port > (int) policy.get("max")) {
      throw new IllegalArgumentException(
          "TCP服务端端口必须在部署预留范围 " + policy.get("min") + "–" + policy.get("max") + " 内");
    }
  }

  @Override
  public boolean del(String productKey) {
    Example ex = new Example(Network.class);
    ex.createCriteria().andEqualTo("productKey", productKey);
    return networkMapper.deleteByExample(ex) > 0;
  }

  @Override
  public List<NetworkVO> selectNetworkList(NetworkBO bo) {
    applyNetworkScope(bo);
    return networkMapper.selectNetworkListV1(bo);
  }

  @Override
  public List<IoTDevice> queryMileSightList(NetworkBO bo) {
    applyNetworkScope(bo);
    return ioTDeviceMapper.queryMileSightList(bo);
  }

  @Override
  public R insertDevInstance(IoTDeviceBO devInstancebo) {
    JSONObject downObj = new JSONObject();
    IoTUser actorUser = resolveActorUser(devInstancebo == null ? null : devInstancebo.getCreatorId());
    // 预添加归属为空
    downObj.set("appUnionId", resolveAppUnionId(devInstancebo == null ? null : devInstancebo.getCreatorId()));
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
    downObj.set("tenantId", resolveTenantId(actorUser));
    downObj.set("deptId", resolveDeptId(devInstancebo, actorUser));
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
      assertDeviceAccess(ioTDevice);
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

  private IoTUser resolveActorUser(String unionId) {
    String targetUnionId = StrUtil.blankToDefault(StrUtil.trim(unionId), SecurityUtils.getUnionIdQuietly());
    if (StrUtil.isBlank(targetUnionId)) {
      return null;
    }
    return ioTUserService.selectUserByUnionId(targetUnionId);
  }

  private String resolveAppUnionId(String unionId) {
    IoTUser actorUser = resolveActorUser(unionId);
    if (actorUser == null) {
      throw new IoTException("操作用户不存在");
    }
    return actorUser.getUnionId();
  }

  private Long resolveDeptId(IoTDeviceBO devInstancebo, IoTUser actorUser) {
    if (devInstancebo == null) {
      return null;
    }
    if (devInstancebo.getDeptId() == null && actorUser != null) {
      devInstancebo.setDeptId(actorUser.getDeptId());
    }
    if (devInstancebo.getDeptId() == null && actorUser != null && !actorUser.isAdmin()) {
      throw new IoTException("当前账号未绑定部门，无法新增设备");
    }
    validateDeviceDept(devInstancebo.getDeptId(), resolveTenantId(actorUser), actorUser);
    return devInstancebo.getDeptId();
  }

  private IoTUser resolveTenantOwner(IoTUser user) {
    if (user == null) {
      return null;
    }
    if (user.isAdmin() || isTenantAdmin(user)) {
      return user;
    }
    String tenantId = resolveTenantId(user);
    if (StrUtil.isBlank(tenantId)) {
      return user;
    }
    List<IoTUser> adminUsers =
        ioTUserService.selectUserList(
            IoTUser.builder().tenantId(tenantId).tenantRole("ADMIN").build());
    return CollUtil.isNotEmpty(adminUsers) ? adminUsers.get(0) : user;
  }

  private void validateDeviceDept(Long deptId, String tenantId, IoTUser operator) {
    if (deptId == null) {
      return;
    }
    SysDept dept = sysDeptService.selectDeptById(deptId);
    if (dept == null
        || StrUtil.isBlank(tenantId)
        || !StrUtil.equals(dept.getTenantId(), tenantId)) {
      throw new IoTException("设备所属部门不合法");
    }
    if (!StrUtil.equals(dept.getStatus(), "0")) {
      throw new IoTException("设备所属部门已停用");
    }
    if (!canManageDept(dept, operator)) {
      throw new IoTException("你无权分配当前部门");
    }
  }

  private boolean canManageDept(SysDept dept, IoTUser operator) {
    if (dept == null) {
      return false;
    }
    if (operator == null || operator.isAdmin()) {
      return true;
    }
    if (!StrUtil.equals(resolveTenantId(operator), dept.getTenantId())) {
      return false;
    }
    DeptDataPermissionScope scope = deptDataPermissionSupport.resolveScope(operator.getUnionId());
    if (scope == null) {
      return actorDeptMatches(operator, dept);
    }
    if (scope.isAll()) {
      return true;
    }
    if (dept.getDeptId() != null && scope.getScopeDeptIds().contains(dept.getDeptId())) {
      return true;
    }
    return actorDeptMatches(operator, dept) && scope.getScopeUnionIds().contains(operator.getUnionId());
  }

  private boolean actorDeptMatches(IoTUser operator, SysDept dept) {
    return operator != null
        && operator.getDeptId() != null
        && dept != null
        && StrUtil.equals(String.valueOf(operator.getDeptId()), String.valueOf(dept.getDeptId()));
  }

  protected String resolveTenantId(IoTUser user) {
    if (user == null) {
      return null;
    }
    return StrUtil.blankToDefault(user.getTenantId(), user.getUnionId());
  }

  private String resolveTenantId(String creatorId) {
    if (StrUtil.isBlank(creatorId)) {
      return null;
    }
    return resolveTenantId(ioTUserService.selectUserByUnionId(creatorId));
  }

  private boolean isTenantAdmin(IoTUser user) {
    return user != null && !user.isAdmin() && StrUtil.equalsIgnoreCase(user.getTenantRole(), "ADMIN");
  }

  private void applyNetworkScope(NetworkBO query) {
    deptDataPermissionSupport.applyScope(query, resolveScopeUnionId());
  }

  private void applyNetworkScope(NetworkQuery query) {
    deptDataPermissionSupport.applyScope(query, resolveScopeUnionId());
  }

  private void assertNetworkAccess(Network network) {
    if (network == null) {
      throw new RuntimeException("网络组件不存在");
    }
    if (!deptDataPermissionSupport.canAccessResource(
        resolvePermissionScope(), network.getTenantId(), null, network.getCreateUser())) {
      throw new RuntimeException("不能操作不是当前租户的网络组件");
    }
  }

  private void assertDeviceAccess(IoTDevice ioTDevice) {
    if (ioTDevice == null) {
      throw new IoTException("设备不存在");
    }
    if (!deptDataPermissionSupport.canAccessResource(
        resolvePermissionScope(), ioTDevice.getTenantId(), ioTDevice.getDeptId(), ioTDevice.getCreatorId())) {
      throw new IoTException("你无权操作当前设备");
    }
  }

  private String resolveScopeUnionId() {
    IoTUser currentUser = resolveActorUser(SecurityUtils.getUnionIdQuietly());
    return currentUser == null ? null : currentUser.getUnionId();
  }

  private DeptDataPermissionScope resolvePermissionScope() {
    String unionId = resolveScopeUnionId();
    return StrUtil.isBlank(unionId) ? null : deptDataPermissionSupport.resolveScope(unionId);
  }

  @Override
  public List<NetworkBO> selectNetworkList(NetworkQuery query) {
    applyNetworkScope(query);
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
    } else if (CollUtil.isNotEmpty(query.getTypes())) {
      // 支持前端直接传 types=TCP_SERVER&types=UDP 这类列表参数
      if (!NetworkTypeUtil.isValidTypes(query.getTypes())) {
        List<String> invalidTypes = NetworkTypeUtil.getInvalidTypes(query.getTypes());
        throw new IoTException("无效的网络类型: " + String.join(", ", invalidTypes));
      }
    }
    // 计算分页参数
    Page<IoTProductVO> page = PageHelper.startPage(query.getPageNum(), query.getPageSize());
    List<NetworkBO> list = networkMapper.selectNetworkList(query);
    for (NetworkBO bo : list) {
      // 启用/停用状态
      bo.setEnableName(bo.isStateEnabled() ? "已启用" : "已停用");
      // 实际运行状态
      String type = bo.getType();
      String productKey = bo.getProductKey();
      String unionId = bo.getUnionId();
      NetworkRuntimeProvider runtimeProvider = findRuntimeProvider(type);
      if (runtimeProvider != null) {
        bo.setConfiguration(runtimeProvider.sanitizeConfiguration(bo.getConfiguration()));
        boolean running = runtimeProvider.isRunning(toRuntimeDefinition(bo));
        bo.setStateName(running ? "已启动" : "未启动");
        bo.setRunning(running);
        List<IoTProductBO> products = ioTProductMapper.selectMqttProductsUseNetwork(unionId);
        bo.setBindProductCount(CollUtil.size(products));
        bo.setBindProducts(products);
      } else if (NetworkType.TCP_CLIENT.getId().equals(type)
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
        // 查出独占型服务端绑定的产品
        if (NetworkType.TCP_SERVER.getId().equals(type)) {
          IoTProductBO ioTProductBO =
              ioTProductMapper.selectTcpProductsUseNetwork(bo.getProductKey());
          bo.setBindTcpServerProductCount(ioTProductBO == null ? 0 : 1);
          bo.setBindTcpServerProducts(ioTProductBO);
        }

      } else if (NetworkType.MQTT_CLIENT.getId().equals(type)
          || NetworkType.MQTT_SERVER.getId().equals(type)) {
        List<IoTProductBO> ioTProductBOS = ioTProductMapper.selectMqttProductsUseNetwork(unionId);
        bo.setBindProductCount(CollUtil.size(ioTProductBOS));
        bo.setBindProducts(ioTProductBOS);
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
        IoTProductBO ioTProductBO = ioTProductMapper.selectTcpProductsUseNetwork(bo.getProductKey());
        bo.setBindTcpServerProductCount(ioTProductBO == null ? 0 : 1);
        bo.setBindTcpServerProducts(ioTProductBO);
      } else {
        bo.setStateName("未知");
        bo.setRunning(Boolean.FALSE);
      }
    }
    return list;
  }

  @Override
  public NetworkVO selectNetworkById(Long id) {
    Network network = networkMapper.selectNetworkById(id);
    if (network == null) {
      return null;
    }
    assertNetworkAccess(network);
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
    vo.setTenantId(network.getTenantId());
    vo.setState(network.getState());
    NetworkRuntimeProvider detailProvider = findRuntimeProvider(network.getType());
    vo.setConfiguration(
        detailProvider == null
            ? network.getConfiguration()
            : detailProvider.sanitizeConfiguration(network.getConfiguration()));
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
      case "HTTP_CLIENT":
        vo.setTypeName(NetworkType.HTTP_CLIENT.getDescription());
        break;
      case "HTTP_SERVER":
        vo.setTypeName(NetworkType.HTTP_SERVER.getDescription());
        break;
      default:
        vo.setTypeName(network.getType());
    }
    // 启用/停用状态
    vo.setEnableName(network.isStateEnabled() ? "已启用" : "已停用");
    // 实际运行状态和绑定产品
    String type = network.getType();
    String productKey = network.getProductKey();
    String unionId = network.getUnionId();
    NetworkRuntimeProvider runtimeProvider = findRuntimeProvider(type);
    if (runtimeProvider != null) {
      boolean running = runtimeProvider.isRunning(toRuntimeDefinition(network));
      vo.setStateName(running ? "已启动" : "未启动");
      vo.setRunning(running);
      List<IoTProductBO> products = ioTProductMapper.selectMqttProductsUseNetwork(unionId);
      vo.setBindProductCount(CollUtil.size(products));
      vo.setBindProducts(products);
    } else if (NetworkType.TCP_CLIENT.getId().equals(type)
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
        IoTProductBO ioTProductBO = ioTProductMapper.selectTcpProductsUseNetwork(productKey);
        vo.setBindTcpServerProductCount(ioTProductBO == null ? 0 : 1);
        vo.setBindTcpServerProducts(ioTProductBO);
      }

    } else if (NetworkType.MQTT_CLIENT.getId().equals(type)
        || NetworkType.MQTT_SERVER.getId().equals(type)) {
      // 查询MQTT绑定产品
      List<IoTProductBO> ioTProductBOS = ioTProductMapper.selectMqttProductsUseNetwork(unionId);
      vo.setBindProductCount(CollUtil.size(ioTProductBOS));
      vo.setBindProducts(ioTProductBOS);
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
      IoTProductBO ioTProductBO = ioTProductMapper.selectTcpProductsUseNetwork(productKey);
      vo.setBindTcpServerProductCount(ioTProductBO == null ? 0 : 1);
      vo.setBindTcpServerProducts(ioTProductBO);
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
    Long excludeId = isUpdate ? network.getId() : null;
    int count = networkMapper.selectTcpNetworkByPortCount(port, excludeId);
    if (count > 0) {
      throw new RuntimeException("端口 " + port + " 已被其他网络组件占用");
    }
  }

  @Override
  @Transactional
  public int insertNetwork(Network network) {
    if (NetworkType.TCP_CLIENT.getId().equals(network.getType())) {
      throw new IllegalArgumentException("当前仅支持创建TCP服务端，请选择TCP_SERVER通信组件");
    }
    // 验证配置
    if (!validateNetworkConfig(network)) {
      throw new RuntimeException("网络组件配置验证失败");
    }

    // 检查配置唯一性
    checkConfigurationUniqueness(network, null);

    // 设置默认值
    if (network.getState() == null) {
      network.setState(Network.STATE_DISABLED);
    }
    if (network.getId() == null) {
      network.setId(SQenGenId.nextId());
    }
    if (network.getCreateDate() == null) {
      network.setCreateDate(new Date());
    }

    IoTUser currentUser = resolveActorUser(SecurityUtils.getUnionIdQuietly());
    if (StrUtil.isBlank(network.getCreateUser()) && currentUser != null) {
      network.setCreateUser(currentUser.getUnionId());
    }
    if (currentUser != null && !currentUser.isAdmin()) {
      network.setTenantId(resolveTenantId(currentUser));
    }
    network.setUnionId(IdUtil.objectId());

    if (isExclusiveProductNetworkType(network.getType())) {
      prepareExclusiveProductNetwork(network, null);
    }
    checkPortAvailable(network, false);
    int result = networkMapper.insertNetwork(network);
    // 新增成功后，若为产品独占服务端，更新iot_product表
    if (result > 0 && shouldSyncProductNetworkUnion(network.getType())) {
      ioTProductMapper.updateNetworkUnionIdByProductKey(
          network.getProductKey(), network.getProductKey());
    }
    NetworkRuntimeProvider runtimeProvider = findRuntimeProvider(network.getType());
    if (result > 0 && runtimeProvider != null && network.isStateEnabled()) {
      startRuntimeAfterCommit(runtimeProvider, toRuntimeDefinition(network));
    }
    return result;
  }

  @Override
  @Transactional
  public int updateNetwork(Network network) {
    if (network == null || network.getId() == null) {
      throw new RuntimeException("网络组件配置验证失败");
    }
    // 检查权限（只能操作自己创建的网络组件，除非是管理员）
    Network existNetwork = networkMapper.selectNetworkById(network.getId());
    if (existNetwork == null) {
      throw new RuntimeException("网络组件不存在");
    }
    assertNetworkAccess(existNetwork);
    network.setType(StrUtil.blankToDefault(network.getType(), existNetwork.getType()));
    network.setName(StrUtil.blankToDefault(network.getName(), existNetwork.getName()));
    network.setUnionId(existNetwork.getUnionId());
    NetworkRuntimeProvider runtimeProvider = findRuntimeProvider(network.getType());
    if (runtimeProvider != null) {
      network.setConfiguration(
          runtimeProvider.prepareConfiguration(
              existNetwork.getConfiguration(), network.getConfiguration()));
    }
    // 验证合并并恢复敏感字段后的完整配置
    if (!validateNetworkConfig(network)) {
      throw new RuntimeException("网络组件配置验证失败");
    }
    network.setTenantId(existNetwork.getTenantId());

    if (isExclusiveProductNetworkType(existNetwork.getType())) {
      prepareExclusiveProductNetwork(network, existNetwork);
    }
    // 检查配置唯一性（排除自己）
    checkConfigurationUniqueness(network, network.getId());
    checkPortAvailable(network, true);
    int count = networkMapper.updateNetwork(network);
    if (count > 0 && shouldSyncProductNetworkUnion(network.getType())) {
      ioTProductMapper.updateNetworkUnionIdByProductKey(
          network.getProductKey(), network.getProductKey());
    }
    if (count > 0 && runtimeProvider != null) {
      Network updated = networkMapper.selectNetworkById(network.getId());
      if (updated != null) {
        if (updated.isStateEnabled()) {
          reloadRuntimeAfterCommit(runtimeProvider, toRuntimeDefinition(updated));
        } else {
          stopRuntimeAfterCommit(runtimeProvider, toRuntimeDefinition(updated));
        }
      }
    }
    return count;
  }

  private boolean isExclusiveProductNetworkType(String type) {
    return NetworkType.TCP_SERVER.getId().equals(type)
        || NetworkType.TCP_CLIENT.getId().equals(type)
        || NetworkType.UDP.getId().equals(type)
        || NetworkType.HTTP_SERVER.getId().equals(type);
  }

  private boolean shouldSyncProductNetworkUnion(String type) {
    return NetworkType.TCP_SERVER.getId().equals(type)
        || NetworkType.UDP.getId().equals(type)
        || NetworkType.HTTP_SERVER.getId().equals(type);
  }

  private void prepareExclusiveProductNetwork(Network network, Network existNetwork) {
    String type = existNetwork == null ? network.getType() : existNetwork.getType();
    String productKey =
        StrUtil.blankToDefault(StrUtil.trim(network.getProductKey()),
            existNetwork == null ? null : existNetwork.getProductKey());
    if (StrUtil.isBlank(productKey)) {
      throw new RuntimeException("TCP/UDP/HTTP服务组件必须选择产品");
    }
    IoTProduct product = ioTProductMapper.getProductByProductKey(productKey);
    if (product == null) {
      throw new RuntimeException("选择的产品不存在");
    }
    validateExclusiveProductNetworkType(type, product);
    network.setType(type);
    network.setProductKey(productKey);
    network.setUnionId(productKey);

    if (existNetwork == null) {
      assertProductCanBindExclusiveNetwork(productKey, null);
      return;
    }
    String oldProductKey = existNetwork.getProductKey();
    if (!StrUtil.equals(oldProductKey, productKey)) {
      throw new RuntimeException("TCP/UDP/HTTP网络组件绑定产品不允许在编辑时变更，请新增组件");
    }
    assertProductCanBindExclusiveNetwork(productKey, existNetwork.getId());
  }

  private void validateExclusiveProductNetworkType(String type, IoTProduct product) {
    if (NetworkType.UDP.getId().equals(type)) {
      if (!ProtocolModule.udp.name().equals(product.getThirdPlatform())) {
        throw new RuntimeException("UDP组件只能绑定UDP产品");
      }
      return;
    }
    if (NetworkType.HTTP_SERVER.getId().equals(type)) {
      if (!ProtocolModule.http.name().equals(product.getThirdPlatform())) {
        throw new RuntimeException("HTTP组件只能绑定HTTP产品");
      }
      return;
    }
    if (!ProtocolModule.tcp.name().equals(product.getThirdPlatform())) {
      throw new RuntimeException("TCP组件只能绑定TCP产品");
    }
  }

  private void assertProductCanBindExclusiveNetwork(String productKey, Long excludeNetworkId) {
    Example example = new Example(Network.class);
    Example.Criteria criteria =
        example
            .createCriteria()
            .andEqualTo("productKey", productKey)
            .andIn(
                "type",
                Arrays.asList(
                    NetworkType.TCP_CLIENT.getId(),
                    NetworkType.TCP_SERVER.getId(),
                    NetworkType.UDP.getId(),
                    NetworkType.HTTP_SERVER.getId()));
    if (excludeNetworkId != null) {
      criteria.andNotEqualTo("id", excludeNetworkId);
    }
    if (networkMapper.selectCountByExample(example) > 0) {
      throw new RuntimeException("该产品已绑定其他TCP/UDP/HTTP服务组件，不能重复绑定");
    }
    Example productExample = new Example(IoTProduct.class);
    productExample
        .createCriteria()
        .andEqualTo("networkUnionId", productKey)
        .andNotEqualTo("productKey", productKey);
    if (ioTProductMapper.selectCountByExample(productExample) > 0) {
      throw new RuntimeException("该网络组件已被其他产品引用，不能重复绑定");
    }
  }

  @Override
  @Transactional
  public int deleteNetworkById(Long id) {
    Network network = networkMapper.selectNetworkById(id);
    if (network == null) {
      throw new RuntimeException("网络组件不存在");
    }
    assertNetworkAccess(network);
    NetworkRuntimeProvider runtimeProvider = findRuntimeProvider(network.getType());
    NetworkRuntimeDefinition runtimeDefinition =
        runtimeProvider == null ? null : toRuntimeDefinition(network);
    String type = network.getType();
    // 独占产品网络在产品下仍有设备时不允许删除。
    if (NetworkType.TCP_SERVER.getId().equals(type)
        || NetworkType.TCP_CLIENT.getId().equals(type)
        || NetworkType.UDP.getId().equals(type)
        || NetworkType.HTTP_SERVER.getId().equals(type)) {
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
    if (runtimeProvider != null) {
      stopRuntimeAfterCommit(runtimeProvider, runtimeDefinition);
    }
    return ct;
  }

  @Override
  @Transactional
  public int deleteNetworkByIds(Long[] ids) {
    if (ids == null || ids.length == 0) {
      return 0;
    }
    int count = 0;
    for (Long id : ids) {
      count += ((INetworkService) AopContext.currentProxy()).deleteNetworkById(id);
    }
    return count;
  }

  @Override
  @Transactional
  public int startNetwork(Long id) {
    Network network = networkMapper.selectNetworkById(id);
    if (network == null) {
      throw new RuntimeException("网络组件不存在");
    }
    assertNetworkAccess(network);
    if (!network.isStateEnabled()) {
      throw new RuntimeException("网络组件已停用，不能启动");
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
        log.info("启动网络组件成功: {}", network.getName());
        return 1;
      } else {
        throw new IllegalStateException("组件运行时未能启动，请检查组件配置及服务日志: " + network.getName());
      }
    } catch (Exception e) {
      log.error("启动网络组件失败: {}", network.getName(), e);
      throw new RuntimeException("启动网络组件失败: " + e.getMessage(), e);
    }
  }

  @Override
  @Transactional
  public int stopNetwork(Long id) {
    Network network = networkMapper.selectNetworkById(id);
    if (network == null) {
      throw new RuntimeException("网络组件不存在");
    }
    assertNetworkAccess(network);

    try {
      // 根据网络类型调用相应的停止逻辑
      boolean success = stopNetworkByType(network);
      if (success) {
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
  public int restartNetwork(Long id) {
    Network network = networkMapper.selectNetworkById(id);
    if (network == null) {
      throw new RuntimeException("网络组件不存在");
    }
    assertNetworkAccess(network);
    if (!network.isStateEnabled()) {
      throw new RuntimeException("网络组件已停用，不能重启");
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
        log.info("重启网络组件成功: {}", network.getName());
        // 重启后状态应该是启动的
        return 1;
      } else {
        throw new RuntimeException("重启网络组件失败: " + network.getName());
      }
    } catch (Exception e) {
      log.error("重启网络组件失败: {}", network.getName(), e);
      throw new RuntimeException("重启网络组件失败: " + e.getMessage(), e);
    }
  }

  @Override
  public List<String> getNetworkTypes() {
    Set<String> types = new java.util.LinkedHashSet<>(Arrays.asList(
        NetworkType.TCP_CLIENT.getId(),
        NetworkType.TCP_SERVER.getId(),
        NetworkType.MQTT_CLIENT.getId(),
        NetworkType.MQTT_SERVER.getId(),
        NetworkType.UDP.getId()));
    networkRuntimeProviders.forEach(provider -> types.addAll(provider.supportedTypes()));
    return new ArrayList<>(types);
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
    if (!isValidNetworkType(network.getType())) {
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
    NetworkRuntimeProvider runtimeProvider = findRuntimeProvider(network.getType());
    if (runtimeProvider != null) {
      runtimeProvider.validate(toRuntimeDefinition(network));
    }
    validateTcpPort(network);
    return true;
  }

  /**
   * 检查网络组件配置的唯一性
   *
   * @param network 网络组件
   * @param excludeId 排除的网络组件ID（用于更新时排除自己）
   */
  private void checkConfigurationUniqueness(Network network, Long excludeId) {
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
      NetworkRuntimeProvider runtimeProvider = findRuntimeProvider(type);
      if (runtimeProvider != null) {
        return runtimeProvider.start(toRuntimeDefinition(network));
      }
      switch (type) {
        case NetworkTypeConstants.TCP_CLIENT:
        case NetworkTypeConstants.TCP_SERVER:
          return startTcpNetwork(network);
        case NetworkTypeConstants.MQTT_CLIENT:
        case NetworkTypeConstants.MQTT_SERVER:
          return startMqttNetwork(network);
        case NetworkTypeConstants.UDP:
          return startUdpNetwork(network);
        default:
          log.warn("不支持的网络类型: {}", type);
          return false;
      }
    } catch (Exception e) {
      log.error("启动网络组件失败: type={}, name={}", type, network.getName(), e);
      throw new RuntimeException(e.getMessage(), e);
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
      NetworkRuntimeProvider runtimeProvider = findRuntimeProvider(type);
      if (runtimeProvider != null) {
        return runtimeProvider.stop(toRuntimeDefinition(network));
      }
      switch (type) {
        case NetworkTypeConstants.TCP_CLIENT:
        case NetworkTypeConstants.TCP_SERVER:
          return stopTcpNetwork(network);
        case NetworkTypeConstants.MQTT_CLIENT:
        case NetworkTypeConstants.MQTT_SERVER:
          return stopMqttNetwork(network);
        case NetworkTypeConstants.UDP:
          return stopUdpNetwork(network);
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
      NetworkRuntimeProvider runtimeProvider = findRuntimeProvider(type);
      if (runtimeProvider != null) {
        return runtimeProvider.restart(toRuntimeDefinition(network));
      }
      switch (type) {
        case NetworkTypeConstants.TCP_CLIENT:
        case NetworkTypeConstants.TCP_SERVER:
          return restartTcpNetwork(network);
        case NetworkTypeConstants.MQTT_CLIENT:
        case NetworkTypeConstants.MQTT_SERVER:
          return restartMqttNetwork(network);
        case NetworkTypeConstants.UDP:
          return restartUdpNetwork(network);
        default:
          log.warn("不支持的网络类型: {}", type);
          return false;
      }
    } catch (Exception e) {
      log.error("重启网络组件失败: type={}, name={}", type, network.getName(), e);
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * 启动TCP网络组件
   *
   * @param network 网络组件
   * @return 是否成功
   */
  private boolean startTcpNetwork(Network network) {
    assertTcpServerType(network);
    if (tcpClusterService == null) {
      log.error("TCP服务管理器未注入，无法启动TCP网络组件: {}", network.getName());
      throw new IoTException("TCP模块未注入，无法启动TCP网络组件");
    }

    String productKey = network.getProductKey();
    if (StrUtil.isBlank(productKey)) {
      log.error("TCP网络组件缺少productKey: {}", network.getName());
      throw new IllegalArgumentException("TCP网络组件未关联产品，productKey不能为空");
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
      throw new RuntimeException(e.getMessage(), e);
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
    assertTcpServerType(network);
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
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private void assertTcpServerType(Network network) {
    if (!NetworkType.TCP_SERVER.getId().equals(network.getType())) {
      throw new IllegalArgumentException(
          "当前TCP运行时仅支持TCP服务端，组件「" + network.getName() + "」的类型为"
              + network.getType() + "，请使用TCP_SERVER通信组件");
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
      log.error("UDP服务管理器未注入，无法启动UDP网络组件: {}", network.getName());
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
      log.error("UDP服务管理器未注入，无法停止UDP网络组件: {}", network.getName());
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
      log.error("UDP服务管理器未注入，无法重启UDP网络组件: {}", network.getName());
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

  private NetworkRuntimeProvider findRuntimeProvider(String type) {
    if (StrUtil.isBlank(type) || networkRuntimeProviders == null) {
      return null;
    }
    return networkRuntimeProviders.stream().filter(provider -> provider.supports(type)).findFirst().orElse(null);
  }

  private NetworkRuntimeDefinition toRuntimeDefinition(Network network) {
    return new NetworkRuntimeDefinition(
        network.getId(), network.getType(), network.getProductKey(), network.getUnionId(),
        network.getName(), network.getConfiguration(), network.getState());
  }

  private void reloadRuntimeAfterCommit(
      NetworkRuntimeProvider runtimeProvider, NetworkRuntimeDefinition definition) {
    runRuntimeAfterCommit(
        "reload", definition, () -> runtimeProvider.restart(definition));
  }

  private void startRuntimeAfterCommit(
      NetworkRuntimeProvider runtimeProvider, NetworkRuntimeDefinition definition) {
    runRuntimeAfterCommit(
        "start", definition, () -> runtimeProvider.start(definition));
  }

  private void stopRuntimeAfterCommit(
      NetworkRuntimeProvider runtimeProvider, NetworkRuntimeDefinition definition) {
    runRuntimeAfterCommit(
        "stop", definition, () -> runtimeProvider.stop(definition));
  }

  private void runRuntimeAfterCommit(
      String action, NetworkRuntimeDefinition definition, BooleanSupplier callback) {
    Runnable task =
        () -> {
          try {
            if (!callback.getAsBoolean()) {
              log.error(
                  "Network runtime {} returned false after database commit: id={}, type={}",
                  action,
                  definition.id(),
                  definition.type());
            }
          } catch (Exception e) {
            log.error(
                "Network runtime {} failed after database commit: id={}, type={}",
                action,
                definition.id(),
                definition.type(),
                e);
          }
        };
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              task.run();
            }
          });
      return;
    }
    task.run();
  }
}
