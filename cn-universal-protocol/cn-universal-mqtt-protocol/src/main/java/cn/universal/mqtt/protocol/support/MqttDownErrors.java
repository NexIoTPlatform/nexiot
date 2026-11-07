package cn.universal.mqtt.protocol.support;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.ERROR_CODE;
import cn.universal.common.domain.R;
import cn.universal.common.exception.IoTErrorCode;

/** MQTT 下行错误定义，统一收口当前模块的错误码与错误消息。 */
public final class MqttDownErrors {

  private static final String SEND_COMMAND_FAILURE_PREFIX = "发送指令失败: ";
  private static final String DEVICE_MAYBE_OFFLINE_MESSAGE = "设备可能不在线";
  private static final String EMPTY_ENCODE_PAYLOAD_MESSAGE =
      "下发报文为空，请检查 encode 驱动是否返回了有效结果";
  private static final String HEX_RAW_FALLBACK_MESSAGE =
      "产品配置的下发编码为 HEX，但当前 encode 驱动未返回有效 HEX 报文，平台已回退为原始 JSON，无法继续下发，请完善 encode 驱动或将产品 encoderType 调整为 STRING";

  private MqttDownErrors() {}

  public static R<?> sendCommandFailed(Exception exception) {
    return R.error(SEND_COMMAND_FAILURE_PREFIX + exception.getMessage());
  }

  public static R<?> deviceNotFound() {
    return ErrorDefinition.DEVICE_NOT_FOUND.response();
  }

  public static R<?> missingDownTopic() {
    return ErrorDefinition.DOWN_TOPIC_MISSING.response();
  }

  public static R<?> codecFailure(String message) {
    return ErrorDefinition.CODEC_ERROR.responseMessage(message);
  }

  public static String deviceMaybeOfflineMessage() {
    return DEVICE_MAYBE_OFFLINE_MESSAGE;
  }

  public static String emptyEncodePayloadMessage() {
    return EMPTY_ENCODE_PAYLOAD_MESSAGE;
  }

  public static String unsupportedEncoderTypeMessage(String encoderType) {
    return "产品配置的 encoderType 不支持: " + encoderType;
  }

  public static String payloadEncodeFailureMessage(
      String encoderType, String payloadSource, Exception exception) {
    if (PayloadMessageType.HEX.matches(encoderType)
        && IoTConstant.PAYLOAD_SOURCE_RAW_FALLBACK.equalsIgnoreCase(payloadSource)) {
      return HEX_RAW_FALLBACK_MESSAGE;
    }
    String reason =
        exception == null ? "编码结果为空" : StrUtil.blankToDefault(exception.getMessage(), "编码结果为空");
    return StrUtil.format("下发报文编码失败，encoderType={}，原因：{}", encoderType, reason);
  }

  private enum PayloadMessageType {
    HEX;

    boolean matches(String encoderType) {
      return name().equalsIgnoreCase(encoderType);
    }
  }

  private enum ErrorDefinition {
    DEVICE_NOT_FOUND(
        ERROR_CODE.DEV_DEL_DEVICE_NO_ID_EXIST.getCode(),
        ERROR_CODE.DEV_DEL_DEVICE_NO_ID_EXIST.getName()),
    DOWN_TOPIC_MISSING(
        ERROR_CODE.DEV_DOWN_NO_MQTT_TOPIC.getCode(),
        ERROR_CODE.DEV_DOWN_NO_MQTT_TOPIC.getName()),
    CODEC_ERROR(IoTErrorCode.CODEC_ERROR.getCode(), IoTErrorCode.CODEC_ERROR.getName());

    private final int code;
    private final String message;

    ErrorDefinition(int code, String message) {
      this.code = code;
      this.message = message;
    }

    R<Void> response() {
      return R.error(code, message);
    }

    R<Void> responseMessage(String customMessage) {
      return R.error(code, customMessage);
    }
  }
}
