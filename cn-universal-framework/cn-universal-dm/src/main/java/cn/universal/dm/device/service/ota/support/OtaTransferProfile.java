package cn.universal.dm.device.service.ota.support;

import cn.hutool.json.JSONUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.Data;

/** 产品的 OTA 传输配置；任务创建时保存快照，避免配置修改影响进行中的升级。 */
@Data
public class OtaTransferProfile {

  private String mode = "URL";
  private int chunkSize = 1024;
  private int ackTimeoutSeconds = 10;
  private int chunkMaxRetries = 3;
  private String dataEncoding = "BASE64";
  private int indexBase = 0;
  private String templateName;

  /** 缺省配置兼容历史 URL 模式；提供了字段时不会静默修正非法值。 */
  public static OtaTransferProfile from(Object value) {
    if (value instanceof OtaTransferProfile profile) {
      value = profile.toMap();
    }
    if (value == null || "".equals(value)) {
      return new OtaTransferProfile();
    }
    if (value instanceof String text) {
      if (!JSONUtil.isTypeJSONObject(text)) {
        throw new IllegalArgumentException("OTA 传输配置必须是对象");
      }
      value = JSONUtil.parseObj(text);
    }
    if (!(value instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException("OTA 传输配置必须是对象");
    }
    OtaTransferProfile result = new OtaTransferProfile();
    if (map.containsKey("mode")) result.mode = text(map, "mode");
    if (map.containsKey("chunkSize")) result.chunkSize = integer(map, "chunkSize");
    if (map.containsKey("ackTimeoutSeconds")) result.ackTimeoutSeconds = integer(map, "ackTimeoutSeconds");
    if (map.containsKey("chunkMaxRetries")) result.chunkMaxRetries = integer(map, "chunkMaxRetries");
    if (map.containsKey("dataEncoding")) result.dataEncoding = text(map, "dataEncoding");
    if (map.containsKey("indexBase")) result.indexBase = integer(map, "indexBase");
    // Hutool represents JSON null with JSONNull rather than Java null after persistence.
    if (!JSONUtil.isNull(map.get("templateName"))) result.templateName = text(map, "templateName");
    result.validate();
    return result;
  }

  public void validate() {
    if (!Set.of("URL", "PUSH", "PULL").contains(mode == null ? "" : mode)) {
      throw new IllegalArgumentException("OTA 传输方式仅支持 URL、PUSH、PULL");
    }
    if (chunkSize < 128 || chunkSize > 32768) throw new IllegalArgumentException("OTA 每包大小须为 128~32768 字节");
    if (ackTimeoutSeconds < 2 || ackTimeoutSeconds > 300) throw new IllegalArgumentException("OTA 确认超时须为 2~300 秒");
    if (chunkMaxRetries < 0 || chunkMaxRetries > 10) throw new IllegalArgumentException("OTA 分包重试须为 0~10 次");
    if (!Set.of("BASE64", "HEX").contains(dataEncoding == null ? "" : dataEncoding)) {
      throw new IllegalArgumentException("OTA 分包编码仅支持 BASE64、HEX");
    }
    if (indexBase != 0 && indexBase != 1) throw new IllegalArgumentException("OTA 包序号起点仅支持 0 或 1");
    if (templateName != null && templateName.length() > 100) throw new IllegalArgumentException("OTA 模板名称不能超过 100 字");
  }

  public boolean isChunked() {
    return !"URL".equals(mode);
  }

  public Map<String, Object> toMap() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("mode", mode);
    result.put("chunkSize", chunkSize);
    result.put("ackTimeoutSeconds", ackTimeoutSeconds);
    result.put("chunkMaxRetries", chunkMaxRetries);
    result.put("dataEncoding", dataEncoding);
    result.put("indexBase", indexBase);
    result.put("templateName", templateName);
    return result;
  }

  private static String text(Map<?, ?> map, String key) {
    Object value = map.get(key);
    if (!(value instanceof String string)) throw new IllegalArgumentException("OTA " + key + " 必须是字符串");
    return string.trim();
  }

  private static int integer(Map<?, ?> map, String key) {
    Object value = map.get(key);
    try {
      // Integer.parseInt prevents decimals, boolean conversion and numeric truncation.
      return Integer.parseInt(String.valueOf(value));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("OTA " + key + " 必须是整数");
    }
  }
}
