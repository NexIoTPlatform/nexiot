package cn.universal.web.service.openapi.v2;

import static cn.universal.web.dto.openapi.v2.OpenApiV2DeviceModels.*;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.domain.R;
import cn.universal.common.exception.IoTException;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tk.mybatis.mapper.entity.Example;

/** 以现有协议下行和影子服务为唯一业务执行入口，不重复实现设备生命周期。 */
@Service
@RequiredArgsConstructor
public class OpenApiV2DeviceService {
  private final OpenApiV2Access access;
  private final IoTDeviceMapper devices;
  private final IoTDeviceShadowService shadows;
  // 只开放已核对为真正本地生命周期的适配器，避免将新增/更新误分派为功能调用。
  private static final Set<String> LIFECYCLE_WRITE =
      Set.of("mqtt", "tcp", "udp", "sip", "industrial", "ezviz");
  private static final Set<String> LIFECYCLE_DELETE =
      Set.of(
          "mqtt",
          "tcp",
          "udp",
          "sip",
          "industrial",
          "ezviz",
          "imoulife",
          "modbus",
          "ttlock",
          "ctaiot",
          "onenet",
          "wvp",
          "dahua_cloud",
          "hik_sdk",
          "dahua_sdk",
          "tiandy_sdk", "uniview_sdk",
          "hik_isup");
  private static final Set<String> RESERVED =
      Set.of(
          "productkey",
          "deviceid",
          "iotid",
          "tenantid",
          "deptid",
          "creatorid",
          "creatorname",
          "appunionid",
          "applicationid",
          "application",
          "isadmin",
          "allowinsert",
          "devicereuse",
          "cmd",
          "metadata",
          "extensions",
          "data",
          "gwproductkey",
          "gwdeviceid",
          "extdeviceid",
          "slaveaddress",
          "devicename",
          "longitude",
          "latitude",
          "detail",
          "deviceaddress",
          "msgid",
          "timestamp",
          "function",
          "properties",
          "event",
          "rawdata",
          "messagetype");

  /**
   * 仅开放已确认通过 executeLockApiRequest 固定 lockId 的锁级函数。 账号级用户/锁列表以及生命周期 handler 不能通过物模型函数绕入。新增能力需重新审查。
   */
  private static final Set<String> TTLOCK_DEVICE_FUNCTIONS =
      Set.of(
          "lock",
          "unlock",
          "lockdetail",
          "queryopenstate",
          "queryelectricquantity",
          "lockrecords",
          "listpassword",
          "addpassword",
          "updatepassword",
          "deletepassword",
          "listcard",
          "addcard",
          "updatecard",
          "deletecard",
          "listfingerprint",
          "addfingerprint",
          "updatefingerprint",
          "deletefingerprint");

  /** 原视频物模型桥接包含实例级操作、原生任务和会话ID；V2仅经独立视频门面开放。 */
  private static final Set<String> MANAGED_VIDEO_PROTOCOLS =
      Set.of("wvp", "hik_sdk", "dahua_sdk", "tiandy_sdk", "uniview_sdk", "hik_isup");

  public DevicePage list(String productKey, String deviceId, int page, int pageSize) {
    IoTUserApplication app = access.application();
    if (page < 1 || pageSize < 1 || pageSize > 200 || ((long) page - 1) * pageSize > 10000)
      throw bad("page从1开始，pageSize为1..200，最大偏移量10000");
    Example query = new Example(IoTDevice.class);
    var criteria =
        query
            .createCriteria()
            .andEqualTo("tenantId", access.tenantId(app))
            .andEqualTo("creatorId", app.getUnionId())
            .andEqualTo("application", app.getAppUniqueId());
    if (productKey != null) {
      OpenApiV2Access.identifier(productKey, "productKey");
      criteria.andEqualTo("productKey", productKey);
    }
    if (deviceId != null) {
      OpenApiV2Access.identifier(deviceId, "deviceId");
      criteria.andEqualTo("deviceId", deviceId);
    }
    query.orderBy("createTime").desc().orderBy("id").desc();
    try (Page<IoTDevice> result = PageHelper.startPage(page, pageSize)) {
      List<IoTDevice> rows = devices.selectByExample(query);
      return new DevicePage(
          rows.stream().map(Device::from).toList(), result.getTotal(), page, pageSize);
    }
  }

  public Device detail(String productKey, String deviceId) {
    IoTDeviceDTO authorized = access.device(productKey, deviceId);
    IoTUserApplication app = access.application();
    IoTDevice row =
        devices.selectOne(
            IoTDevice.builder()
                .iotId(authorized.getIotId())
                .productKey(productKey)
                .deviceId(deviceId)
                .tenantId(access.tenantId(app))
                .creatorId(app.getUnionId())
                .application(app.getAppUniqueId())
                .build());
    if (row == null) throw new org.springframework.security.access.AccessDeniedException("设备归属已变化");
    return Device.from(row);
  }

  public List<IoTDevicePropertiesBO> properties(String productKey, String deviceId) {
    IoTDeviceDTO device = access.device(productKey, deviceId);
    List<IoTDevicePropertiesBO> properties = shadows.getDevState(device.getIotId());
    return properties == null ? List.of() : properties;
  }

  public Map<String, Object> model(String productKey, String deviceId) {
    String metadata = access.device(productKey, deviceId).getMetadata();
    return StrUtil.isBlank(metadata) ? Map.of() : JSONUtil.parseObj(metadata);
  }

  public CreationReceipt create(String productKey, CreateDevice body) {
    OpenApiV2Access.identifier(body.deviceId(), "deviceId");
    IoTUserApplication app = access.application();
    IoTProduct product = access.product(productKey);
    lifecycleProtocol(product.getThirdPlatform(), DownCmd.DEV_ADD);
    // TCP 当前新增器不消费地址；明确拒绝，避免成功回执掩盖字段丢失。
    if ("tcp".equals(product.getThirdPlatform()) && body.deviceAddress() != null)
      throw bad("TCP设备新增暂不支持deviceAddress，请创建后通过更新接口设置");
    if (devices.selectCount(
            IoTDevice.builder().productKey(productKey).deviceId(body.deviceId()).build())
        > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "设备已存在，请使用查询或显式绑定接口");
    JSONObject data = new JSONObject();
    if (body.data() != null) {
      body.data()
          .forEach(
              (key, value) -> {
                if (key == null || RESERVED.contains(key.toLowerCase(Locale.ROOT)))
                  throw bad("data包含保留控制字段");
                data.set(key, value);
              });
    }
    // Ezviz新增器会把 data.deviceSerial 作为云端目标；它必须与本次已授权的平台设备编号一致。
    if ("ezviz".equalsIgnoreCase(product.getThirdPlatform())) {
      forceTarget(data, "deviceSerial", body.deviceId());
    }
    data.set("deviceName", body.deviceName());
    put(data, "detail", body.detail());
    put(data, "deviceAddress", body.deviceAddress());
    coordinates(data, body.longitude(), body.latitude());
    String gwProduct = StrUtil.blankToDefault(body.gwProductKey(), product.getGwProductKey());
    String gwDevice = body.gwDeviceId();
    boolean subDevice = DeviceNode.GATEWAY_SUB_DEVICE.name().equals(product.getDeviceNode());
    if (subDevice && StrUtil.isBlank(gwDevice)) gwDevice = body.extDeviceId();
    if (subDevice) {
      if (StrUtil.isBlank(gwDevice)) throw bad("子设备必须指定已绑定当前应用的父网关");
      if (StrUtil.isBlank(gwProduct)) throw bad("指定网关设备时必须同时指定网关产品");
      requireGateway(gwProduct, gwDevice);
      if (StrUtil.isNotBlank(body.extDeviceId()) && !gwDevice.equals(body.extDeviceId()))
        throw bad("子设备extDeviceId必须与已授权父网关gwDeviceId一致");
    } else if (StrUtil.isNotBlank(body.gwProductKey())
        || StrUtil.isNotBlank(gwDevice)
        || StrUtil.isNotBlank(body.slaveAddress())) {
      throw bad("只有网关子设备产品可指定网关与从站地址");
    }
    JSONObject request = envelope(app, productKey, body.deviceId(), null, DownCmd.DEV_ADD);
    request.set("data", data);
    put(request, "detail", body.detail());
    put(request, "deviceAddress", body.deviceAddress());
    put(request, "extDeviceId", subDevice ? gwDevice : body.extDeviceId());
    put(request, "gwProductKey", subDevice ? gwProduct : null);
    put(request, "gwDeviceId", gwDevice);
    put(request, "slaveAddress", body.slaveAddress());
    request.set("deptId", access.department(app));
    CommandReceipt receipt = dispatch(product.getThirdPlatform(), request, true);
    // 子设备拦截器可能重写 deviceId；只从实际持久化记录投影身份，绝不返回原始协议实体/密钥。
    IoTDevice query =
        IoTDevice.builder()
            .productKey(productKey)
            .tenantId(access.tenantId(app))
            .creatorId(app.getUnionId())
            .application(app.getAppUniqueId())
            .build();
    Object iotId = receipt.result() instanceof Map<?, ?> result ? result.get("iotId") : null;
    if (iotId instanceof String id && StrUtil.isNotBlank(id)) query.setIotId(id);
    else query.setDeviceId(body.deviceId());
    IoTDevice created = devices.selectOne(query);
    return new CreationReceipt(
        receipt.messageId(),
        receipt.accepted(),
        created == null
            ? null
            : new DeviceIdentity(
                created.getIotId(), created.getProductKey(), created.getDeviceId()));
  }

  public CommandReceipt update(String productKey, String deviceId, UpdateDevice body) {
    IoTDeviceDTO device = access.device(productKey, deviceId);
    lifecycleProtocol(device.getThirdPlatform(), DownCmd.DEV_UPDATE);
    JSONObject data = new JSONObject();
    if (body.deviceName() != null && body.deviceName().isBlank()) throw bad("设备名称不可为空白");
    // 旧协议更新器会无条件读取 deviceName，省略时必须补回原名称，避免清空。
    put(data, "deviceName", body.deviceName() == null ? device.getDeviceName() : body.deviceName());
    if (body.detail() != null && body.detail().isBlank()
        || body.deviceAddress() != null && body.deviceAddress().isBlank())
      throw bad("说明和地址传入时不可为空白；省略字段保持原值");
    put(data, "detail", body.detail());
    put(data, "deviceAddress", body.deviceAddress());
    coordinates(data, body.longitude(), body.latitude());
    if (body.deviceName() == null
        && body.detail() == null
        && body.deviceAddress() == null
        && body.longitude() == null
        && body.latitude() == null) throw bad("至少提供一个更新字段");
    JSONObject request =
        envelope(access.application(), productKey, deviceId, device.getIotId(), DownCmd.DEV_UPDATE);
    request.set("data", data);
    put(request, "detail", body.detail());
    put(request, "deviceAddress", body.deviceAddress());
    return dispatch(device.getThirdPlatform(), request, false);
  }

  public CommandReceipt delete(String productKey, String deviceId) {
    IoTDeviceDTO device = access.device(productKey, deviceId);
    lifecycleProtocol(device.getThirdPlatform(), DownCmd.DEV_DEL);
    JSONObject request =
        envelope(access.application(), productKey, deviceId, device.getIotId(), DownCmd.DEV_DEL);
    request.set("data", new JSONObject());
    return dispatch(device.getThirdPlatform(), request, false);
  }

  public CommandReceipt invoke(
      String productKey, String deviceId, String function, FunctionInvocation body) {
    OpenApiV2Access.identifier(function, "function");
    IoTDeviceDTO device = access.device(productKey, deviceId);
    if (DeviceNode.GATEWAY_SUB_DEVICE.equals(device.getDeviceNode()))
      requireGateway(device.getGwProductKey(), device.getExtDeviceId());
    if (device.getDeviceMetadata().getFunctionOrNull(function) == null) throw bad("设备物模型未定义此功能");
    Map<String, Object> functionData =
        body == null || body.data() == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(body.data());
    constrainFunctionTarget(device, function, functionData);
    JSONObject request =
        envelope(
            access.application(), productKey, deviceId, device.getIotId(), DownCmd.DEV_FUNCTION);
    request.set("function", new JSONObject().set("function", function).set("data", functionData));
    request.set("messageType", "FUNCTIONS");
    return dispatch(device.getThirdPlatform(), request, true);
  }

  /**
   * 将协议中可选择目标的字段收敛到已经通过 V2 绑定校验的设备。
   *
   * <p>这一步必须发生在构造 UnifiedDownlinkCommand 之前：多个旧适配器会优先读取嵌套 function.data，而不是外层
   * deviceId。缺失时补齐，显式不一致时拒绝，避免“外层已授权、 内层却操作另一台设备”的混淆授权。
   */
  private void constrainFunctionTarget(
      IoTDeviceDTO device, String function, Map<String, Object> data) {
    String protocol =
        StrUtil.blankToDefault(device.getThirdPlatform(), "").toLowerCase(Locale.ROOT);
    String normalizedFunction = function.toLowerCase(Locale.ROOT);
    if (MANAGED_VIDEO_PROTOCOLS.contains(protocol))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "统一视频平台请使用V2视频专用接口；通用功能入口不开放实例、原生任务及会话操作");
    if ("sip".equals(protocol) && "call".equals(function)) {
      // SIP平台会同时呼叫源端和目标端；两端都必须绑定当前应用，而不只是同一租户。
      String targetProduct =
          data.get("targetProductKey") == null
              ? device.getProductKey()
              : String.valueOf(data.get("targetProductKey"));
      String targetDevice =
          data.get("targetDeviceId") == null ? null : String.valueOf(data.get("targetDeviceId"));
      IoTDeviceDTO target = access.device(targetProduct, targetDevice);
      if (!"sip".equals(target.getThirdPlatform())) throw bad("SIP被叫设备必须是当前应用绑定的SIP设备");
      data.put("targetProductKey", targetProduct);
      data.put("targetDeviceId", targetDevice);
    }
    if ("ttlock".equals(protocol)) {
      if (!TTLOCK_DEVICE_FUNCTIONS.contains(normalizedFunction))
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "TTLock账号级功能不属于单设备V2授权范围，请使用管理端或专用同步接口");
      Integer lockId = ttlockDeviceId(device);
      if (lockId == null)
        throw new ResponseStatusException(HttpStatus.CONFLICT, "当前TTLock设备缺少可核对的lockId，暂不能通过V2调用");
      Object requested = data.get("lockId");
      if (requested != null && !String.valueOf(lockId).equals(String.valueOf(requested)))
        throw bad("TTLock功能的lockId必须与当前已授权设备一致");
      data.put("lockId", lockId);
    }
    if ("ezviz".equals(protocol)) forceTarget(data, "deviceSerial", device.getDeviceId());
  }

  private static void forceTarget(Map<String, Object> data, String key, String target) {
    Object requested = data.get(key);
    if (requested != null && !target.equals(String.valueOf(requested)))
      throw bad("功能参数" + key + "必须与当前已授权设备一致");
    data.put(key, target);
  }

  private static Integer ttlockDeviceId(IoTDeviceDTO device) {
    Integer ext = parseInteger(device.getExtDeviceId());
    if (ext != null) return ext;
    if (StrUtil.isBlank(device.getDevConfiguration())) return null;
    try {
      JSONObject root = JSONUtil.parseObj(device.getDevConfiguration());
      JSONObject ttlock = root.getJSONObject("ttlock");
      return ttlock == null ? root.getInt("lockId") : ttlock.getInt("lockId");
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static Integer parseInteger(String value) {
    if (StrUtil.isBlank(value)) return null;
    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private JSONObject envelope(
      IoTUserApplication app, String product, String device, String iotId, DownCmd cmd) {
    return new JSONObject()
        .set("productKey", product)
        .set("deviceId", device)
        .set("iotId", iotId)
        .set("appUnionId", app.getUnionId())
        .set("applicationId", app.getAppUniqueId())
        .set("tenantId", access.tenantId(app))
        .set("cmd", cmd.getValue())
        .set("msgId", IdUtil.simpleUUID());
  }

  private CommandReceipt dispatch(String protocol, JSONObject request, boolean includeResult) {
    UnifiedDownlinkCommand command =
        UnifiedDownlinkCommand.fromJson(request).withSource("openapi-v2");
    R<?> result = IoTDownlFactory.getIDownOrThrow(protocol).doAction(command);
    if (result == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "协议服务未返回处理结果");
    if (!result.isSuccess())
      throw new IoTException("协议服务拒绝操作", result.getCode() == null ? 409 : result.getCode());
    return new CommandReceipt(command.getMsgId(), true, includeResult ? result.getData() : null);
  }

  private static void put(JSONObject target, String key, Object value) {
    if (value != null) target.set(key, value);
  }

  private void requireGateway(String productKey, String deviceId) {
    IoTDeviceDTO gateway = access.device(productKey, deviceId);
    if (!DeviceNode.GATEWAY.equals(gateway.getDeviceNode())) throw bad("父设备必须是当前应用已绑定的网关设备");
  }

  private static void lifecycleProtocol(String protocol, DownCmd operation) {
    Set<String> supported = operation == DownCmd.DEV_DEL ? LIFECYCLE_DELETE : LIFECYCLE_WRITE;
    if (!supported.contains(protocol == null ? "" : protocol))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "当前协议尚未开放此V2设备生命周期操作，请使用管理端维护后绑定应用；详见协议支持矩阵");
  }

  private static void coordinates(JSONObject target, String longitude, String latitude) {
    if (longitude == null && latitude == null) return;
    if (StrUtil.hasBlank(longitude, latitude)) throw bad("经纬度需同时传入合法数值");
    try {
      if (new BigDecimal(longitude).abs().compareTo(BigDecimal.valueOf(180)) > 0
          || new BigDecimal(latitude).abs().compareTo(BigDecimal.valueOf(90)) > 0)
        throw bad("经纬度超出范围");
    } catch (NumberFormatException exception) {
      throw bad("经纬度必须为数值");
    }
    target.set("longitude", longitude).set("latitude", latitude);
  }

  private static ResponseStatusException bad(String reason) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
  }
}
