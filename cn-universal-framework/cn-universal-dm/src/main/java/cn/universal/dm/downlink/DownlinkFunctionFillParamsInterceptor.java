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

package cn.universal.dm.downlink;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.downlink.DownlinkInterceptor;
import cn.universal.core.downlink.InterceptorPhase;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.core.metadata.FunctionMetadata;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.device.shadow.DeviceShadowWrapper;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 下行功能参数自动填充拦截器 用于自动填充功能调用中缺失的参数值
 *
 * <p>使用场景：
 *
 * <ul>
 *   <li>功能调用参数补全：当用户未传递某些参数时，从设备影子中获取
 *   <li>减少前端参数传递：前端只需传递变化的参数，固定参数自动从影子获取
 *   <li>简化设备控制：例如电表抄表功能，表号可自动从影子获取
 * </ul>
 *
 * <p>执行逻辑：
 *
 * <ol>
 *   <li>仅对DEV_FUNCTION指令生效
 *   <li>解析物模型中该功能的所有输入参数
 *   <li>检查用户传递的data中是否缺少参数（null和空白字符串视为缺省）
 *   <li>依次从影子reported、desired、设备配置和允许的设备标准字段中获取缺失参数的值并回填
 *   <li>以用户传递的非空值为准，不覆盖已传递的参数（包括0和false）
 *   <li>自动填充产品自定义字段（thirdConfiguration.customField → 设备configuration → defaultValue）
 *   <li>最后以功能输入参数的物模型默认值兜底；expands.autoFill=false禁用该参数的自动补参
 * </ol>
 *
 * @version 1.0
 * @since 2025/12/11
 */
@Slf4j
@Component
@Order(350) // 在数据校验(50)之后，子设备拦截(400)之前执行
public class DownlinkFunctionFillParamsInterceptor implements DownlinkInterceptor {

  @Resource private IoTProductDeviceService ioTProductDeviceService;

  @Resource private IoTDeviceShadowService ioTDeviceShadowService;

  @Resource private IoTDeviceService ioTDeviceService;

  @Resource private StringRedisTemplate stringRedisTemplate;

  /** Redis Key前缀 */
  private static final String REDIS_KEY_PREFIX = "iot:cmd:";

  /** Redis自增序列Key前缀 */
  private static final String REDIS_SEQ_PREFIX = "iot:seq:";

  /** Redis缓存时间(秒) - 默认300秒 */
  private static final long REDIS_TTL = 300;

  @Override
  public String getName() {
    return "下行功能参数自动填充拦截器";
  }

  @Override
  public int getOrder() {
    return 350;
  }

  @Override
  public InterceptorPhase getPhase() {
    // 在消息转换之前执行，修改原始命令
    return InterceptorPhase.PRE;
  }

  @Override
  public boolean supports(DownlinkContext<?> context) {
    // 仅支持功能调用指令
    if (context.getCommand() == null) {
      return false;
    }
    return DownCmd.DEV_FUNCTION.equals(context.getCommand().getCmd());
  }

  @Override
  public boolean preHandle(DownlinkContext<?> context) throws Exception {
    try {
      // 获取命令对象
      var command = context.getCommand();
      String productKey = command.getProductKey();
      String deviceId = command.getDeviceId();
      Map<String, Object> function = command.getFunction();

      // 基本校验
      if (StrUtil.isBlank(productKey) || StrUtil.isBlank(deviceId)) {
        log.debug("[功能参数填充] 产品或设备信息缺失，跳过处理");
        return true;
      }

      if (function == null || function.isEmpty()) {
        log.debug("[功能参数填充] 功能参数为空，跳过处理");
        return true;
      }

      // 获取功能标识和data
      String functionId = (String) function.get("function");
      if (StrUtil.isBlank(functionId)) {
        log.debug("[功能参数填充] 功能标识为空，跳过处理");
        return true;
      }

      // 获取或初始化data对象
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) function.get("data");
      if (data == null) {
        data = new HashMap<>();
        function.put("data", data);
      }

      if (IoTConstant.SET_DEVICE_DESIRED_PROPERTIES.equals(functionId)) {
        String commandId = handleCommandId(data, productKey, deviceId, functionId, function);
        if (commandId != null) {
          context.setAttribute("commandId", commandId);
        }
        context.setAttribute("paramsFilled", true);
        log.debug("[功能参数填充] setDeviceDesiredProperties 按 patch 语义跳过自动补参");
        return true;
      }

      log.debug(
          "[功能参数填充] 开始处理: productKey={}, deviceId={}, function={}, inputFields={}",
          productKey,
          deviceId,
          functionId,
          data.keySet());

      // 1. 获取产品物模型，解析该功能需要的参数列表
      IoTProduct product = ioTProductDeviceService.getProduct(productKey);
      if (product == null || StrUtil.isBlank(product.getMetadata())) {
        log.warn("[功能参数填充] 产品不存在或物模型为空: productKey={}", productKey);
        return true;
      }

      // 2. 从物模型中找到对应的功能定义
      List<AbstractPropertyMetadata> requiredParams =
          getFunctionInputParams(product.getMetadata(), functionId);
      if (CollectionUtil.isEmpty(requiredParams)) {
        log.debug("[功能参数填充] 该功能无需物模型输入参数: function={}", functionId);
      }
      Set<String> manualParamIds =
          requiredParams.stream()
              .filter(param -> !isAutoFillEnabled(param))
              .map(AbstractPropertyMetadata::getId)
              .collect(Collectors.toSet());

      // 3. 检查哪些参数缺失(使用final变量避免lambda问题)
      final Map<String, Object> finalData = data;
      List<String> missingParamIds =
          requiredParams.stream()
              .map(AbstractPropertyMetadata::getId)
              .filter(StrUtil::isNotBlank)
              .filter(paramId -> !manualParamIds.contains(paramId))
              .filter(paramId -> isBlankValue(finalData.get(paramId)))
              .toList();

      if (missingParamIds.isEmpty()) {
        log.debug("[功能参数填充] 所有参数已传递，无需填充");
        // 注意:不要提前返回,还需要执行 handleCommandId() 逻辑
      } else {
        log.debug("[功能参数填充] 检测到缺失参数: {}", missingParamIds);

        List<String> remainingParamIds = new ArrayList<>(missingParamIds);
        // 4. 从设备影子获取缺失参数的值
        int filledCount = 0;
        DeviceShadowWrapper shadow =
            ioTDeviceShadowService.getDeviceShadowWrapper(productKey, deviceId);
        if (shadow != null && !shadow.isEmpty()) {
          // 从影子中提取参数值
          for (String paramId : missingParamIds) {
            Object value = shadow.getReported(paramId);
            if (isBlankValue(value) || !isSimpleType(value)) {
              value = shadow.getDesired(paramId);
            }
            if (!isBlankValue(value) && isSimpleType(value)) {
              data.put(paramId, value);
              filledCount++;
              remainingParamIds.remove(paramId);
              log.debug("[功能参数填充] 从影子回填参数: {}", paramId);
            }
          }
        }

        // 5. 影子中未找到的参数，尝试从设备devConfiguration获取
        if (!remainingParamIds.isEmpty()) {
          IoTDeviceDTO deviceDTO = ioTDeviceService.selectDevInstanceBO(productKey, deviceId);
          JSONObject config = null;
          if (deviceDTO != null && StrUtil.isNotBlank(deviceDTO.getDevConfiguration())) {
            try {
              config = JSONUtil.parseObj(deviceDTO.getDevConfiguration());
            } catch (Exception e) {
              log.warn(
                  "[功能参数填充] 解析设备configuration失败: iotId={}, errorType={}",
                  deviceDTO.getIotId(),
                  e.getClass().getSimpleName());
            }
          }
          for (String paramId : remainingParamIds) {
            Object value = config == null ? null : config.get(paramId);
            if (isBlankValue(value) || !isSimpleType(value)) {
              value = getStandardDeviceField(deviceDTO, paramId);
            }
            if (!isBlankValue(value) && isSimpleType(value)) {
              data.put(paramId, value);
              filledCount++;
              log.debug("[功能参数填充] 从设备配置或标准字段回填参数: {}", paramId);
            }
          }
        }

        if (filledCount > 0) {
          // 必须回写到function对象！
          // 原因：hutool JSONObject.get() 返回的是副本，修改副本不影响原对象
          // 验证日志显示 dataCheck != data (引用不同)，因此必须显式 put 回去
          function.put("data", data);
          context.setAttribute("filledCount", filledCount);
          log.info(
              "[功能参数填充] 成功回填 {} 个参数: productKey={}, deviceId={}, function={}, fields={}",
              filledCount,
              productKey,
              deviceId,
              functionId,
              data.keySet());
        } else {
          log.debug("[功能参数填充] 未找到缺失参数的值");
        }
      }

      // 6. 填充产品自定义字段（thirdConfiguration.customField → 设备devConfiguration → defaultValue）
      int customFieldFilledCount =
          fillCustomFieldParams(product, data, productKey, deviceId, manualParamIds);
      if (customFieldFilledCount > 0) {
        function.put("data", data);
        Integer prevCount = context.getAttribute("filledCount");
        context.setAttribute(
            "filledCount", (prevCount != null ? prevCount : 0) + customFieldFilledCount);
        log.info(
            "[自定义字段填充] 成功回填 {} 个自定义字段: productKey={}, deviceId={}, fields={}",
            customFieldFilledCount,
            productKey,
            deviceId,
            data.keySet());
      }

      // 7. 最后使用功能输入参数的物模型默认值兜底，保留已有自定义字段默认值的优先级
      List<String> defaultParamIds =
          missingParamIds.stream().filter(paramId -> isBlankValue(finalData.get(paramId))).toList();
      if (!defaultParamIds.isEmpty()) {
        Map<String, Object> metadataDefaults =
            getFunctionInputDefaults(product.getMetadata(), functionId);
        int defaultFilledCount = 0;
        for (String paramId : defaultParamIds) {
          Object value = metadataDefaults.get(paramId);
          if (!isBlankValue(value) && isSimpleType(value)) {
            data.put(paramId, value);
            defaultFilledCount++;
            log.debug("[功能参数填充] 从物模型默认值回填参数: {}", paramId);
          }
        }
        if (defaultFilledCount > 0) {
          function.put("data", data);
          Integer prevCount = context.getAttribute("filledCount");
          context.setAttribute(
              "filledCount", (prevCount != null ? prevCount : 0) + defaultFilledCount);
        }
      }

      // ===== 异步指令追踪: 处理 commandId =====
      String commandId = handleCommandId(data, productKey, deviceId, functionId, function);
      if (commandId != null) {
        context.setAttribute("commandId", commandId);
      }
      // ===== commandId 处理完成 =====

      // 记录到上下文
      context.setAttribute("paramsFilled", true);

    } catch (Exception e) {
      log.error(
          "[功能参数填充] 处理异常: productKey={}, deviceId={}, errorType={}",
          context.getCommand().getProductKey(),
          context.getCommand().getDeviceId(),
          e.getClass().getSimpleName());
      // 不中断执行，继续后续流程
    }

    return true; // 继续执行
  }

  @Override
  public void postHandle(DownlinkContext<?> context) throws Exception {
    // 后置处理：记录填充结果
    Boolean filled = context.getAttribute("paramsFilled");
    Integer filledCount = context.getAttribute("filledCount");

    if (Boolean.TRUE.equals(filled) && filledCount != null && filledCount > 0) {
      log.debug("[功能参数填充][PostHandle] 参数填充完成: filledCount={}", filledCount);
    }
  }

  @Override
  public void afterCompletion(DownlinkContext<?> context, Exception ex) {
    if (ex != null) {
      log.error(
          "[功能参数填充][AfterCompletion] 下行处理异常: productKey={}, deviceId={}, error={}",
          context.getCommand().getProductKey(),
          context.getCommand().getDeviceId(),
          ex.getMessage());
    }
  }

  // ==================== 私有辅助方法 ====================

  /**
   * 填充产品自定义字段参数
   *
   * <p>从产品的 thirdConfiguration.customField 中获取自定义字段定义， 当下行命令 data 中缺失这些字段时，按优先级自动填充：
   *
   * <ol>
   *   <li>设备 devConfiguration 中的值（设备添加时录入的自定义字段值）
   *   <li>customField 定义中的 defaultValue（字段默认值）
   * </ol>
   *
   * <p>典型场景：Modbus 设备的 slaveAddress 字段，在设备添加时已配置， 下发指令时无需用户再次传递。
   *
   * @param product 产品实体
   * @param data 功能调用的 data 参数（可变，会被修改）
   * @param productKey 产品标识
   * @param deviceId 设备ID
   * @return 成功填充的字段数量
   */
  private int fillCustomFieldParams(
      IoTProduct product,
      Map<String, Object> data,
      String productKey,
      String deviceId,
      Set<String> manualParamIds) {

    // 1. 解析产品的 thirdConfiguration
    if (StrUtil.isBlank(product.getThirdConfiguration())) {
      return 0;
    }

    JSONObject thirdConfig;
    try {
      thirdConfig = JSONUtil.parseObj(product.getThirdConfiguration());
    } catch (Exception e) {
      log.warn(
          "[自定义字段填充] 解析thirdConfiguration失败: productKey={}, errorType={}",
          productKey,
          e.getClass().getSimpleName());
      return 0;
    }

    // 2. 提取 customField 列表
    List<JSONObject> customFields = thirdConfig.getBeanList("customField", JSONObject.class);
    if (CollectionUtil.isEmpty(customFields)) {
      return 0;
    }

    // 3. 筛选缺失或空值的自定义字段
    List<JSONObject> missingFields =
        customFields.stream()
            .filter(field -> StrUtil.isNotBlank(field.getStr("id")))
            .filter(field -> !manualParamIds.contains(field.getStr("id")))
            .filter(field -> isBlankValue(data.get(field.getStr("id"))))
            .toList();

    if (missingFields.isEmpty()) {
      log.debug("[自定义字段填充] 所有自定义字段已传递，无需填充");
      return 0;
    }

    log.debug(
        "[自定义字段填充] 检测到缺失的自定义字段: {}",
        missingFields.stream().map(f -> f.getStr("id")).toList());

    // 4. 加载设备 devConfiguration（设备添加时录入的自定义字段值）
    JSONObject deviceConfig = loadDeviceConfig(productKey, deviceId);

    // 5. 按优先级填充: 设备devConfiguration > customField.defaultValue
    int filledCount = 0;
    for (JSONObject field : missingFields) {
      String fieldId = field.getStr("id");

      // 优先从设备 devConfiguration 获取
      if (deviceConfig != null) {
        Object value = deviceConfig.get(fieldId);
        if (!isBlankValue(value) && isSimpleType(value)) {
          data.put(fieldId, value);
          filledCount++;
          log.debug("[自定义字段填充] 从设备configuration回填: {}", fieldId);
          continue;
        }
      }

      // 其次从 customField.defaultValue 获取
      Object defaultValue = field.get("defaultValue");
      if (!isBlankValue(defaultValue) && isSimpleType(defaultValue)) {
        data.put(fieldId, defaultValue);
        filledCount++;
        log.debug("[自定义字段填充] 从字段默认值回填: {}", fieldId);
      }
    }

    return filledCount;
  }

  /**
   * 加载设备 configuration 配置
   *
   * @param productKey 产品标识
   * @param deviceId 设备ID
   * @return 解析后的配置JSON对象，解析失败返回 null
   */
  private JSONObject loadDeviceConfig(String productKey, String deviceId) {
    try {
      IoTDeviceDTO deviceDTO = ioTDeviceService.selectDevInstanceBO(productKey, deviceId);
      if (deviceDTO != null && StrUtil.isNotBlank(deviceDTO.getDevConfiguration())) {
        return JSONUtil.parseObj(deviceDTO.getDevConfiguration());
      }
    } catch (Exception e) {
      log.warn(
          "[自定义字段填充] 加载设备configuration失败: productKey={}, deviceId={}, errorType={}",
          productKey,
          deviceId,
          e.getClass().getSimpleName());
    }
    return null;
  }

  /**
   * 判断值是否为空白值（null、空字符串、纯空白字符串）
   *
   * @param value 待判断的值
   * @return true-空白值, false-有效值
   */
  private boolean isBlankValue(Object value) {
    if (JSONUtil.isNull(value)) {
      return true;
    }
    if (value instanceof String) {
      return StrUtil.isBlank((String) value);
    }
    return false;
  }

  private boolean isAutoFillEnabled(AbstractPropertyMetadata param) {
    Map<String, Object> expands = param.getExpands();
    return expands == null || !"false".equalsIgnoreCase(String.valueOf(expands.get("autoFill")));
  }

  /** 仅允许标识类标准字段补参，不从设备实体中猜测其他参数或读取凭据。 */
  private Object getStandardDeviceField(IoTDeviceDTO device, String paramId) {
    if (device == null) {
      return null;
    }
    return switch (paramId) {
      case "deviceId" -> device.getDeviceId();
      case "iotId" -> device.getIotId();
      case "deviceName" -> device.getDeviceName();
      case "productKey" -> device.getProductKey();
      case "extDeviceId" -> device.getExtDeviceId();
      default -> null;
    };
  }

  /**
   * 处理异步指令追踪的commandId
   *
   * <p>逻辑：
   *
   * <ol>
   *   <li>如果data中已有commandId，使用用户传入的值
   *   <li>如果没有，生成UUID作为commandId
   *   <li>将commandId写入Redis: iot:cmd:{productKey}:{deviceId}:{functionId}
   *   <li>设置TTL=60秒(可配置)
   *   <li>将commandId回写到data中
   * </ol>
   *
   * @param data 功能调用的data参数
   * @param productKey 产品标识
   * @param deviceId 设备ID
   * @param functionId 功能ID
   * @param function 完整的function对象(用于回写)
   * @return commandId
   */
  private String handleCommandId(
      final Map<String, Object> data,
      String productKey,
      String deviceId,
      String functionId,
      Map<String, Object> function) {
    try {
      // 1. 获取或生成 commandId
      String commandId = (String) data.get("commandId");
      boolean isUserProvided = StrUtil.isNotBlank(commandId);

      if (!isUserProvided) {
        // 使用Redis自增生成短commandId,格式: {productKey}:{deviceId}:{seq}
        // 优点: 短小、有业务含义、与设备关联、保证唯一性
        commandId = generateCommandId(productKey, deviceId, functionId);
        log.debug(
            "[异步指令追踪] 生成commandId: productKey={}, deviceId={}, functionId={}, commandId={}",
            productKey,
            deviceId,
            functionId,
            commandId);
      } else {
        log.debug(
            "[异步指令追踪] 使用用户提供的commandId: productKey={}, deviceId={}, functionId={}, commandId={}",
            productKey,
            deviceId,
            functionId,
            commandId);
      }

      // 2. 写入Redis
      String redisKey = buildRedisKey(productKey, deviceId, functionId);
      stringRedisTemplate.opsForValue().set(redisKey, commandId, REDIS_TTL, TimeUnit.SECONDS);

      log.info(
          "[异步指令追踪] Redis缓存写入成功: key={}, commandId={}, TTL={}秒", redisKey, commandId, REDIS_TTL);

      // 3. 回写到data (如果是新生成的)
      if (!isUserProvided) {
        data.put("commandId", commandId);
        function.put("data", data);
        log.debug("[异步指令追踪] commandId已回写到data");
      }

      return commandId;

    } catch (Exception e) {
      log.error(
          "[异步指令追踪] 处理commandId异常: productKey={}, deviceId={}, functionId={}, error={}",
          productKey,
          deviceId,
          functionId,
          e.getMessage(),
          e);
      return null;
    }
  }

  /**
   * 生成短commandId (使用Redis自增)
   *
   * <p>格式: {productKey}:{deviceId}:{seq}
   *
   * <p>示例: HnDdSnj1kiQg:250707ea8dda:1001
   *
   * <p>优点:
   *
   * <ul>
   *   <li>长度短 (~30字符 vs UUID 32字符)
   *   <li>有业务含义 (可直接看出设备信息)
   *   <li>保证唯一性 (Redis自增)
   *   <li>便于排查 (日志中可直接识别设备)
   * </ul>
   *
   * @param productKey 产品标识
   * @param deviceId 设备ID
   * @return commandId
   */
  private String generateCommandId(String productKey, String deviceId, String function) {
    try {
      // 1. 构建Redis自增Key
      String seqKey = REDIS_SEQ_PREFIX + productKey + ":" + deviceId;

      // 2. 获取自增序号 (从1开始)
      Long seq = stringRedisTemplate.opsForValue().increment(seqKey, 1);

      // 3. 构建commandId: {function}:{seq}
      String commandId = function + ":" + seq;

      log.debug(
          "[异步指令追踪] Redis自增生成commandId: seqKey={}, seq={}, commandId={}", seqKey, seq, commandId);

      return commandId;

    } catch (Exception e) {
      log.error(
          "[异步指令追踪] Redis自增失败,降级使用UUID: productKey={}, deviceId={}, error={}",
          productKey,
          deviceId,
          e.getMessage());

      // 降级方案: 使用UUID (去除横线)
      return IdUtil.objectId();
    }
  }

  /**
   * 构建Redis Key
   *
   * @param productKey 产品标识
   * @param deviceId 设备ID
   * @param functionId 功能ID
   * @return Redis Key格式: iot:cmd:{productKey}:{deviceId}:{functionId}
   */
  private String buildRedisKey(String productKey, String deviceId, String functionId) {
    return REDIS_KEY_PREFIX + productKey + ":" + deviceId + ":" + functionId;
  }

  /**
   * 判断是否为简单类型（字符串、数字、布尔值） 忽略复杂类型：JSONObject、JSONArray、Map、List、Array
   *
   * @param value 待判断的值
   * @return true-简单类型, false-复杂类型
   */
  private boolean isSimpleType(Object value) {
    if (value == null) {
      return false;
    }

    // 字符串类型
    if (value instanceof String) {
      return true;
    }

    // 数字类型：Integer, Long, Double, Float, Short, Byte
    if (value instanceof Number) {
      return true;
    }

    // 布尔类型
    if (value instanceof Boolean) {
      return true;
    }

    // 忽略复杂类型
    // JSONObject, JSONArray, Map, List, Array 等
    return false;
  }

  /**
   * 从物模型中获取指定功能的输入参数列表
   *
   * @param metadata 物模型JSON字符串
   * @param functionId 功能标识
   * @return 输入参数列表，如果未找到返回空列表
   */
  private List<AbstractPropertyMetadata> getFunctionInputParams(
      String metadata, String functionId) {
    try {
      JSONObject metadataJson = JSONUtil.parseObj(metadata);

      // 获取functions数组
      List<JSONObject> functions = metadataJson.getBeanList("functions", JSONObject.class);
      if (CollectionUtil.isEmpty(functions)) {
        return List.of();
      }

      // 查找指定的功能
      for (JSONObject func : functions) {
        String id = func.getStr("id");
        if (functionId.equals(id)) {
          // 找到对应功能，解析其inputs参数
          FunctionMetadata functionMetadata = new FunctionMetadata(func);
          return functionMetadata.getInputs();
        }
      }

      log.debug("[功能参数填充] 物模型中未找到功能定义: functionId={}", functionId);
      return List.of();

    } catch (Exception e) {
      log.error(
          "[功能参数填充] 解析物模型失败: functionId={}, errorType={}",
          functionId,
          e.getClass().getSimpleName());
      return List.of();
    }
  }

  /**
   * 读取功能输入参数的默认值。
   *
   * <p>PropertyMetadata 为了保持物模型类型兼容不会保留所有原始字段，因此这里从产品
   * metadata 原文读取默认值，兼容历史上出现过的几种位置。
   */
  private Map<String, Object> getFunctionInputDefaults(String metadata, String functionId) {
    Map<String, Object> defaults = new HashMap<>();
    try {
      JSONObject metadataJson = JSONUtil.parseObj(metadata);
      List<JSONObject> functions = metadataJson.getBeanList("functions", JSONObject.class);
      if (CollectionUtil.isEmpty(functions)) {
        return defaults;
      }

      JSONObject function =
          functions.stream()
              .filter(item -> functionId.equals(item.getStr("id")))
              .findFirst()
              .orElse(null);
      if (function == null) {
        return defaults;
      }

      List<JSONObject> inputs = function.getBeanList("inputs", JSONObject.class);
      if (CollectionUtil.isEmpty(inputs)) {
        return defaults;
      }
      for (JSONObject input : inputs) {
        String paramId = input.getStr("id");
        if (StrUtil.isBlank(paramId)) {
          continue;
        }
        Object value = firstValue(input, "defaultValue", "default");
        if (value == null) {
          value = firstValue(input.getJSONObject("expands"), "defaultValue", "default");
        }
        if (value == null) {
          value = firstValue(input.getJSONObject("valueType"), "defaultValue", "default");
        }
        if (value != null) {
          defaults.put(paramId, value);
        }
      }
    } catch (Exception e) {
      log.warn(
          "[功能参数填充] 读取物模型默认值失败: functionId={}, errorType={}",
          functionId,
          e.getClass().getSimpleName());
    }
    return defaults;
  }

  private Object firstValue(JSONObject json, String... keys) {
    if (json == null) {
      return null;
    }
    for (String key : keys) {
      if (json.containsKey(key)) {
        Object value = json.get(key);
        if (!isBlankValue(value) && isSimpleType(value)) {
          return value;
        }
      }
    }
    return null;
  }
}
