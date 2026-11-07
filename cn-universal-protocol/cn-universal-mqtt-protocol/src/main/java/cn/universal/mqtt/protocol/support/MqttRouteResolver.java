package cn.universal.mqtt.protocol.support;

import cn.hutool.core.util.StrUtil;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.persistence.entity.IoTProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** MQTT通道路由解析器，统一按产品绑定或消息来源决定走系统/第三方 MQTT。 */
@Component
public class MqttRouteResolver {

  @Autowired private IoTProductDeviceService ioTProductDeviceService;

  public MqttRoute resolveProductRoute(String productKey) {
    return resolveProductRoute(productKey, null);
  }

  public MqttRoute resolveProductRoute(String productKey, IoTProduct ioTProduct) {
    String networkUnionId = resolveBoundNetworkUnionId(productKey, ioTProduct);
    if (StrUtil.isBlank(networkUnionId)) {
      return MqttRoute.system();
    }
    return MqttRoute.third(networkUnionId);
  }

  public MqttRoute resolveReplyRoute(MQTTUPRequest request) {
    if (request == null) {
      return MqttRoute.system();
    }
    if (request.isSysMQTTBroker()) {
      return MqttRoute.system();
    }
    if (StrUtil.isNotBlank(request.getNetworkUnionId())) {
      return MqttRoute.third(request.getNetworkUnionId());
    }
    return resolveProductRoute(request.getProductKey(), request.getIoTProduct());
  }

  public String resolveBoundNetworkUnionId(String productKey, IoTProduct ioTProduct) {
    String networkUnionId = null;
    if (StrUtil.isNotBlank(productKey)) {
      networkUnionId = ioTProductDeviceService.selectNetworkUnionId(productKey);
    } else if (ioTProduct != null) {
      networkUnionId = ioTProduct.getNetworkUnionId();
    }
    if (ioTProduct != null) {
      ioTProduct.setNetworkUnionId(networkUnionId);
    }
    return networkUnionId;
  }

  public enum Channel {
    SYSTEM,
    THIRD
  }

  public static final class MqttRoute {
    private final Channel channel;
    private final String networkUnionId;

    private MqttRoute(Channel channel, String networkUnionId) {
      this.channel = channel;
      this.networkUnionId = networkUnionId;
    }

    public static MqttRoute system() {
      return new MqttRoute(Channel.SYSTEM, null);
    }

    public static MqttRoute third(String networkUnionId) {
      return new MqttRoute(Channel.THIRD, networkUnionId);
    }

    public Channel getChannel() {
      return channel;
    }

    public String getNetworkUnionId() {
      return networkUnionId;
    }

    public boolean isSystem() {
      return Channel.SYSTEM.equals(channel);
    }

    public boolean isThird() {
      return Channel.THIRD.equals(channel);
    }
  }
}
