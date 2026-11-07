package cn.universal.http.access.down;

import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.persistence.base.BaseDownRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HttpDownRequest extends BaseDownRequest {
  private transient UnifiedDownlinkCommand command;
  private transient IoTProduct product;
  private transient IoTDeviceDTO device;
  private String encodedPayload;

  public static boolean isDeviceLifecycle(DownCmd command) {
    if (command == null) return false;
    return switch (command) {
      case DEV_ADD, DEV_ADDS, DEVICE_ADD, DEV_DEL, DEVICE_DELETE, DEV_UPDATE, DEVICE_UPDATE -> true;
      default -> false;
    };
  }

  public static boolean isProductLifecycle(DownCmd command) {
    if (command == null) return false;
    return switch (command) {
      case PRO_ADD, PRO_UPDATE, PRO_DEL, PRODUCT_ADD, PRODUCT_UPDATE, PRODUCT_DELETE,
          PRODUCT_PUBLISH -> true;
      default -> false;
    };
  }
}
