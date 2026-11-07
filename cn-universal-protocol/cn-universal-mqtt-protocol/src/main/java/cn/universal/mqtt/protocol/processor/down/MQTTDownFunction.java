package cn.universal.mqtt.protocol.processor.down;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.domain.R;
import cn.universal.common.utils.PayloadCodecUtils;
import cn.universal.core.message.DownCommonData;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.mqtt.protocol.config.MqttConstant;
import cn.universal.mqtt.protocol.entity.MQTTDownRequest;
import cn.universal.mqtt.protocol.processor.MQTTDownMessageProcessor;
import cn.universal.mqtt.protocol.support.MqttDownErrors;
import cn.universal.mqtt.protocol.support.MqttRouteResolver;
import cn.universal.mqtt.protocol.support.MqttRouteResolver.MqttRoute;
import cn.universal.mqtt.protocol.system.SysMQTTManager;
import cn.universal.mqtt.protocol.third.ThirdMQTTServerManager;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import cn.universal.mqtt.protocol.topic.MQTTTopicType;
import cn.universal.persistence.base.DeviceCommandSender;
import cn.universal.persistence.base.IoTDeviceLifeCycle;
import cn.universal.persistence.base.IoTDownAdapter;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** MQTT 功能下发处理器，负责设备校验、主题解析、报文编码和发送结果处理。 */
@Component
@Slf4j(topic = "mqtt")
public class MQTTDownFunction extends IoTDownAdapter<MQTTDownRequest>
    implements MQTTDownMessageProcessor, DeviceCommandSender {

  private static final String SUPPORTED_PROTOCOL = "MQTT";
  private static final String GATEWAY_POLLING_FUNCTION = "gateway_polling";

  @Autowired private MqttRouteResolver mqttRouteResolver;
  @Autowired private SysMQTTManager sysMQTTManager;
  @Autowired private ThirdMQTTServerManager thirdMqttServerManager;

  @Autowired private IoTProductDeviceService ioTProductDeviceService;

  @Resource private IoTDeviceMapper ioTDeviceMapper;

  @Resource private MQTTTopicManager mqttTopicManager;

  @Resource private IoTProductMapper ioTProductMapper;

  @Resource private IoTDeviceService ioTDeviceService;

  @Resource(name = "ioTDeviceActionAfterService")
  private IoTDeviceLifeCycle ioTDeviceLifeCycle;

  // ==================== DeviceCommandSender Implementation ====================

  @Override
  public String getSupportedProtocol() {
    return SUPPORTED_PROTOCOL;
  }

  @Override
  public R<?> sendCommand(String productKey, String deviceId, String payload) {
    log.info(
        "[MQTTDownFunction] Gateway polling command: productKey={}, deviceId={}, payload={}",
        productKey,
        deviceId,
        payload);

    try {
      MQTTDownRequest request = buildGatewayPollingRequest(productKey, deviceId, payload);
      populateRequestContext(request);
      return process(request);
    } catch (Exception e) {
      log.error(
          "[MQTTDownFunction] Gateway polling command failed: productKey={}, deviceId={}",
          productKey,
          deviceId,
          e);
      return MqttDownErrors.sendCommandFailed(e);
    }
  }

  // ==================== Original Process Method ====================

  @Override
  public R<?> process(MQTTDownRequest downRequest) {
    // 先把设备和产品确认好，后续主题解析、编解码都依赖这些上下文。
    ValidationResult validation = validateDeviceAndProduct(downRequest);
    if (!validation.isValid()) {
      return validation.getErrorResponse();
    }

    // 全局功能命中时直接返回，不再继续往 MQTT broker 发送。
    R<?> globalFunctionResult =
        callGlobalFunction(validation.getProduct(), validation.getDevice(), downRequest);
    if (Objects.nonNull(globalFunctionResult)) {
      return globalFunctionResult;
    }

    IoTProduct product = validation.getProduct();
    MqttRoute route = mqttRouteResolver.resolveProductRoute(downRequest.getProductKey(), product);
    String downTopic = resolveDownTopic(downRequest, product, route);
    if (StrUtil.isBlank(downTopic)) {
      return MqttDownErrors.missingDownTopic();
    }

    // commandId、payload 和 encoderType 在这里统一收口，后面的流程只消费结果。
    CommandContext commandContext = buildCommandContext(downRequest);
    EncodeResult encodeResult =
        encodePayload(
            commandContext.getEncoderType(),
            commandContext.getPayload(),
            downRequest.getPayloadSource());
    if (!encodeResult.isSuccess()) {
      log.warn(
          "[MQTTDownFunction] 下发payload编码失败: productKey={}, deviceId={}, encoderType={}, payloadSource={}, payload={}, error={}",
          downRequest.getProductKey(),
          downRequest.getDeviceId(),
          commandContext.getEncoderType(),
          downRequest.getPayloadSource(),
          commandContext.getPayload(),
          encodeResult.getErrorMessage());
      return MqttDownErrors.codecFailure(encodeResult.getErrorMessage());
    }

    boolean success = pushMessage(route, downTopic, encodeResult.getPayloadBytes());
    return buildSendResponse(downRequest, commandContext, success);
  }

  private MQTTDownRequest buildGatewayPollingRequest(
      String productKey, String deviceId, String payload) {
    MQTTDownRequest request = new MQTTDownRequest();
    request.setProductKey(productKey);
    request.setDeviceId(deviceId);
    request.setCmd(DownCmd.DEV_FUNCTION);
    request.setPayload(payload);

    Map<String, Object> function = new HashMap<>();
    function.put("function", GATEWAY_POLLING_FUNCTION);
    request.setFunction(function);
    return request;
  }

  private void populateRequestContext(MQTTDownRequest request) {
    // sendCommand 复用统一 process 前，提前把产品和设备上下文装进请求。
    IoTProduct product = loadProduct(request.getProductKey());
    if (product != null) {
      request.setIoTProduct(product);
    }

    IoTDevice device = loadDevice(request.getProductKey(), request.getDeviceId());
    if (device != null) {
      request.setIoTDeviceDTO(toDeviceDTO(device));
    }
  }

  private ValidationResult validateDeviceAndProduct(MQTTDownRequest downRequest) {
    IoTDevice device = loadDevice(downRequest.getProductKey(), downRequest.getDeviceId());
    if (device == null) {
      return ValidationResult.error(MqttDownErrors.deviceNotFound());
    }
    return ValidationResult.success(resolveProduct(downRequest), device);
  }

  private IoTProduct resolveProduct(MQTTDownRequest downRequest) {
    IoTProduct product = downRequest.getIoTProduct();
    if (product != null) {
      return product;
    }
    product = loadProduct(downRequest.getProductKey());
    if (product != null) {
      downRequest.setIoTProduct(product);
    }
    return product;
  }

  private IoTProduct loadProduct(String productKey) {
    return ioTProductMapper.selectOne(IoTProduct.builder().productKey(productKey).build());
  }

  private IoTDevice loadDevice(String productKey, String deviceId) {
    return ioTDeviceMapper.selectOne(
        IoTDevice.builder().productKey(productKey).deviceId(deviceId).build());
  }

  private IoTDeviceDTO toDeviceDTO(IoTDevice device) {
    IoTDeviceDTO deviceDTO = new IoTDeviceDTO();
    BeanUtils.copyProperties(device, deviceDTO);
    return deviceDTO;
  }

  private String resolveDownTopic(
      MQTTDownRequest downRequest, IoTProduct product, MqttRoute route) {
    // 先应用产品端云配置里的自定义主题，再回退到系统/第三方默认主题规则。
    applyConfiguredDownTopics(downRequest, product);
    return buildDownTopic(downRequest, route);
  }

  private CommandContext buildCommandContext(MQTTDownRequest downRequest) {
    Map<String, Object> function = downRequest.getFunction();
    if (function == null) {
      function = new HashMap<>();
      downRequest.setFunction(function);
    }

    String commandId = resolveCommandId(downRequest);
    function.put("commandId", commandId);

    String payload = downRequest.getPayload();
    if (StrUtil.isBlank(payload)) {
      // 当业务编解码没有返回 payload 时，保持原有行为，回退为 function JSON。
      payload = JSONUtil.toJsonStr(function);
    }

    String encoderType = ioTProductDeviceService.getProductEncoderType(downRequest.getProductKey());
    return new CommandContext(commandId, payload, encoderType);
  }

  private String resolveCommandId(MQTTDownRequest downRequest) {
    DownCommonData downCommonData = downRequest.getDownCommonData();
    if (downCommonData != null && downCommonData.getCommandId() != null) {
      return downCommonData.getCommandId();
    }
    return null;
  }

  private R<?> buildSendResponse(
      MQTTDownRequest downRequest, CommandContext commandContext, boolean success) {
    Map<String, Object> response = new HashMap<>();
    if (success) {
      // 成功时返回格式化后的 payload，并补记设备指令日志。
      populateResponsePayload(response, commandContext.getPayload());
      response.put("success", true);
      saveCommandLog(downRequest, commandContext.getCommandId(), commandContext.getPayload());
    } else {
      // MQTT broker 发失败时，沿用原逻辑把设备状态标记为离线提示调用方。
      log.info("buildSendResponse 离线消息事件");
      ioTDeviceLifeCycle.offline(downRequest.getProductKey(), downRequest.getDeviceId());
      response.put("msg", MqttDownErrors.deviceMaybeOfflineMessage());
    }
    response.put("commandId", commandContext.getCommandId());
    return R.ok(response);
  }

  private void populateResponsePayload(Map<String, Object> response, String payload) {
    if (JSONUtil.isTypeJSON(payload)) {
      response.putAll(JSONUtil.parseObj(payload));
      return;
    }
    response.put("payload", payload);
  }

  private void saveCommandLog(MQTTDownRequest downRequest, String commandId, String payload) {
    JSONObject commandBody = new JSONObject();
    commandBody.set("commandId", commandId);
    commandBody.set("function", downRequest.getFunction().getOrDefault("function", ""));
    commandBody.set("payload", payload);
    if (downRequest.getData() != null) {
      commandBody.set("data", downRequest.getData());
    }
    ioTDeviceLifeCycle.command(downRequest.getIoTDeviceDTO(), commandId, commandBody);
    if (StrUtil.isNotBlank(downRequest.getSubDeviceId())
        && StrUtil.isNotBlank(downRequest.getSubProductKey())) {
      IoTDeviceDTO subIoTDeviceDTO =
          ioTDeviceService.selectDevInstanceBO(
              downRequest.getSubProductKey(), downRequest.getSubDeviceId());
      ioTDeviceLifeCycle.command(subIoTDeviceDTO, commandId, commandBody);
    }
  }

  private void applyConfiguredDownTopics(MQTTDownRequest downRequest, IoTProduct ioTProduct) {
    if (ioTProduct == null || StrUtil.isBlank(ioTProduct.getConfiguration())) {
      return;
    }
    try {
      JSONObject jsonObject = JSONUtil.parseObj(ioTProduct.getConfiguration());
      if (StrUtil.isBlank(downRequest.getDownTopic()) && jsonObject.containsKey("downTopic")) {
        String downTopic = jsonObject.getStr("downTopic", null);
        if (StrUtil.isNotBlank(downTopic)) {
          downRequest.setDownTopic(downTopic);
        }
      }
      if (StrUtil.isBlank(downRequest.getSubDeviceDownTopic())
          && jsonObject.containsKey("subDeviceDownTopic")) {
        String subDeviceDownTopic = jsonObject.getStr("subDeviceDownTopic", null);
        if (StrUtil.isNotBlank(subDeviceDownTopic)) {
          downRequest.setSubDeviceDownTopic(subDeviceDownTopic);
        }
      }
    } catch (Exception e) {
      log.warn(
          "[MQTTDownFunction] 解析产品下发主题配置失败, productKey={}, error={}",
          ioTProduct.getProductKey(),
          e.getMessage());
    }
  }

  private String buildDownTopic(MQTTDownRequest downRequest, MqttRoute route) {
    Map<String, String> topicVariables = buildTopicVariables(downRequest);
    String downTopic = resolveConfiguredDownTopicPattern(downRequest);
    if (StrUtil.isBlank(downTopic)) {
      return buildReplyTopic(downRequest, topicVariables, route);
    }
    return mqttTopicManager.fillTopicPattern(downTopic, topicVariables);
  }

  private String resolveConfiguredDownTopicPattern(MQTTDownRequest downRequest) {
    if (Boolean.TRUE.equals(downRequest.getIsGatewayProxy())
        && StrUtil.isNotBlank(downRequest.getSubDeviceDownTopic())) {
      return downRequest.getSubDeviceDownTopic();
    }
    return downRequest.getDownTopic();
  }

  private EncodeResult encodePayload(String encoderType, String payload, String payloadSource) {
    if (StrUtil.isBlank(payload)) {
      return EncodeResult.failure(MqttDownErrors.emptyEncodePayloadMessage());
    }
    String normalizedEncoderType = normalizeEncoderType(encoderType);
    if (PayloadCodecUtils.getCodec(normalizedEncoderType).isEmpty()) {
      return EncodeResult.failure(MqttDownErrors.unsupportedEncoderTypeMessage(normalizedEncoderType));
    }
    try {
      byte[] payloadBytes = PayloadCodecUtils.encode(normalizedEncoderType, payload);
      if (payloadBytes != null) {
        return EncodeResult.success(payloadBytes);
      }
    } catch (Exception e) {
      return EncodeResult.failure(
          MqttDownErrors.payloadEncodeFailureMessage(normalizedEncoderType, payloadSource, e));
    }
    return EncodeResult.failure(
        MqttDownErrors.payloadEncodeFailureMessage(normalizedEncoderType, payloadSource, null));
  }

  private String normalizeEncoderType(String encoderType) {
    return StrUtil.blankToDefault(encoderType, PayloadCodecUtils.CodecType.STRING.name())
        .trim()
        .toUpperCase(Locale.ROOT);
  }

  private boolean pushMessage(MqttRoute route, String downTopic, byte[] payloadBytes) {
    if (route.isSystem()) {
      if (!sysMQTTManager.isEnabled()) {
        log.warn("[MQTTDownFunction] 系统MQTT未启用，无法下发，topic={}", downTopic);
        return false;
      }
      // 内置 broker 直接按字节数组发送，保留产品编码后的原始内容。
      return sysMQTTManager.publishMessage(
          downTopic, payloadBytes, sysMQTTManager.getConfig().getDefaultQos(), false);
    }
    // 第三方 broker 也直接发送原始字节，避免 HEX 报文经过字符集转换后被改写。
    return thirdMqttServerManager.publishMessage(
        route.getNetworkUnionId(), downTopic, payloadBytes);
  }

  /** 构建回复topic */
  protected String buildReplyTopic(MQTTDownRequest request) {
    return buildReplyTopic(
        request,
        buildTopicVariables(request),
        mqttRouteResolver.resolveProductRoute(request.getProductKey(), request.getIoTProduct()));
  }

  private String buildReplyTopic(
      MQTTDownRequest request, Map<String, String> topicVariables, MqttRoute route) {
    String productKey = request.getProductKey();
    String deviceId = request.getDeviceId();
    // 未绑定第三方的MQTT通道，使用内置MQTT服务，则直接按照内置的主题规则
    if (route.isSystem()) {
      return MQTTTopicType.THING_DOWN.buildTopic(productKey, deviceId);
    }
    // 1. 优先查第三方MQTT自定义下发topic
    String thirdPartyDownPattern =
        mqttTopicManager.getThirdPartyDownTopicPattern(
            route.getNetworkUnionId(), MqttConstant.TYPE_DOWN);
    if (thirdPartyDownPattern != null) {
      return mqttTopicManager.fillTopicPattern(thirdPartyDownPattern, topicVariables);
    }
    return MQTTTopicType.THING_DOWN.buildTopic(productKey, deviceId);
  }

  private Map<String, String> buildTopicVariables(MQTTDownRequest downRequest) {
    Map<String, String> variables = new HashMap<>();
    String gatewayProductKey = resolveGatewayProductKey(downRequest);
    String gatewayDeviceId = resolveGatewayDeviceId(downRequest);
    variables.put("productKey", StrUtil.blankToDefault(downRequest.getProductKey(), ""));
    variables.put("deviceId", StrUtil.blankToDefault(downRequest.getDeviceId(), ""));
    variables.put("gatewayProductKey", StrUtil.blankToDefault(gatewayProductKey, ""));
    variables.put("gatewayDeviceId", StrUtil.blankToDefault(gatewayDeviceId, ""));
    variables.put("gwProductKey", StrUtil.blankToDefault(gatewayProductKey, ""));
    variables.put("gwDeviceId", StrUtil.blankToDefault(gatewayDeviceId, ""));
    variables.put("gatewayPk", StrUtil.blankToDefault(gatewayProductKey, ""));
    variables.put("gatewayDk", StrUtil.blankToDefault(gatewayDeviceId, ""));
    variables.put("subProductKey", StrUtil.blankToDefault(downRequest.getSubProductKey(), ""));
    variables.put("subDeviceId", StrUtil.blankToDefault(downRequest.getSubDeviceId(), ""));
    variables.put("subPk", StrUtil.blankToDefault(downRequest.getSubProductKey(), ""));
    variables.put("subDk", StrUtil.blankToDefault(downRequest.getSubDeviceId(), ""));
    variables.put("imei", StrUtil.blankToDefault(downRequest.getDeviceId(), ""));
    variables.put("unionId", StrUtil.blankToDefault(downRequest.getAppUnionId(), ""));
    variables.put("appUnionId", StrUtil.blankToDefault(downRequest.getAppUnionId(), ""));
    return variables;
  }

  private String resolveGatewayProductKey(MQTTDownRequest downRequest) {
    if (Boolean.TRUE.equals(downRequest.getIsGatewayProxy())
        && StrUtil.isNotBlank(downRequest.getProductKey())) {
      return downRequest.getProductKey();
    }
    if (StrUtil.isNotBlank(downRequest.getGwProductKey())) {
      return downRequest.getGwProductKey();
    }
    return downRequest.getProductKey();
  }

  private String resolveGatewayDeviceId(MQTTDownRequest downRequest) {
    if (Boolean.TRUE.equals(downRequest.getIsGatewayProxy())
        && StrUtil.isNotBlank(downRequest.getDeviceId())) {
      return downRequest.getDeviceId();
    }
    if (StrUtil.isNotBlank(downRequest.getGwDeviceId())) {
      return downRequest.getGwDeviceId();
    }
    if (StrUtil.isNotBlank(downRequest.getExtDeviceId())) {
      return downRequest.getExtDeviceId();
    }
    return downRequest.getDeviceId();
  }

  @Override
  public boolean supports(MQTTDownRequest request) {
    return DownCmd.DEV_FUNCTION.equals(request.getCmd());
  }

  @Override
  public String getName() {
    return "";
  }

  @Override
  public int getOrder() {
    return 0;
  }

  private static class ValidationResult {
    private final boolean valid;
    private final IoTProduct product;
    private final IoTDevice device;
    private final R<?> errorResponse;

    private ValidationResult(
        boolean valid, IoTProduct product, IoTDevice device, R<?> errorResponse) {
      this.valid = valid;
      this.product = product;
      this.device = device;
      this.errorResponse = errorResponse;
    }

    public static ValidationResult success(IoTProduct product, IoTDevice device) {
      return new ValidationResult(true, product, device, null);
    }

    public static ValidationResult error(R<?> errorResponse) {
      return new ValidationResult(false, null, null, errorResponse);
    }

    public boolean isValid() {
      return valid;
    }

    public IoTProduct getProduct() {
      return product;
    }

    public IoTDevice getDevice() {
      return device;
    }

    public R<?> getErrorResponse() {
      return errorResponse;
    }
  }

  private static class CommandContext {
    private final String commandId;
    private final String payload;
    private final String encoderType;

    private CommandContext(String commandId, String payload, String encoderType) {
      this.commandId = commandId;
      this.payload = payload;
      this.encoderType = encoderType;
    }

    public String getCommandId() {
      return commandId;
    }

    public String getPayload() {
      return payload;
    }

    public String getEncoderType() {
      return encoderType;
    }
  }

  private static class EncodeResult {
    private final boolean success;
    private final byte[] payloadBytes;
    private final String errorMessage;

    private EncodeResult(boolean success, byte[] payloadBytes, String errorMessage) {
      this.success = success;
      this.payloadBytes = payloadBytes;
      this.errorMessage = errorMessage;
    }

    public static EncodeResult success(byte[] payloadBytes) {
      return new EncodeResult(true, payloadBytes, null);
    }

    public static EncodeResult failure(String errorMessage) {
      return new EncodeResult(false, null, errorMessage);
    }

    public boolean isSuccess() {
      return success;
    }

    public byte[] getPayloadBytes() {
      return payloadBytes;
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }
}
