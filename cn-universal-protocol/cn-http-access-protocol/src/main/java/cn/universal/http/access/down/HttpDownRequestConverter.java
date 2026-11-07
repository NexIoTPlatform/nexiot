package cn.universal.http.access.down;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.downlink.converter.DownRequestConverter;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.ICodecService;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HttpDownRequestConverter implements DownRequestConverter<HttpDownRequest> {

  private final IoTProductDeviceService productService;
  private final IoTDeviceService deviceService;
  private final ICodecService codecService;

  public HttpDownRequestConverter(
      IoTProductDeviceService productService,
      IoTDeviceService deviceService,
      ICodecService codecService) {
    this.productService = productService;
    this.deviceService = deviceService;
    this.codecService = codecService;
  }

  @Override
  public HttpDownRequest convert(UnifiedDownlinkCommand command, DownlinkContext<?> context) {
    if (command == null || blank(command.getProductKey())
        || (!HttpDownRequest.isProductLifecycle(command.getCmd()) && blank(command.getDeviceId()))) {
      throw new IllegalArgumentException("HTTP downlink requires productKey and deviceId");
    }
    IoTProduct product = productService.getProduct(command.getProductKey());
    if (product == null || !"http".equalsIgnoreCase(product.getThirdPlatform())) {
      throw new IllegalArgumentException("HTTP product does not exist: " + command.getProductKey());
    }
    HttpDownRequest request = new HttpDownRequest();
    BeanUtil.copyProperties(command, request, "extensions", "metadata");
    request.setCommand(command);
    request.setProduct(product);
    request.setIoTProduct(product);
    request.setData(JSONUtil.parseObj(command.getExtensions()));
    // Local lifecycle work runs before insertion or after deletion and never invokes a device driver.
    if (HttpDownRequest.isDeviceLifecycle(command.getCmd())
        || HttpDownRequest.isProductLifecycle(command.getCmd())) {
      return request;
    }
    IoTDeviceDTO device = deviceService.selectDevInstanceBO(command.getProductKey(), command.getDeviceId());
    if (device == null) {
      throw new IllegalArgumentException("HTTP device does not exist: " + command.getDeviceId());
    }
    request.setDevice(device);
    request.setIoTDeviceDTO(device);
    Map<String, Object> codecContext = new LinkedHashMap<>();
    codecContext.put("device", device);
    codecContext.put("product", product);
    codecContext.put("command", command);
    codecContext.put("downlinkAttributes", context.getAttributes());
    String encoded = codecService.encode(command.getProductKey(), command.toJson().toString(), codecContext);
    if (encoded == null || encoded.isBlank()) {
      throw new IllegalArgumentException("Product encode returned an empty HTTP command payload");
    }
    request.setEncodedPayload(encoded);
    request.setPayload(encoded);
    return request;
  }

  @Override
  public String supportedProtocol() {
    return "http";
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
